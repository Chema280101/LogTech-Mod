package io.github.chema280101.logtech.ore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Generador determinista de vetas de mineral basado en la semilla del mundo y coordenadas de región.
 * No tiene dependencias de clases de Minecraft para permitir pruebas unitarias y garantizar
 * que la generación no interactúe con chunks ni genere sobrecarga de ticks.
 */
public final class VeinGenerator {
    public static final int REGION_SIZE = 128;
    public static final int MIN_RADIUS = 12;
    public static final int MAX_RADIUS = 24;
    public static final int MIN_RESERVE = 20_000;
    public static final int MAX_RESERVE = 60_000;
    public static final int MIN_PURITY = 1;
    public static final int MAX_PURITY = 5;

    private VeinGenerator() {
    }

    /**
     * Convierte una coordenada de bloque a su coordenada de región correspondiente.
     */
    public static int toRegionCoordinate(int blockCoord) {
        return Math.floorDiv(blockCoord, REGION_SIZE);
    }

    /**
     * Deriva una semilla determinista pseudoaleatoria de 64 bits para una región dada
     * combinando la semilla del mundo con las coordenadas de la región mediante SplitMix64.
     */
    public static long getRegionSeed(long worldSeed, int regionX, int regionZ) {
        long h = worldSeed + ((long) regionX * 341873128712L) + ((long) regionZ * 132897987541L) + 0x9E3779B97F4A7C15L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    /**
     * Genera de 0 a 2 vetas deterministas para la región especificada.
     */
    public static List<DepositVein> generateVeins(long worldSeed, int regionX, int regionZ) {
        Random rng = new Random(getRegionSeed(worldSeed, regionX, regionZ));
        int count = rng.nextInt(3); // 0, 1 o 2 vetas
        if (count == 0) {
            return Collections.emptyList();
        }

        List<DepositVein> veins = new ArrayList<>(count);
        int regionMinX = regionX * REGION_SIZE;
        int regionMinZ = regionZ * REGION_SIZE;

        for (int i = 0; i < count; i++) {
            String id = "vein_" + regionX + "_" + regionZ + "_" + i;
            DepositType type = (rng.nextInt(2) == 0) ? DepositType.NEOFERRITE : DepositType.CRYSTALITE;
            int centerX = regionMinX + rng.nextInt(REGION_SIZE);
            int centerZ = regionMinZ + rng.nextInt(REGION_SIZE);
            int radius = MIN_RADIUS + rng.nextInt(MAX_RADIUS - MIN_RADIUS + 1); // 12 a 24
            int initialReserve = MIN_RESERVE + rng.nextInt(MAX_RESERVE - MIN_RESERVE + 1); // 20000 a 60000
            int purity = MIN_PURITY + rng.nextInt(MAX_PURITY - MIN_PURITY + 1); // 1 a 5

            veins.add(new DepositVein(id, type, regionX, regionZ, i, centerX, centerZ, radius, initialReserve, purity));
        }

        return Collections.unmodifiableList(veins);
    }

    /**
     * Busca y devuelve todas las vetas cuyos centros están dentro de maxDistance bloques
     * desde una coordenada de bloque (blockX, blockZ), ordenadas de más cercana a más lejana.
     */
    public static List<DepositVein> getVeinsNear(long worldSeed, int blockX, int blockZ, int maxDistance) {
        int minRx = Math.floorDiv(blockX - maxDistance, REGION_SIZE);
        int maxRx = Math.floorDiv(blockX + maxDistance, REGION_SIZE);
        int minRz = Math.floorDiv(blockZ - maxDistance, REGION_SIZE);
        int maxRz = Math.floorDiv(blockZ + maxDistance, REGION_SIZE);

        List<DepositVein> result = new ArrayList<>();
        double maxDistSq = (double) maxDistance * maxDistance;

        for (int rx = minRx; rx <= maxRx; rx++) {
            for (int rz = minRz; rz <= maxRz; rz++) {
                List<DepositVein> regionVeins = generateVeins(worldSeed, rx, rz);
                for (DepositVein vein : regionVeins) {
                    double dx = (double) vein.getCenterX() - blockX;
                    double dz = (double) vein.getCenterZ() - blockZ;
                    if ((dx * dx + dz * dz) <= maxDistSq) {
                        result.add(vein);
                    }
                }
            }
        }

        result.sort(Comparator.comparingDouble(v -> v.distanceTo(blockX, blockZ)));
        return result;
    }

    /**
     * Encuentra cualquier veta que contenga las coordenadas de bloque (blockX, blockZ)
     * dentro de su columna circular.
     */
    public static List<DepositVein> getVeinsAt(long worldSeed, int blockX, int blockZ) {
        int minRx = Math.floorDiv(blockX - MAX_RADIUS, REGION_SIZE);
        int maxRx = Math.floorDiv(blockX + MAX_RADIUS, REGION_SIZE);
        int minRz = Math.floorDiv(blockZ - MAX_RADIUS, REGION_SIZE);
        int maxRz = Math.floorDiv(blockZ + MAX_RADIUS, REGION_SIZE);

        List<DepositVein> result = new ArrayList<>();
        for (int rx = minRx; rx <= maxRx; rx++) {
            for (int rz = minRz; rz <= maxRz; rz++) {
                for (DepositVein vein : generateVeins(worldSeed, rx, rz)) {
                    if (vein.contains(blockX, blockZ)) {
                        result.add(vein);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Reconstruye o busca una veta por su ID estable (ej: vein_2_-1_0).
     */
    public static DepositVein getVeinById(long worldSeed, String id) {
        if (id == null) return null;
        String[] parts = id.split("_");
        if (parts.length == 4 && "vein".equals(parts[0])) {
            try {
                int rx = Integer.parseInt(parts[1]);
                int rz = Integer.parseInt(parts[2]);
                int index = Integer.parseInt(parts[3]);
                List<DepositVein> veins = generateVeins(worldSeed, rx, rz);
                if (index >= 0 && index < veins.size()) {
                    return veins.get(index);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
