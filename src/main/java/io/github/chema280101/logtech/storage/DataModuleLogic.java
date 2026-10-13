package io.github.chema280101.logtech.storage;

import java.util.List;

/**
 * Lógica matemática pura para cálculo y balanceo de capacidades de módulos de datos (v0.1, v0.2 nivel 2 y 3).
 * Desacoplada de Minecraft para permitir pruebas unitarias rigurosas y deterministas.
 */
public final class DataModuleLogic {
    public static final int DEFAULT_BASE_HOT_CAPACITY = 4096;
    public static final int DEFAULT_BASE_COLD_CAPACITY = 16384;
    public static final int DEFAULT_ADVANCED_MULTIPLIER = 4;
    public static final int DEFAULT_MASTER_MULTIPLIER = 16;

    public enum ModuleTier {
        BASIC(1),
        ADVANCED(4),
        MASTER(16);

        private final int defaultMultiplier;

        ModuleTier(int defaultMultiplier) {
            this.defaultMultiplier = defaultMultiplier;
        }

        public int getDefaultMultiplier() {
            return defaultMultiplier;
        }
    }

    private DataModuleLogic() {
    }

    /**
     * Calcula la capacidad aportada por un módulo individual dado su multiplicador y la capacidad base del rack.
     *
     * @param baseCapacityPerModule capacidad base por módulo del rack (caliente o frío)
     * @param multiplier            multiplicador de capacidad del tipo de módulo (1 para básico, 4 para avanzado, 16 para maestro)
     * @return capacidad en ítems que aporta el módulo
     */
    public static long calculateModuleCapacity(long baseCapacityPerModule, int multiplier) {
        if (baseCapacityPerModule <= 0 || multiplier <= 0) {
            return 0L;
        }
        return baseCapacityPerModule * (long) multiplier;
    }

    /**
     * Calcula la capacidad total de un rack sumando las capacidades individuales de sus ranuras.
     *
     * @param baseCapacityPerModule capacidad base por módulo del rack
     * @param moduleMultipliers     lista de multiplicadores de los módulos instalados en las ranuras (0 o nulo para vacías)
     * @return capacidad total en ítems del rack
     */
    public static long calculateRackCapacity(long baseCapacityPerModule, List<Integer> moduleMultipliers) {
        if (baseCapacityPerModule <= 0 || moduleMultipliers == null) {
            return 0L;
        }
        long total = 0L;
        for (Integer mult : moduleMultipliers) {
            if (mult != null && mult > 0) {
                total += calculateModuleCapacity(baseCapacityPerModule, mult);
            }
        }
        return total;
    }

    /**
     * Calcula la capacidad agregada de una red sumando las capacidades de los racks conectados.
     *
     * @param rackCapacities lista con la capacidad de cada rack de la red
     * @return capacidad total combinada
     */
    public static long calculateNetworkCapacity(List<Long> rackCapacities) {
        if (rackCapacities == null) {
            return 0L;
        }
        long total = 0L;
        for (Long cap : rackCapacities) {
            if (cap != null && cap > 0) {
                total += cap;
            }
        }
        return total;
    }
}
