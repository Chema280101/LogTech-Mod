package io.github.chema280101.logtech.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Motor de evaluación y ejecución de reglas de automatización «condición → acción».
 * Clase Java pura desacoplada de Minecraft.
 *
 * @param <K> tipo de clave del ítem
 */
public class RuleEngine<K> {
    public static final int MAX_RULES = 8;
    public static final double SPACE_HYSTERESIS_PERCENT = 2.0;
    public static final double COUNT_HYSTERESIS_RATIO = 0.05;

    public record NetworkState<K>(Map<K, Long> itemAmounts, long totalStored, long totalCapacity) {
        public long getAmount(K key) {
            if (key == null || itemAmounts == null) {
                return 0L;
            }
            return itemAmounts.getOrDefault(key, 0L);
        }

        public double getUsedSpacePercentage() {
            if (totalCapacity <= 0L) {
                return 0.0;
            }
            return ((double) totalStored / (double) totalCapacity) * 100.0;
        }
    }

    private static class RuleState {
        boolean active = false;
        boolean notificationFired = false;
    }

    private final List<AutomationRule<K>> rules;
    private final RuleState[] states;

    public RuleEngine() {
        this.rules = new ArrayList<>(MAX_RULES);
        this.states = new RuleState[MAX_RULES];
        for (int i = 0; i < MAX_RULES; i++) {
            this.rules.add(AutomationRule.empty(i));
            this.states[i] = new RuleState();
        }
    }

    public AutomationRule<K> getRule(int index) {
        if (index < 0 || index >= MAX_RULES) {
            return null;
        }
        return this.rules.get(index);
    }

    public void setRule(int index, AutomationRule<K> rule) {
        if (index < 0 || index >= MAX_RULES) {
            return;
        }
        this.rules.set(index, rule != null ? rule : AutomationRule.empty(index));
        // Reset de estado interno si la regla cambia
        this.states[index].active = false;
        this.states[index].notificationFired = false;
    }

    public List<AutomationRule<K>> getRules() {
        return Collections.unmodifiableList(this.rules);
    }

    public boolean isRuleActive(int index) {
        if (index < 0 || index >= MAX_RULES) {
            return false;
        }
        return this.states[index].active;
    }

    public boolean isRedstoneSignalActive() {
        return !getActiveRedstoneChannels().isEmpty();
    }

    public boolean isRedstoneSignalActive(int channel) {
        return getActiveRedstoneChannels().contains(channel);
    }

    public Set<Integer> getActiveRedstoneChannels() {
        Set<Integer> activeChannels = new HashSet<>();
        for (int i = 0; i < MAX_RULES; i++) {
            AutomationRule<K> rule = this.rules.get(i);
            if (rule != null && rule.isValid() && this.states[i].active) {
                if (rule.getAction().isRedstone()) {
                    activeChannels.add(rule.getAction().getRedstoneChannel());
                }
            }
        }
        return activeChannels;
    }

    public boolean isItemMoveToColdActive(K item) {
        if (item == null) {
            return false;
        }
        for (int i = 0; i < MAX_RULES; i++) {
            AutomationRule<K> rule = this.rules.get(i);
            if (rule != null && rule.isValid() && this.states[i].active) {
                if (rule.getAction() == RuleAction.MOVE_TO_COLD && item.equals(rule.getItem())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Evalúa las reglas activas con histéresis anti-parpadeo y rearme de notificaciones.
     *
     * @param state estado actual del almacenamiento de la red
     * @return resultado inmutable con acciones activas y avisos
     */
    public RuleEvaluationResult<K> evaluate(NetworkState<K> state) {
        Set<K> activeColdItems = new HashSet<>();
        List<RuleNotification<K>> notifications = new ArrayList<>();
        Set<Integer> activeRedstoneChannels = new HashSet<>();
        boolean redstoneActive = false;

        for (int i = 0; i < MAX_RULES; i++) {
            AutomationRule<K> rule = this.rules.get(i);
            RuleState rState = this.states[i];

            if (rule == null || !rule.isValid()) {
                rState.active = false;
                rState.notificationFired = false;
                continue;
            }

            boolean wasActive = rState.active;
            boolean nowActive;

            RuleConditionType cond = rule.getConditionType();
            long threshold = rule.getThreshold();

            if (cond.requiresItem()) {
                long currentCount = state != null ? state.getAmount(rule.getItem()) : 0L;
                long margin = Math.max(1L, (long) Math.floor(threshold * COUNT_HYSTERESIS_RATIO));
                if (threshold == 0L) {
                    margin = 0L;
                }

                if (cond == RuleConditionType.ITEM_COUNT_GREATER) {
                    if (!wasActive) {
                        nowActive = (currentCount > threshold);
                    } else {
                        // Con histéresis: se mantiene activo hasta que caiga por debajo de umbral - margen
                        nowActive = (currentCount > threshold - margin);
                    }
                } else { // ITEM_COUNT_LESS
                    if (!wasActive) {
                        nowActive = (currentCount < threshold);
                    } else {
                        // Con histéresis: se mantiene activo hasta que suba por encima de umbral + margen
                        nowActive = (currentCount < threshold + margin);
                    }
                }
            } else { // Condición de porcentaje de espacio usado
                double currentSpace = state != null ? state.getUsedSpacePercentage() : 0.0;
                double thresh = (double) threshold;

                if (cond == RuleConditionType.TOTAL_SPACE_GREATER) {
                    if (!wasActive) {
                        nowActive = (currentSpace > thresh);
                    } else {
                        // Con histéresis: se mantiene activo hasta que caiga por debajo de umbral - margen
                        nowActive = (currentSpace > thresh - SPACE_HYSTERESIS_PERCENT);
                    }
                } else { // TOTAL_SPACE_LESS
                    if (!wasActive) {
                        nowActive = (currentSpace < thresh);
                    } else {
                        // Con histéresis: se mantiene activo hasta que suba por encima de umbral + margen
                        nowActive = (currentSpace < thresh + SPACE_HYSTERESIS_PERCENT);
                    }
                }
            }

            rState.active = nowActive;

            if (nowActive) {
                switch (rule.getAction()) {
                    case MOVE_TO_COLD -> {
                        if (rule.getItem() != null) {
                            activeColdItems.add(rule.getItem());
                        }
                    }
                    case NOTIFY -> {
                        // Disparar aviso exactamente una vez al activarse
                        if (!rState.notificationFired) {
                            rState.notificationFired = true;
                            notifications.add(new RuleNotification<>(i, rule.copy()));
                        }
                    }
                    case REDSTONE_SIGNAL, REDSTONE_2, REDSTONE_3, REDSTONE_4 -> {
                        redstoneActive = true;
                        activeRedstoneChannels.add(rule.getAction().getRedstoneChannel());
                    }
                }
            } else {
                // Cuando deja de cumplirse la condición, se rearma el aviso
                rState.notificationFired = false;
            }
        }

        return new RuleEvaluationResult<>(
                Collections.unmodifiableSet(activeColdItems),
                Collections.unmodifiableList(notifications),
                redstoneActive,
                Collections.unmodifiableSet(activeRedstoneChannels)
        );
    }

    public void clear() {
        for (int i = 0; i < MAX_RULES; i++) {
            this.rules.set(i, AutomationRule.empty(i));
            this.states[i].active = false;
            this.states[i].notificationFired = false;
        }
    }
}
