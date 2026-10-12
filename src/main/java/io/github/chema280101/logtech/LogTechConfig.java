package io.github.chema280101.logtech;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class LogTechConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Storage
    public static final ModConfigSpec.IntValue DATA_MODULE_CAPACITY;

    // Energy consumption
    public static final ModConfigSpec.IntValue CORE_ENERGY_CONSUMPTION;
    public static final ModConfigSpec.IntValue CRUSHER_ENERGY_CONSUMPTION;
    public static final ModConfigSpec.IntValue DRILL_ENERGY_CONSUMPTION;

    // Drill mechanics
    public static final ModConfigSpec.IntValue DRILL_PROCESS_TIME;
    public static final ModConfigSpec.IntValue DRILL_PRODUCTION_AMOUNT;
    public static final ModConfigSpec.BooleanValue INFINITE_RESERVES;

    // Scanner
    public static final ModConfigSpec.IntValue SCANNER_RADIUS;
    public static final ModConfigSpec.IntValue SCANNER_COOLDOWN;

    // Terminal
    public static final ModConfigSpec.IntValue STATS_SYNC_INTERVAL;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("storage");
        DATA_MODULE_CAPACITY = BUILDER
                .comment("Capacidad de almacenamiento aportada por cada Modulo de datos en un Rack (en items).",
                         "Por defecto: 4096")
                .defineInRange("dataModuleCapacity", 4096, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("energy");
        CORE_ENERGY_CONSUMPTION = BUILDER
                .comment("Consumo de energia del Nucleo en FE por tick mientras esta encendido.",
                         "Por defecto: 20")
                .defineInRange("coreEnergyConsumption", 20, 0, Integer.MAX_VALUE);
        CRUSHER_ENERGY_CONSUMPTION = BUILDER
                .comment("Consumo de energia del Triturador en FE por tick durante el procesamiento.",
                         "Por defecto: 40")
                .defineInRange("crusherEnergyConsumption", 40, 0, Integer.MAX_VALUE);
        DRILL_ENERGY_CONSUMPTION = BUILDER
                .comment("Consumo de energia de la Perforadora en FE por tick mientras mina.",
                         "Por defecto: 80")
                .defineInRange("drillEnergyConsumption", 80, 0, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("drill");
        DRILL_PROCESS_TIME = BUILDER
                .comment("Intervalo de produccion de la Perforadora en ticks por ciclo de minado (20 ticks = 1 segundo).",
                         "Por defecto: 40")
                .defineInRange("drillProcessTime", 40, 1, Integer.MAX_VALUE);
        DRILL_PRODUCTION_AMOUNT = BUILDER
                .comment("Cantidad de mineral crudo producido por ciclo por la Perforadora.",
                         "Por defecto: 1")
                .defineInRange("drillProductionAmount", 1, 1, 64);
        INFINITE_RESERVES = BUILDER
                .comment("Si esta activado, las vetas de mineral nunca se agotan al ser minadas por Perforadoras.",
                         "Por defecto: false")
                .define("infiniteReserves", false);
        BUILDER.pop();

        BUILDER.push("scanner");
        SCANNER_RADIUS = BUILDER
                .comment("Radio de busqueda en bloques para el Escaner de mano.",
                         "Por defecto: 96")
                .defineInRange("scannerRadius", 96, 1, 512);
        SCANNER_COOLDOWN = BUILDER
                .comment("Tiempo de recarga del Escaner en ticks tras realizar una prospeccion (20 ticks = 1 segundo).",
                         "Por defecto: 100")
                .defineInRange("scannerCooldown", 100, 0, 72000);
        BUILDER.pop();

        BUILDER.push("terminal");
        STATS_SYNC_INTERVAL = BUILDER
                .comment("Intervalo en ticks para la sincronizacion de paquetes de telemetria al abrir la pestana de estadisticas del Terminal.",
                         "Por defecto: 20")
                .defineInRange("statsSyncInterval", 20, 1, 1200);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private LogTechConfig() {
    }
}
