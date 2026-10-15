package io.github.chema280101.logtech.machine;

/**
 * Modelo de calor en Java pura desacoplado de dependencias de Minecraft.
 *
 * Cada máquina tiene una temperatura de 0 a 100 que se actualiza cada 20 ticks (1 segundo).
 * - Con generación base, la disipación térmica estabiliza la temperatura por debajo de 70 (en ~50 °C).
 * - Por encima de 70 °C, la máquina trabaja a media velocidad (0.5x).
 * - Al llegar a 100 °C, entra en estado de sobrecalentamiento (overheated = true, velocidad 0.0x).
 * - Histéresis: una vez sobrecalentada, no se reanuda hasta que el calor desciende a <= 40 °C.
 */
public class HeatModel {
    public static final double DEFAULT_SLOWDOWN_THRESHOLD = 70.0;
    public static final double DEFAULT_OVERHEAT_THRESHOLD = 100.0;
    public static final double DEFAULT_RESUME_THRESHOLD = 40.0;
    public static final double DEFAULT_SLOWDOWN_SPEED = 0.5;
    public static final double DEFAULT_NORMAL_EQUILIBRIUM_HEAT = 50.0;

    private double heat;
    private boolean overheated;

    private double slowdownThreshold;
    private double overheatThreshold;
    private double resumeThreshold;
    private double slowdownSpeed;

    public HeatModel() {
        this(0.0, false, DEFAULT_SLOWDOWN_THRESHOLD, DEFAULT_OVERHEAT_THRESHOLD, DEFAULT_RESUME_THRESHOLD, DEFAULT_SLOWDOWN_SPEED);
    }

    public HeatModel(double initialHeat, boolean initialOverheated) {
        this(initialHeat, initialOverheated, DEFAULT_SLOWDOWN_THRESHOLD, DEFAULT_OVERHEAT_THRESHOLD, DEFAULT_RESUME_THRESHOLD, DEFAULT_SLOWDOWN_SPEED);
    }

    public HeatModel(
            double initialHeat,
            boolean initialOverheated,
            double slowdownThreshold,
            double overheatThreshold,
            double resumeThreshold,
            double slowdownSpeed
    ) {
        this.heat = Math.max(0.0, Math.min(100.0, initialHeat));
        this.overheated = initialOverheated;
        this.slowdownThreshold = slowdownThreshold;
        this.overheatThreshold = overheatThreshold;
        this.resumeThreshold = resumeThreshold;
        this.slowdownSpeed = slowdownSpeed;
    }

    /**
     * Actualiza el modelo de calor (debe invocarse una vez cada 20 ticks / 1 segundo).
     *
     * @param working              si la máquina está realizando trabajo en este ciclo
     * @param generationRate       tasa base de calor generado por segundo
     * @param generationMultiplier multiplicador de calor (1.0 normal, mayor con Turbo)
     * @param coolingRate          tasa de disipación / enfriamiento por segundo
     */
    public void tick(boolean working, double generationRate, double generationMultiplier, double coolingRate) {
        if (working && !this.overheated) {
            this.heat += generationRate * generationMultiplier;
        }

        if (this.heat > 0.0) {
            double dissipation = coolingRate * (this.heat / DEFAULT_NORMAL_EQUILIBRIUM_HEAT);
            if (!working || this.overheated) {
                // Enfriamiento mínimo para asegurar llegada a 0 sin cola asintótica infinita
                dissipation = Math.max(dissipation, Math.min(this.heat, Math.max(0.5, coolingRate)));
            }
            this.heat = Math.max(0.0, this.heat - dissipation);
        }

        // Detección de sobrecalentamiento e histéresis
        if (this.heat >= this.overheatThreshold) {
            this.heat = Math.min(100.0, this.heat);
            this.overheated = true;
        } else if (this.overheated && this.heat <= this.resumeThreshold) {
            this.overheated = false;
        }
    }

    public double getHeat() {
        return this.heat;
    }

    public void setHeat(double heat) {
        this.heat = Math.max(0.0, Math.min(100.0, heat));
    }

    public boolean isOverheated() {
        return this.overheated;
    }

    public void setOverheated(boolean overheated) {
        this.overheated = overheated;
    }

    /**
     * Multiplicador de velocidad de procesamiento:
     * - 0.0 si está sobrecalentada
     * - 0.5 (slowdownSpeed) si el calor supera slowdownThreshold (> 70)
     * - 1.0 si opera con normalidad (<= 70)
     */
    public double getSpeedMultiplier() {
        if (this.overheated) {
            return 0.0;
        }
        if (this.heat > this.slowdownThreshold) {
            return this.slowdownSpeed;
        }
        return 1.0;
    }

    public double getSlowdownThreshold() {
        return this.slowdownThreshold;
    }

    public void setSlowdownThreshold(double slowdownThreshold) {
        this.slowdownThreshold = slowdownThreshold;
    }

    public double getOverheatThreshold() {
        return this.overheatThreshold;
    }

    public void setOverheatThreshold(double overheatThreshold) {
        this.overheatThreshold = overheatThreshold;
    }

    public double getResumeThreshold() {
        return this.resumeThreshold;
    }

    public void setResumeThreshold(double resumeThreshold) {
        this.resumeThreshold = resumeThreshold;
    }

    public double getSlowdownSpeed() {
        return this.slowdownSpeed;
    }

    public void setSlowdownSpeed(double slowdownSpeed) {
        this.slowdownSpeed = slowdownSpeed;
    }
}
