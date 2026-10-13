package io.github.chema280101.logtech;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class LogTechConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Storage
    public static final ModConfigSpec.IntValue DATA_MODULE_CAPACITY;
    public static final ModConfigSpec.IntValue COLD_DATA_MODULE_CAPACITY;
    public static final ModConfigSpec.IntValue DATA_MODULE_ADVANCED_MULTIPLIER;
    public static final ModConfigSpec.IntValue DATA_MODULE_MASTER_MULTIPLIER;
    public static final ModConfigSpec.IntValue TIER_POLICY_CYCLE_INTERVAL;
    public static final ModConfigSpec.DoubleValue TIER_POLICY_DECAY_RATE;
    public static final ModConfigSpec.DoubleValue TIER_POLICY_HOT_HIGH_WATERMARK;
    public static final ModConfigSpec.DoubleValue TIER_POLICY_HOT_LOW_WATERMARK;
    public static final ModConfigSpec.IntValue TIER_POLICY_MAX_ITEMS_PER_CYCLE;
    public static final ModConfigSpec.BooleanValue TIER_POLICY_DEBUG_LOG;
    public static final ModConfigSpec.IntValue COLD_RETRIEVAL_DELAY_TICKS;
    public static final ModConfigSpec.IntValue COLD_RETRIEVAL_MAX_ITEMS;
    public static final ModConfigSpec.IntValue MAX_RULES;
    public static final ModConfigSpec.IntValue SCREEN_REACH_DISTANCE;
    public static final ModConfigSpec.IntValue RULE_NOTIFICATION_RANGE;

    // Energy consumption
    public static final ModConfigSpec.IntValue CORE_ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue CORE_ENERGY_CONSUMPTION;
    public static final ModConfigSpec.IntValue CRUSHER_ENERGY_CONSUMPTION;
    public static final ModConfigSpec.IntValue DRILL_ENERGY_CONSUMPTION;
    public static final ModConfigSpec.IntValue WASHER_ENERGY_CONSUMPTION;
    public static final ModConfigSpec.IntValue CENTRIFUGE_ENERGY_CONSUMPTION;
    public static final ModConfigSpec.IntValue COAL_GENERATOR_GENERATION;
    public static final ModConfigSpec.IntValue COAL_GENERATOR_MAX_TRANSFER;
    public static final ModConfigSpec.IntValue COAL_GENERATOR_CAPACITY;
    public static final ModConfigSpec.IntValue ENERGY_DISTRIBUTION_INTERVAL;
    public static final ModConfigSpec.IntValue ENERGY_PRIORITY_HOLD_CYCLES;

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
        COLD_DATA_MODULE_CAPACITY = BUILDER
                .comment("Capacidad de almacenamiento aportada por cada Modulo de datos en un Rack Frio (en items).",
                         "Por defecto: 16384 (4 veces la del Rack Caliente)")
                .defineInRange("coldDataModuleCapacity", 16384, 1, Integer.MAX_VALUE);
        DATA_MODULE_ADVANCED_MULTIPLIER = BUILDER
                .comment("Multiplicador de capacidad para el Modulo de datos avanzado (Nivel 2).",
                         "Por defecto: 4 (x4 capacidad)")
                .defineInRange("dataModuleAdvancedMultiplier", 4, 1, 10000);
        DATA_MODULE_MASTER_MULTIPLIER = BUILDER
                .comment("Multiplicador de capacidad para el Modulo de datos maestro (Nivel 3).",
                         "Por defecto: 16 (x16 capacidad)")
                .defineInRange("dataModuleMasterMultiplier", 16, 1, 10000);
        TIER_POLICY_CYCLE_INTERVAL = BUILDER
                .comment("Intervalo en ticks entre ciclos de rebalanceo automatico entre niveles caliente y frio.",
                         "Por defecto: 200 (10 segundos)")
                .defineInRange("tierPolicyCycleInterval", 200, 20, 72000);
        TIER_POLICY_DECAY_RATE = BUILDER
                .comment("Factor de decaimiento aplicado a la puntuacion de extraccion cada ciclo (0.0 a 1.0).",
                         "Por defecto: 0.9")
                .defineInRange("tierPolicyDecayRate", 0.9, 0.0, 1.0);
        TIER_POLICY_HOT_HIGH_WATERMARK = BUILDER
                .comment("Porcentaje de uso del nivel caliente (0.0 a 1.0) a partir del cual se bajan items al frio.",
                         "Por defecto: 0.9 (90%)")
                .defineInRange("tierPolicyHotHighWatermark", 0.9, 0.1, 1.0);
        TIER_POLICY_HOT_LOW_WATERMARK = BUILDER
                .comment("Porcentaje objetivo de uso del nivel caliente (0.0 a 1.0) al que se busca descender bajando items.",
                         "Por defecto: 0.8 (80%)")
                .defineInRange("tierPolicyHotLowWatermark", 0.8, 0.0, 0.99);
        TIER_POLICY_MAX_ITEMS_PER_CYCLE = BUILDER
                .comment("Numero maximo de tipos de items distintos evaluados/movidos por ciclo para evitar caidas de rendimiento.",
                         "Por defecto: 50")
                .defineInRange("tierPolicyMaxItemsPerCycle", 50, 1, 10000);
        TIER_POLICY_DEBUG_LOG = BUILDER
                .comment("Habilita registros de depuracion detallados en la consola al ejecutar los ciclos de rebalanceo.",
                         "Por defecto: false")
                .define("tierPolicyDebugLog", false);
        COLD_RETRIEVAL_DELAY_TICKS = BUILDER
                .comment("Retraso en ticks para recuperar items desde el nivel frio en el Terminal (20 ticks = 1 segundo).",
                         "Por defecto: 40 (2 segundos)")
                .defineInRange("coldRetrievalDelayTicks", 40, 0, 72000);
        COLD_RETRIEVAL_MAX_ITEMS = BUILDER
                .comment("Cantidad maxima de items que se pueden pedir por solicitud de recuperacion desde el nivel frio.",
                         "Por defecto: 64")
                .defineInRange("coldRetrievalMaxItems", 64, 1, 64);
        MAX_RULES = BUILDER
                .comment("Numero maximo de reglas de automatizacion configurables en el Nucleo.",
                         "Por defecto: 8")
                .defineInRange("maxRules", 8, 1, 8);
        SCREEN_REACH_DISTANCE = BUILDER
                .comment("Distancia maxima en bloques entre el jugador y el bloque para interactuar con pantallas y terminales.",
                         "Por defecto: 8")
                .defineInRange("screenReachDistance", 8, 1, 64);
        RULE_NOTIFICATION_RANGE = BUILDER
                .comment("Radio en bloques alrededor del Nucleo en el que los jugadores reciben notificaciones por chat y sonido de las reglas.",
                         "Por defecto: 64")
                .defineInRange("ruleNotificationRange", 64, 1, 512);
        BUILDER.pop();

        BUILDER.push("energy");
        CORE_ENERGY_CAPACITY = BUILDER
                .comment("Capacidad del bufer interno de energia del Nucleo en FE.",
                         "Por defecto: 200000")
                .defineInRange("coreEnergyCapacity", 200000, 1000, Integer.MAX_VALUE);
        CORE_ENERGY_CONSUMPTION = BUILDER
                .comment("Consumo de energia del Nucleo en FE por tick mientras esta encendido.",
                         "Por defecto: 20")
                .defineInRange("coreEnergyConsumption", 20, 0, Integer.MAX_VALUE);
        CRUSHER_ENERGY_CONSUMPTION = BUILDER
                .comment("Consumo de energia del Triturador en FE por tick durante el procesamiento.",
                         "Por defecto: 40")
                .defineInRange("crusherEnergyConsumption", 40, 0, Integer.MAX_VALUE);
        WASHER_ENERGY_CONSUMPTION = BUILDER
                .comment("Consumo de energia de la Lavadora en FE por tick durante el procesamiento.",
                         "Por defecto: 60")
                .defineInRange("washerEnergyConsumption", 60, 0, Integer.MAX_VALUE);
        CENTRIFUGE_ENERGY_CONSUMPTION = BUILDER
                .comment("Consumo de energia de la Centrifuga en FE por tick durante el procesamiento.",
                         "Por defecto: 80")
                .defineInRange("centrifugeEnergyConsumption", 80, 0, Integer.MAX_VALUE);
        DRILL_ENERGY_CONSUMPTION = BUILDER
                .comment("Consumo de energia de la Perforadora en FE por tick mientras mina.",
                         "Por defecto: 80")
                .defineInRange("drillEnergyConsumption", 80, 0, Integer.MAX_VALUE);
        COAL_GENERATOR_GENERATION = BUILDER
                .comment("Generacion de energia del Generador de Carbon temporal en FE por tick mientras consume combustible.",
                         "Por defecto: 40")
                .defineInRange("coalGeneratorGeneration", 40, 1, Integer.MAX_VALUE);
        COAL_GENERATOR_MAX_TRANSFER = BUILDER
                .comment("Tasa maxima de transferencia de energia del Generador de Carbon en FE por tick.",
                         "Por defecto: 100")
                .defineInRange("coalGeneratorMaxTransfer", 100, 1, Integer.MAX_VALUE);
        COAL_GENERATOR_CAPACITY = BUILDER
                .comment("Capacidad del bufer interno de energia del Generador de Carbon temporal en FE.",
                         "Por defecto: 10000")
                .defineInRange("coalGeneratorCapacity", 10000, 1000, Integer.MAX_VALUE);
        ENERGY_DISTRIBUTION_INTERVAL = BUILDER
                .comment("Intervalo en ticks entre ciclos de reparto de energia por red desde el Nucleo (20 ticks = 1 segundo).",
                         "Por defecto: 20")
                .defineInRange("energyDistributionInterval", 20, 1, 1200);
        ENERGY_PRIORITY_HOLD_CYCLES = BUILDER
                .comment("Numero de ciclos de estabilizacion temporal para evitar parpadeos y oscilaciones en maquinas con prioridad baja o en el limite.",
                         "Por defecto: 3")
                .defineInRange("energyPriorityHoldCycles", 3, 1, 20);
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
