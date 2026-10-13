package io.github.chema280101.logtech.network;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lógica pura desacoplada para el reparto proporcional de energía de la red a las máquinas consumidoras.
 */
public final class EnergyDistributionLogic {

    private EnergyDistributionLogic() {
    }

    /**
     * Reparte la energía disponible entre los miembros demandantes.
     * Si la energía disponible cubre la suma pedida, asigna el 100% a cada uno.
     * Si es menor, realiza un reparto proporcional a la demanda de cada miembro:
     *   asignación = floor(energíaDisponible * pedido / totalPedido)
     *
     * Invariantes garantizadas:
     * - Ningún miembro recibe más de lo pedido.
     * - Ningún miembro recibe una cantidad negativa.
     * - La suma de energía repartida nunca supera availableEnergy.
     *
     * @param requests mapa de miembro -> energía demandada en FE
     * @param availableEnergy energía total disponible para repartir en FE
     * @param <T> tipo del identificador o instancia del miembro
     * @return mapa de miembro -> energía asignada en FE
     */
    public static <T> Map<T, Integer> distributeProportional(Map<T, Integer> requests, int availableEnergy) {
        if (requests == null || requests.isEmpty() || availableEnergy <= 0) {
            return Collections.emptyMap();
        }

        long totalRequested = 0L;
        for (int req : requests.values()) {
            if (req > 0) {
                totalRequested += req;
            }
        }

        if (totalRequested <= 0L) {
            return Collections.emptyMap();
        }

        Map<T, Integer> distribution = new LinkedHashMap<>();

        if (availableEnergy >= totalRequested) {
            for (Map.Entry<T, Integer> entry : requests.entrySet()) {
                if (entry.getValue() > 0) {
                    distribution.put(entry.getKey(), entry.getValue());
                }
            }
            return distribution;
        }

        for (Map.Entry<T, Integer> entry : requests.entrySet()) {
            int req = entry.getValue();
            if (req > 0) {
                int share = (int) ((long) availableEnergy * req / totalRequested);
                distribution.put(entry.getKey(), share);
            }
        }

        return distribution;
    }
}
