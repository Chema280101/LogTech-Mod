package io.github.chema280101.logtech.machine;

/**
 * Contrato común para máquinas que generan calor y disipan temperatura mediante bloques vecinos o fluidos.
 */
public interface HeatDissipatingMachine {

    /**
     * Recalcula la tasa de enfriamiento examinando los 6 bloques vecinos directos.
     * Solo debe invocarse cuando un vecino cambia (neighborChanged / updateShape) o al inicializarse.
     */
    void recalculateCooling();

    /**
     * Devuelve la tasa de enfriamiento actual en caché sin realizar consultas de bloques vecinos.
     */
    double getCachedCooling();

    /**
     * Devuelve la cantidad de disipadores vecinos directos detectados en caché (0 a 6).
     */
    int getCachedHeatsinks();

    /**
     * Devuelve la cantidad de caras vecinas directas en contacto con agua en caché (0 a 6).
     */
    int getCachedWaterSides();

    /**
     * Devuelve el modelo de calor asociado a la máquina.
     */
    HeatModel getHeatModel();
}
