package io.github.chema280101.logtech.ore;

import java.util.Objects;

/**
 * Representa una veta de mineral generada de forma determinista.
 * En la versión 0.1, las vetas se modelan como columnas circulares.
 */
public class DepositVein {
    private final String id;
    private final DepositType type;
    private final int regionX;
    private final int regionZ;
    private final int veinIndex;
    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final int initialReserve;
    private final int purity;

    public DepositVein(String id, DepositType type, int regionX, int regionZ, int veinIndex,
                       int centerX, int centerZ, int radius, int initialReserve, int purity) {
        this.id = id;
        this.type = type;
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.veinIndex = veinIndex;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.initialReserve = initialReserve;
        this.purity = purity;
    }

    public String getId() {
        return id;
    }

    public DepositType getType() {
        return type;
    }

    public int getRegionX() {
        return regionX;
    }

    public int getRegionZ() {
        return regionZ;
    }

    public int getVeinIndex() {
        return veinIndex;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public int getRadius() {
        return radius;
    }

    public int getInitialReserve() {
        return initialReserve;
    }

    public int getPurity() {
        return purity;
    }

    /**
     * Determina si una coordenada de bloque (x, z) cae dentro de la columna circular de la veta.
     */
    public boolean contains(int x, int z) {
        long dx = (long) x - centerX;
        long dz = (long) z - centerZ;
        return (dx * dx + dz * dz) <= ((long) radius * radius);
    }

    /**
     * Distancia euclidiana en el plano horizontal (X/Z) desde una posición dada al centro de la veta.
     */
    public double distanceTo(double x, double z) {
        double dx = x - centerX;
        double dz = z - centerZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public double distanceTo(int x, int z) {
        return distanceTo((double) x, (double) z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepositVein that = (DepositVein) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DepositVein{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", center=[" + centerX + ", " + centerZ + "]" +
                ", radius=" + radius +
                ", initialReserve=" + initialReserve +
                ", purity=" + purity +
                '}';
    }
}
