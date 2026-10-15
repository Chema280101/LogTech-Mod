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
    public static final ModConfigSpec.IntValue MAX_SAVED_VIEWS;
    public static final ModConfigSpec.IntValue SCREEN_REACH_DISTANCE;
    public static final ModConfigSpec.IntValue RULE_NOTIFICATION_RANGE;
    public static final ModConfigSpec.IntValue TRASH_CAPACITY;
    public static final ModConfigSpec.IntValue TRASH_LIFETIME_TICKS;
    public static final ModConfigSpec.BooleanValue QUARANTINE_ENABLED;
    public static final ModConfigSpec.IntValue QUARANTINE_CAPACITY;
    public static final ModConfigSpec.IntValue QUARANTINE_DRAIN_PER_CYCLE;
    public static final ModConfigSpec.IntValue COMPACT_MAX_CONVERSIONS_PER_CYCLE;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> COMPACT_BLACKLIST;
    public static final ModConfigSpec.IntValue RATE_TRACKER_MAX_ITEMS;

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

    // Heat mechanics
    public static final ModConfigSpec.DoubleValue HEAT_BASE_GENERATION;
    public static final ModConfigSpec.DoubleValue HEAT_COOLING_RATE;
    public static final ModConfigSpec.DoubleValue HEAT_HEATSINK_COOLING;
    public static final ModConfigSpec.DoubleValue HEAT_WATER_COOLING;
    public static final ModConfigSpec.DoubleValue HEAT_SLOWDOWN_THRESHOLD;
    public static final ModConfigSpec.DoubleValue HEAT_OVERHEAT_THRESHOLD;
    public static final ModConfigSpec.DoubleValue HEAT_RESUME_THRESHOLD;
    public static final ModConfigSpec.DoubleValue HEAT_SLOWDOWN_SPEED;

    // Terminal
    public static final ModConfigSpec.IntValue STATS_SYNC_INTERVAL;

    // Firmware
    public static final ModConfigSpec.DoubleValue FIRMWARE_ECO_SPEED;
    public static final ModConfigSpec.DoubleValue FIRMWARE_ECO_ENERGY;
    public static final ModConfigSpec.DoubleValue FIRMWARE_ECO_HEAT;
    public static final ModConfigSpec.DoubleValue FIRMWARE_TURBO_SPEED;
    public static final ModConfigSpec.DoubleValue FIRMWARE_TURBO_ENERGY;
    public static final ModConfigSpec.DoubleValue FIRMWARE_TURBO_HEAT;
    public static final ModConfigSpec.DoubleValue FIRMWARE_SAFE_OVERHEAT_THRESHOLD;
    public static final ModConfigSpec.DoubleValue FIRMWARE_SAFE_RESUME_THRESHOLD;

    // Security and Activity Log
    public static final ModConfigSpec.BooleanValue ACTIVITY_LOG_ENABLED;
    public static final ModConfigSpec.IntValue ACTIVITY_LOG_CAPACITY;
    public static final ModConfigSpec.IntValue ACTIVITY_LOG_GROUPING_WINDOW_TICKS;

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
        MAX_SAVED_VIEWS = BUILDER
                .comment("Numero maximo de vistas guardadas configurables en el Terminal.",
                         "Por defecto: 8")
                .defineInRange("maxSavedViews", 8, 1, 8);
        SCREEN_REACH_DISTANCE = BUILDER
                .comment("Distancia maxima en bloques entre el jugador y el bloque para interactuar con pantallas y terminales.",
                         "Por defecto: 8")
                .defineInRange("screenReachDistance", 8, 1, 64);
        RULE_NOTIFICATION_RANGE = BUILDER
                .comment("Radio en bloques alrededor del Nucleo en el que los jugadores reciben notificaciones por chat y sonido de las reglas.",
                         "Por defecto: 64")
                .defineInRange("ruleNotificationRange", 64, 1, 512);
        TRASH_CAPACITY = BUILDER
                .comment("Capacidad maxima de almacenamiento de items en la papelera de reciclaje del Nucleo.",
                         "Por defecto: 4096 items")
                .defineInRange("trashCapacity", 4096, 1, Integer.MAX_VALUE);
        TRASH_LIFETIME_TICKS = BUILDER
                .comment("Tiempo de vida en ticks de los items en la papelera del Nucleo antes de caducar (20 ticks = 1 seg, 36000 ticks = 30 min).",
                         "Por defecto: 36000 ticks")
                .defineInRange("trashLifetimeTicks", 36000, 20, Integer.MAX_VALUE);
        QUARANTINE_ENABLED = BUILDER
                .comment("Habilita el bufer de cuarentena para evitar perdida de items cuando la red esta llena, sobrecargada o apagada.",
                         "Por defecto: true")
                .define("quarantineEnabled", true);
        QUARANTINE_CAPACITY = BUILDER
                .comment("Capacidad maxima de almacenamiento de items en el bufer de cuarentena del Nucleo.",
                         "Por defecto: 1024 items")
                .defineInRange("quarantineCapacity", 1024, 1, Integer.MAX_VALUE);
        QUARANTINE_DRAIN_PER_CYCLE = BUILDER
                .comment("Cantidad maxima de items vaciados desde la cuarentena hacia la red por ciclo de 20 ticks cuando hay espacio.",
                         "Por defecto: 64 items")
                .defineInRange("quarantineDrainPerCycle", 64, 1, Integer.MAX_VALUE);
        COMPACT_MAX_CONVERSIONS_PER_CYCLE = BUILDER
                .comment("Cantidad maxima de conversiones de compactacion por ciclo de 20 ticks en el Nucleo.",
                         "Por defecto: 64")
                .defineInRange("compactMaxConversionsPerCycle", 64, 1, Integer.MAX_VALUE);
        COMPACT_BLACKLIST = BUILDER
                .comment("Lista de IDs de items (ej. \"minecraft:cobblestone\") excluidos de la compactacion automatica.",
                         "Por defecto: lista vacia")
                .defineList("compactBlacklist", java.util.List.of(), o -> o instanceof String);
        RATE_TRACKER_MAX_ITEMS = BUILDER
                .comment("Numero maximo de tipos de items distintos rastreados simultaneamente en memoria para acotar el uso de RAM.",
                         "Por defecto: 10000")
                .defineInRange("rateTrackerMaxItems", 10000, 100, 1_000_000);
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
        BUILDER.push("heat");
        HEAT_BASE_GENERATION = BUILDER
                .comment("Calor base generado por segundo (cada 20 ticks) mientras una maquina esta trabajando.",
                         "Por defecto: 2.0")
                .defineInRange("heatBaseGeneration", 2.0, 0.0, 100.0);
        HEAT_COOLING_RATE = BUILDER
                .comment("Tasa de enfriamiento y disipacion termica base por segundo.",
                         "Por defecto: 2.0")
                .defineInRange("heatCoolingRate", 2.0, 0.0, 100.0);
        HEAT_HEATSINK_COOLING = BUILDER
                .comment("Enfriamiento extra por segundo aportado por cada bloque disipador (heatsink) vecino adyacente.",
                         "Por defecto: 1.0")
                .defineInRange("heatHeatsinkCooling", 1.0, 0.0, 100.0);
        HEAT_WATER_COOLING = BUILDER
                .comment("Enfriamiento extra por segundo aportado por cada cara adyacente en contacto con agua (fuente o corriente).",
                         "Por defecto: 0.5")
                .defineInRange("heatWaterCooling", 0.5, 0.0, 100.0);
        HEAT_SLOWDOWN_THRESHOLD = BUILDER
                .comment("Temperatura a partir de la cual una maquina reduce su velocidad de trabajo a la mitad.",
                         "Por defecto: 70.0")
                .defineInRange("heatSlowdownThreshold", 70.0, 0.0, 100.0);
        HEAT_OVERHEAT_THRESHOLD = BUILDER
                .comment("Temperatura a la que una maquina se apaga por sobrecalentamiento.",
                         "Por defecto: 100.0")
                .defineInRange("heatOverheatThreshold", 100.0, 1.0, 100.0);
        HEAT_RESUME_THRESHOLD = BUILDER
                .comment("Temperatura a la que una maquina sobrecalentada vuelve a encenderse tras enfriarse (histeresis).",
                         "Por defecto: 40.0")
                .defineInRange("heatResumeThreshold", 40.0, 0.0, 99.0);
        HEAT_SLOWDOWN_SPEED = BUILDER
                .comment("Multiplicador de velocidad de procesamiento al superar el umbral de ralentizacion.",
                         "Por defecto: 0.5")
                .defineInRange("heatSlowdownSpeed", 0.5, 0.01, 1.0);
        BUILDER.pop();

        BUILDER.push("firmware");
        FIRMWARE_ECO_SPEED = BUILDER
                .comment("Multiplicador de velocidad de procesamiento para el chip Firmware Eco.",
                         "Por defecto: 0.7 (30% mas lento)")
                .defineInRange("firmwareEcoSpeed", 0.7, 0.05, 10.0);
        FIRMWARE_ECO_ENERGY = BUILDER
                .comment("Multiplicador de consumo de energia por tick para el chip Firmware Eco.",
                         "Por defecto: 0.5 (la mitad de energia)")
                .defineInRange("firmwareEcoEnergy", 0.5, 0.05, 10.0);
        FIRMWARE_ECO_HEAT = BUILDER
                .comment("Multiplicador de generacion de calor por segundo para el chip Firmware Eco.",
                         "Por defecto: 0.5 (la mitad de calor)")
                .defineInRange("firmwareEcoHeat", 0.5, 0.0, 10.0);

        FIRMWARE_TURBO_SPEED = BUILDER
                .comment("Multiplicador de velocidad de procesamiento para el chip Firmware Turbo.",
                         "Por defecto: 1.5 (50% mas rapido)")
                .defineInRange("firmwareTurboSpeed", 1.5, 0.05, 10.0);
        FIRMWARE_TURBO_ENERGY = BUILDER
                .comment("Multiplicador de consumo de energia por tick para el chip Firmware Turbo.",
                         "Por defecto: 2.0 (el doble de energia)")
                .defineInRange("firmwareTurboEnergy", 2.0, 0.05, 20.0);
        FIRMWARE_TURBO_HEAT = BUILDER
                .comment("Multiplicador de generacion de calor por segundo para el chip Firmware Turbo.",
                         "Por defecto: 3.0 (el triple de calor)")
                .defineInRange("firmwareTurboHeat", 3.0, 0.0, 20.0);

        FIRMWARE_SAFE_OVERHEAT_THRESHOLD = BUILDER
                .comment("Temperatura de apagado inmediato por seguridad para el chip Firmware Seguro (en °C).",
                         "Por defecto: 85.0")
                .defineInRange("firmwareSafeOverheatThreshold", 85.0, 1.0, 100.0);
        FIRMWARE_SAFE_RESUME_THRESHOLD = BUILDER
                .comment("Temperatura de reanudacion tras apagado por seguridad para el chip Firmware Seguro (en °C).",
                         "Por defecto: 30.0")
                .defineInRange("firmwareSafeResumeThreshold", 30.0, 0.0, 84.0);
        BUILDER.pop();

        BUILDER.push("security");
        ACTIVITY_LOG_ENABLED = BUILDER
                .comment("Habilita el registro de actividad de jugadores en el Nucleo de la red.",
                         "Por defecto: true")
                .define("activityLogEnabled", true);
        ACTIVITY_LOG_CAPACITY = BUILDER
                .comment("Capacidad maxima del bufer circular de entradas de registro de actividad en el Nucleo.",
                         "Por defecto: 500")
                .defineInRange("activityLogCapacity", 500, 10, 10000);
        ACTIVITY_LOG_GROUPING_WINDOW_TICKS = BUILDER
                .comment("Ventana temporal en ticks para agrupar acciones repetidas del mismo jugador en el registro de actividad (20 ticks = 1 segundo).",
                         "Por defecto: 200 (10 segundos)")
                .defineInRange("activityLogGroupingWindowTicks", 200, 0, 72000);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private LogTechConfig() {
    }
}
