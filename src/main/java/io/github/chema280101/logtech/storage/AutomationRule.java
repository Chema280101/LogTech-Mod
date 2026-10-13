package io.github.chema280101.logtech.storage;

import java.util.Objects;

/**
 * Modelo de una regla de automatización «condición → acción».
 *
 * @param <K> tipo de clave del ítem (ej. ItemKey en el juego, String en pruebas unitarias)
 */
public class AutomationRule<K> {
    private int id;
    private K item;
    private RuleConditionType conditionType;
    private long threshold;
    private RuleAction action;
    private boolean enabled;

    public AutomationRule(int id, K item, RuleConditionType conditionType, long threshold, RuleAction action, boolean enabled) {
        this.id = id;
        this.item = item;
        this.conditionType = conditionType != null ? conditionType : RuleConditionType.ITEM_COUNT_GREATER;
        this.threshold = Math.max(0L, threshold);
        this.action = action != null ? action : RuleAction.MOVE_TO_COLD;
        this.enabled = enabled;
    }

    public static <K> AutomationRule<K> empty(int id) {
        return new AutomationRule<>(id, null, RuleConditionType.ITEM_COUNT_GREATER, 0L, RuleAction.MOVE_TO_COLD, false);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public K getItem() {
        return item;
    }

    public void setItem(K item) {
        this.item = item;
    }

    public RuleConditionType getConditionType() {
        return conditionType;
    }

    public void setConditionType(RuleConditionType conditionType) {
        this.conditionType = conditionType != null ? conditionType : RuleConditionType.ITEM_COUNT_GREATER;
    }

    public long getThreshold() {
        return threshold;
    }

    public void setThreshold(long threshold) {
        this.threshold = Math.max(0L, threshold);
    }

    public RuleAction getAction() {
        return action;
    }

    public void setAction(RuleAction action) {
        this.action = action != null ? action : RuleAction.MOVE_TO_COLD;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Comprueba si la regla es válida para ser evaluada.
     * Si una regla requiere un ítem pero no tiene ninguno, se ignora.
     */
    public boolean isValid() {
        if (!enabled) {
            return false;
        }
        if (conditionType == null || action == null) {
            return false;
        }
        if (conditionType.requiresItem() && item == null) {
            return false;
        }
        if (action.requiresItem() && item == null) {
            return false;
        }
        return threshold >= 0L;
    }

    public AutomationRule<K> copy() {
        return new AutomationRule<>(id, item, conditionType, threshold, action, enabled);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutomationRule<?> that = (AutomationRule<?>) o;
        return id == that.id && threshold == that.threshold && enabled == that.enabled &&
                Objects.equals(item, that.item) && conditionType == that.conditionType && action == that.action;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, item, conditionType, threshold, action, enabled);
    }
}
