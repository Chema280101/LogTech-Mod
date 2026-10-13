package io.github.chema280101.logtech.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Distribuidor de energía por prioridades con estabilidad temporal en los límites.
 * Clase Java pura desacoplada de Minecraft para reparto de FE en la red.
 *
 * Jerarquía de prioridades:
 * 1. ALTA (HIGH): primer nivel
 * 2. NORMAL (NORMAL): segundo nivel
 * 3. BAJA (LOW): tercer nivel
 *
 * Dentro de un mismo nivel, el reparto es proporcional a lo solicitado.
 * Una máquina sin energía suficiente por prioridad se marca como SHED (apagada por prioridad).
 *
 * Para evitar que una máquina en el límite se encienda y apague en cada ciclo (chattering cada 20 ticks),
 * cada asignación / estado se mantiene estable durante varios ciclos (hold duration).
 *
 * @param <T> identificador o referencia del miembro de la red
 */
public class PriorityEnergyDistributor<T> {

    public static final int DEFAULT_HOLD_CYCLES = 3;

    public record Request<T>(T member, int requestedEnergy, EnergyPriority priority) {
    }

    public record Allocation(int allocatedEnergy, boolean shed) {
    }

    public record DistributionResult<T>(Map<T, Allocation> allocations, int totalDistributed) {
        public Allocation get(T member) {
            return allocations.getOrDefault(member, new Allocation(0, false));
        }

        public int getAllocated(T member) {
            return get(member).allocatedEnergy();
        }

        public boolean isShed(T member) {
            return get(member).shed();
        }
    }

    public static class MemberState {
        private int allocated = 0;
        private boolean shed = false;
        private int remainingHold = 0;
        private EnergyPriority lastPriority = EnergyPriority.NORMAL;

        public int getAllocated() {
            return allocated;
        }

        public boolean isShed() {
            return shed;
        }

        public int getRemainingHold() {
            return remainingHold;
        }

        public EnergyPriority getLastPriority() {
            return lastPriority;
        }
    }

    private final int holdCycles;
    private final Map<T, MemberState> states = new LinkedHashMap<>();

    public PriorityEnergyDistributor() {
        this(DEFAULT_HOLD_CYCLES);
    }

    public PriorityEnergyDistributor(int holdCycles) {
        this.holdCycles = Math.max(1, holdCycles);
    }

    public int getHoldCycles() {
        return this.holdCycles;
    }

    public MemberState getState(T member) {
        return this.states.get(member);
    }

    public void clear() {
        this.states.clear();
    }

    public DistributionResult<T> distribute(Collection<Request<T>> requests, int availableEnergy) {
        if (requests == null || requests.isEmpty()) {
            return new DistributionResult<>(Collections.emptyMap(), 0);
        }
        Map<T, Request<T>> map = new LinkedHashMap<>();
        for (Request<T> req : requests) {
            if (req != null && req.member() != null) {
                map.put(req.member(), req);
            }
        }
        return distribute(map, availableEnergy);
    }

    public DistributionResult<T> distribute(Map<T, Request<T>> requests, int availableEnergy) {
        if (requests == null || requests.isEmpty()) {
            return new DistributionResult<>(Collections.emptyMap(), 0);
        }

        // Si la energía disponible es 0 o menor, nadie recibe energía
        if (availableEnergy <= 0) {
            Map<T, Allocation> zeroAlloc = new LinkedHashMap<>();
            for (Map.Entry<T, Request<T>> entry : requests.entrySet()) {
                T member = entry.getKey();
                Request<T> req = entry.getValue();
                MemberState state = this.states.computeIfAbsent(member, k -> new MemberState());
                // Si la prioridad cambió por acción del usuario, reiniciar hold
                if (state.lastPriority != req.priority()) {
                    state.remainingHold = 0;
                    state.lastPriority = req.priority();
                }
                boolean shed = req.requestedEnergy() > 0 && req.priority() != EnergyPriority.HIGH;
                state.allocated = 0;
                state.shed = shed;
                zeroAlloc.put(member, new Allocation(0, shed));
            }
            return new DistributionResult<>(zeroAlloc, 0);
        }

        // 1. Detectar cambios explícitos de prioridad y reiniciar hold en ese miembro
        for (Map.Entry<T, Request<T>> entry : requests.entrySet()) {
            T member = entry.getKey();
            Request<T> req = entry.getValue();
            MemberState state = this.states.computeIfAbsent(member, k -> new MemberState());
            if (state.lastPriority != req.priority()) {
                state.remainingHold = 0;
                state.lastPriority = req.priority();
            }
        }

        // 2. Clasificar miembros con demanda por niveles de prioridad
        List<Request<T>> highReqs = new ArrayList<>();
        List<Request<T>> normalReqs = new ArrayList<>();
        List<Request<T>> lowReqs = new ArrayList<>();

        for (Request<T> req : requests.values()) {
            if (req.requestedEnergy() > 0) {
                switch (req.priority()) {
                    case HIGH -> highReqs.add(req);
                    case NORMAL -> normalReqs.add(req);
                    case LOW -> lowReqs.add(req);
                }
            }
        }

        // 3. Reparto raw ideal por niveles (HIGH -> NORMAL -> LOW)
        Map<T, Integer> rawAlloc = new LinkedHashMap<>();
        Map<T, Boolean> rawShed = new LinkedHashMap<>();

        // Inicializar por defecto para miembros sin demanda
        for (Request<T> req : requests.values()) {
            rawAlloc.put(req.member(), 0);
            rawShed.put(req.member(), false);
        }

        int energyBudget = availableEnergy;

        // Nivel 1: ALTA
        energyBudget = allocateTier(highReqs, energyBudget, rawAlloc, rawShed, false);

        // Nivel 2: NORMAL
        boolean normalCanBeShed = availableEnergy > 0;
        energyBudget = allocateTier(normalReqs, energyBudget, rawAlloc, rawShed, normalCanBeShed);

        // Nivel 3: BAJA
        energyBudget = allocateTier(lowReqs, energyBudget, rawAlloc, rawShed, true);

        // 4. Aplicar estabilización de asignación (hold cycles)
        Map<T, Allocation> finalAllocations = new LinkedHashMap<>();
        int currentDistributed = 0;

        for (Map.Entry<T, Request<T>> entry : requests.entrySet()) {
            T member = entry.getKey();
            Request<T> req = entry.getValue();
            MemberState state = this.states.computeIfAbsent(member, k -> new MemberState());

            int targetAlloc = rawAlloc.getOrDefault(member, 0);
            boolean targetShed = rawShed.getOrDefault(member, false);

            if (req.requestedEnergy() <= 0) {
                state.allocated = 0;
                state.shed = false;
                state.remainingHold = 0;
                finalAllocations.put(member, new Allocation(0, false));
                continue;
            }

            boolean isHeld = state.remainingHold > 0;
            int effectiveAlloc;
            boolean effectiveShed;

            if (isHeld) {
                // Mantener estado anterior si hay un hold activo ante fluctuaciones en el límite
                if (state.shed) {
                    // Si el nuevo cálculo le otorga la totalidad de lo pedido (ej. se añadió un generador),
                    // la máquina vuelve inmediatamente. Si es una fluctuación parcial menor, se mantiene SHED.
                    if (targetAlloc >= req.requestedEnergy()) {
                        effectiveAlloc = targetAlloc;
                        effectiveShed = false;
                        state.remainingHold = this.holdCycles;
                    } else {
                        effectiveAlloc = 0;
                        effectiveShed = true;
                        state.remainingHold--;
                    }
                } else {
                    // Estaba activa: intentar mantener encendida durante el periodo de hold
                    effectiveAlloc = Math.min(req.requestedEnergy(), Math.max(targetAlloc, state.allocated));
                    effectiveShed = false;
                    state.remainingHold--;
                }
            } else {
                // Sin hold activo: adoptar el cálculo de este ciclo
                effectiveAlloc = targetAlloc;
                effectiveShed = targetShed;

                // Si hubo transición de estado, iniciar un nuevo periodo de hold
                if (effectiveShed != state.shed || (effectiveAlloc > 0) != (state.allocated > 0)) {
                    state.remainingHold = this.holdCycles;
                }
            }

            state.allocated = effectiveAlloc;
            state.shed = effectiveShed;
            finalAllocations.put(member, new Allocation(effectiveAlloc, effectiveShed));
            currentDistributed += effectiveAlloc;
        }

        // 5. Garantía estricta de conservación: total distribuido <= availableEnergy
        if (currentDistributed > availableEnergy) {
            int toDeduct = currentDistributed - availableEnergy;
            // Reducir desde menor prioridad a mayor prioridad
            toDeduct = trimAllocations(finalAllocations, lowReqs, toDeduct);
            if (toDeduct > 0) {
                toDeduct = trimAllocations(finalAllocations, normalReqs, toDeduct);
            }
            if (toDeduct > 0) {
                toDeduct = trimAllocations(finalAllocations, highReqs, toDeduct);
            }
        }

        int totalDistributed = 0;
        for (Map.Entry<T, Allocation> entry : finalAllocations.entrySet()) {
            totalDistributed += entry.getValue().allocatedEnergy();
            MemberState state = this.states.get(entry.getKey());
            if (state != null) {
                state.allocated = entry.getValue().allocatedEnergy();
                state.shed = entry.getValue().shed();
            }
        }

        return new DistributionResult<>(finalAllocations, totalDistributed);
    }

    private int allocateTier(
            List<Request<T>> tierReqs,
            int budget,
            Map<T, Integer> rawAlloc,
            Map<T, Boolean> rawShed,
            boolean markShedWhenZero
    ) {
        if (tierReqs.isEmpty()) {
            return budget;
        }

        long totalTierDemand = 0L;
        for (Request<T> req : tierReqs) {
            totalTierDemand += req.requestedEnergy();
        }

        if (totalTierDemand <= 0L) {
            return budget;
        }

        if (budget >= totalTierDemand) {
            // El presupuesto cubre el 100% de este nivel
            for (Request<T> req : tierReqs) {
                rawAlloc.put(req.member(), req.requestedEnergy());
                rawShed.put(req.member(), false);
            }
            return (int) (budget - totalTierDemand);
        }

        if (budget > 0) {
            // Reparto proporcional dentro de este nivel: floor(budget * req / totalTierDemand)
            int distributedInTier = 0;
            for (Request<T> req : tierReqs) {
                int share = (int) ((long) budget * req.requestedEnergy() / totalTierDemand);
                rawAlloc.put(req.member(), share);
                rawShed.put(req.member(), share == 0 && markShedWhenZero);
                distributedInTier += share;
            }
            return budget - distributedInTier;
        }

        // Presupuesto agotado: toda la prioridad queda sin energía
        for (Request<T> req : tierReqs) {
            rawAlloc.put(req.member(), 0);
            rawShed.put(req.member(), markShedWhenZero);
        }
        return 0;
    }

    private int trimAllocations(Map<T, Allocation> finalAllocations, List<Request<T>> tierReqs, int toDeduct) {
        for (Request<T> req : tierReqs) {
            if (toDeduct <= 0) {
                break;
            }
            Allocation alloc = finalAllocations.get(req.member());
            if (alloc != null && alloc.allocatedEnergy() > 0) {
                int canReduce = Math.min(alloc.allocatedEnergy(), toDeduct);
                int newAlloc = alloc.allocatedEnergy() - canReduce;
                boolean shed = newAlloc == 0 && alloc.shed();
                finalAllocations.put(req.member(), new Allocation(newAlloc, shed));
                toDeduct -= canReduce;
            }
        }
        return toDeduct;
    }
}
