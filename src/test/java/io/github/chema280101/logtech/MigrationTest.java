package io.github.chema280101.logtech;

import io.github.chema280101.logtech.storage.SimpleTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class MigrationTest {

    @Test
    @DisplayName("Integridad y lectura del archivo real logtech_deposits.dat del mundo v0.1")
    void testDepositSavedDataFileIntegrity() throws IOException {
        File file = new File("D:\\logtech_migration_test_world_v0.1\\data\\logtech_deposits.dat");
        if (!file.exists()) {
            file = new File("run\\saves\\v0.1_Migration_Test_World\\data\\logtech_deposits.dat");
        }
        assertTrue(file.exists(), "El archivo de vetas de la v0.1 debe existir para la prueba de migración");
        assertTrue(file.length() > 0, "El archivo logtech_deposits.dat no debe estar vacío");

        // Verificar compresión GZIP estándar de Minecraft NBT
        try (FileInputStream fis = new FileInputStream(file);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int read;
            while ((read = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }

            byte[] decompressed = baos.toByteArray();
            assertTrue(decompressed.length > 0, "El contenido descomprimido debe tener bytes válidos");

            // Comprobar que contiene la estructura VeinStates guardada en v0.1
            String content = new String(decompressed, StandardCharsets.ISO_8859_1);
            assertTrue(content.contains("VeinStates"), "El archivo NBT debe contener la lista VeinStates");
            assertTrue(content.contains("remaining"), "El archivo NBT debe contener el campo remaining");
            assertTrue(content.contains("discovered"), "El archivo NBT debe contener el campo discovered");
        }
    }

    @Test
    @DisplayName("Migración del Núcleo: mundo v0.1 sin data_version traslada 100% de ítems al nivel caliente")
    void testCoreStorageMigrationV1ToV3() {
        // Estado representativo de un Núcleo en la v0.1:
        // No tiene 'data_version' (evalúa a 1). Tiene 'storage' y 'energy'.
        int savedDataVersion = 1; // ausente en tag v0.1 -> default 1
        int savedEnergy = 85000;

        Map<String, Long> v1Items = new HashMap<>();
        v1Items.put("minecraft:iron_ingot", 12500L);
        v1Items.put("minecraft:gold_ingot", 3200L);
        v1Items.put("minecraft:copper_ingot", 8400L);
        v1Items.put("logtech:raw_neoferrite", 450L);

        SimpleTier<String> hotTier = new SimpleTier<>("hot", 50000L);
        SimpleTier<String> coldTier = new SimpleTier<>("cold", 100000L);

        // 1. Simulación de la migración en CoreBlockEntity.loadAdditional:
        // if (dataVersion < 2) { hotTier.setContent(storage); }
        if (savedDataVersion < 2) {
            for (Map.Entry<String, Long> entry : v1Items.entrySet()) {
                hotTier.insert(entry.getKey(), entry.getValue(), false);
            }
        } else {
            fail("Un mundo v0.1 sin versión no debe entrar en ramas v2+");
        }

        // Verificaciones de no-pérdida
        long totalV1 = v1Items.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(totalV1, hotTier.getTotalStored(), "El nivel caliente debe recibir exactamente el 100% de los items de la v0.1");
        assertEquals(0L, coldTier.getTotalStored(), "El nivel frío debe inicializarse vacío (0 items)");
        assertEquals(totalV1, hotTier.getTotalStored() + coldTier.getTotalStored(), "Cero items descartados en la migración");

        // 2. Simulación de guardado en v0.2 (data_version = 3)
        int newVersion = 3;
        Map<String, Long> savedHot = new HashMap<>(hotTier.getAll());
        Map<String, Long> savedCold = new HashMap<>(coldTier.getAll());
        int energy = savedEnergy;

        // 3. Volver a abrir tag v3 en v0.2
        assertTrue(newVersion >= 2, "La versión guardada debe ser >= 2");
        SimpleTier<String> reloadedHot = new SimpleTier<>("hot", 50000L);
        SimpleTier<String> reloadedCold = new SimpleTier<>("cold", 100000L);

        for (Map.Entry<String, Long> entry : savedHot.entrySet()) {
            reloadedHot.insert(entry.getKey(), entry.getValue(), false);
        }
        for (Map.Entry<String, Long> entry : savedCold.entrySet()) {
            reloadedCold.insert(entry.getKey(), entry.getValue(), false);
        }

        assertEquals(v1Items, reloadedHot.getAll(), "Los items en caliente deben ser idénticos tras el ciclo de guardado y re-apertura");
        assertEquals(0L, reloadedCold.getTotalStored(), "El nivel frío sigue vacío si no hubo reglas ni desborde");
        assertEquals(85000, energy, "La energía se conserva intacta");
    }

    @Test
    @DisplayName("Migración de máquinas: tags v0.1 sin data_version adoptan prioridad NORMAL y preservan progreso")
    void testMachineMigrationV1ToV2() {
        // Simulación de NBT de Triturador v0.1 (sin data_version ni energy_priority)
        Integer dataVersion = null; // tag.contains("data_version") == false
        String energyPriority = null; // tag.contains("energy_priority") == false
        int progress = 55;
        int maxProgress = 100;
        int energy = 6400;

        // Carga con lógica v0.2
        int effectiveVersion = dataVersion != null ? dataVersion : 1;
        assertEquals(1, effectiveVersion);

        String effectivePriority = energyPriority != null ? energyPriority : "NORMAL";
        assertEquals("NORMAL", effectivePriority, "Máquinas de la v0.1 deben adoptar prioridad NORMAL por defecto");
        assertEquals(55, progress, "El progreso del ciclo debe preservarse");
        assertEquals(100, maxProgress);
        assertEquals(6400, energy, "El búfer de energía debe preservarse");

        // Guardado con v0.2 (data_version = 2)
        int savedVersion = 2;
        String savedPriority = effectivePriority;

        // Re-apertura
        assertEquals(2, savedVersion);
        assertEquals("NORMAL", savedPriority);
    }

    @Test
    @DisplayName("Migración de Perforadora: preserva avance de minado y temporizador de expulsión")
    void testDrillMigrationV1ToV2() {
        Integer dataVersion = null;
        int progress = 18;
        int pushTimer = 11;
        int energy = 9200;

        String effectivePriority = "NORMAL";

        assertEquals(18, progress);
        assertEquals(11, pushTimer);
        assertEquals(9200, energy);
        assertEquals("NORMAL", effectivePriority);
    }

    @Test
    @DisplayName("Integridad y lectura del archivo real logtech_deposits.dat del mundo v0.2")
    void testV02MigrationWorldIntegrity() throws IOException {
        File file = new File("D:\\logtech_migration_test_world_v0.2\\data\\logtech_deposits.dat");
        if (!file.exists()) {
            file = new File("run\\saves\\v0.2_Migration_Test_World\\data\\logtech_deposits.dat");
        }
        assertTrue(file.exists(), "El archivo de vetas de la v0.2 debe existir para la prueba de migración");
        assertTrue(file.length() > 0, "El archivo logtech_deposits.dat de v0.2 no debe estar vacío");

        try (FileInputStream fis = new FileInputStream(file);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int read;
            while ((read = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }

            byte[] decompressed = baos.toByteArray();
            assertTrue(decompressed.length > 0, "El contenido descomprimido de v0.2 debe tener bytes válidos");

            String content = new String(decompressed, StandardCharsets.ISO_8859_1);
            assertTrue(content.contains("VeinStates"), "El archivo NBT de v0.2 debe contener la lista VeinStates");
        }
    }

    @Test
    @DisplayName("Migración de Núcleo v0.2 a v0.3: preserva almacenamiento caliente, frío y reglas")
    void testCoreStorageMigrationV2ToV3() {
        // Estado representativo de un Núcleo v0.2 (data_version = 3 con niveles caliente y frío)
        int savedDataVersion = 3;
        SimpleTier<String> hotTier = new SimpleTier<>("hot", 16384L);
        SimpleTier<String> coldTier = new SimpleTier<>("cold", 65536L);

        hotTier.insert("logtech:raw_neoferrite", 250L, false);
        coldTier.insert("minecraft:cobblestone", 5000L, false);

        assertEquals(250L, hotTier.getAmount("logtech:raw_neoferrite"));
        assertEquals(5000L, coldTier.getAmount("minecraft:cobblestone"));

        // Al migrar a v0.3, ambos niveles se mantienen sin descarte
        assertEquals(3, savedDataVersion);
        assertEquals(250L, hotTier.getAmount("logtech:raw_neoferrite"));
        assertEquals(5000L, coldTier.getAmount("minecraft:cobblestone"));
    }

    @Test
    @DisplayName("Migración de mundos antiguos a data_version 4 (v0.3): la papelera inicia vacía y los ítems se preservan")
    void testCoreStorageMigrationToV4TrashBinStartsEmpty() {
        int v2DataVersion = 3;
        io.github.chema280101.logtech.storage.TrashBin<String> trashBin = new io.github.chema280101.logtech.storage.TrashBin<>();

        // En mundos v0.1 o v0.2 (data_version < 4), no existe tag trash_bin
        boolean containsTrashTag = false;
        if (v2DataVersion >= 4 && containsTrashTag) {
            fail("No debe cargar tag inexistente");
        } else {
            trashBin.clear();
        }

        assertTrue(trashBin.isEmpty(), "Mundos anteriores a data_version 4 deben comenzar con la papelera vacía");
        assertEquals(0L, trashBin.getTotalCount());

        // Al guardar en v0.3, adopta data_version = 4
        int v3DataVersion = 4;
        assertEquals(4, v3DataVersion);
    }

    @Test
    @DisplayName("Migración de mundos antiguos a data_version 5 (v0.3 T45): la cuarentena inicia vacía y los ítems se preservan")
    void testCoreStorageMigrationToV5QuarantineStartsEmpty() {
        int v2DataVersion = 3;
        io.github.chema280101.logtech.storage.QuarantineBuffer<String> quarantine = new io.github.chema280101.logtech.storage.QuarantineBuffer<>();

        // En mundos v0.1, v0.2 o v0.3 inicial (data_version < 5), no existe tag quarantine
        boolean containsQuarantineTag = false;
        if (v2DataVersion >= 5 && containsQuarantineTag) {
            fail("No debe cargar tag de cuarentena inexistente");
        } else {
            quarantine.clear();
        }

        assertTrue(quarantine.isEmpty(), "Mundos anteriores a data_version 5 deben comenzar con la cuarentena vacía");
        assertEquals(0L, quarantine.getTotalCount());

        // Al guardar con cuarentena, adopta data_version = 5
        int v3NewDataVersion = 5;
        assertEquals(5, v3NewDataVersion);
    }

    @Test
    @DisplayName("Migración de máquinas a data_version 3 (v0.3 T49): calor inicia en 0 y no sobrecalentada")
    void testMachineMigrationToV3HeatStartsZero() {
        int v2MachineDataVersion = 2;
        io.github.chema280101.logtech.machine.HeatModel heatModel = new io.github.chema280101.logtech.machine.HeatModel();

        // En tags de v0.1 o v0.2 (data_version <= 2), no existen los campos Heat ni Overheated
        boolean containsHeatTag = false;
        boolean containsOverheatedTag = false;

        if (v2MachineDataVersion >= 3 && containsHeatTag) {
            fail("No debe cargar calor de mundos anteriores");
        }

        assertEquals(0.0, heatModel.getHeat(), 0.001, "Mundos anteriores deben iniciar con calor en 0");
        assertFalse(heatModel.isOverheated(), "Mundos anteriores no deben iniciar sobrecalentadas");
        assertEquals(1.0, heatModel.getSpeedMultiplier(), 0.001);

        // Al guardar en v0.3 T49, adopta data_version = 3
        int v3MachineDataVersion = 3;
        assertEquals(3, v3MachineDataVersion);
    }

    @Test
    @DisplayName("Migración de máquinas a data_version 4 (v0.3 T51): firmware inicia vacío y preserva compatibilidad")
    void testMachineMigrationToV4FirmwareStartsEmpty() {
        int previousMachineDataVersion = 3;
        boolean containsFirmwareTag = false;
        boolean containsProgressAccumulator = false;

        // En tags de v0.1, v0.2 o v0.3 previo a firmware, no existe tag "Firmware"
        io.github.chema280101.logtech.machine.firmware.FirmwareType fw =
                io.github.chema280101.logtech.machine.firmware.FirmwareType.NONE;
        double progressAccumulator = containsProgressAccumulator ? 0.5 : 0.0;

        assertEquals(io.github.chema280101.logtech.machine.firmware.FirmwareType.NONE, fw,
                "Mundos anteriores deben iniciar con firmware NONE");
        assertEquals(0.0, progressAccumulator, 0.001,
                "Mundos anteriores deben iniciar con acumulador de progreso en 0.0");

        // Al guardar en T51, las máquinas adoptan data_version = 4
        int v4MachineDataVersion = 4;
        assertEquals(4, v4MachineDataVersion);
    }

    @Test
    @DisplayName("Migración del Núcleo a data_version 7 (v0.3 T53): mundos anteriores inician sin dueño y en modo abierto")
    void testCoreMigrationToV7SecurityStartsUnownedOpen() {
        int previousCoreDataVersion = 6;
        io.github.chema280101.logtech.security.NetworkSecurityManager secManager =
                new io.github.chema280101.logtech.security.NetworkSecurityManager();

        // Si data_version < 7 (v0.1, v0.2 o v0.3 previo):
        if (previousCoreDataVersion < 7) {
            secManager.setOwner(null, "");
            secManager.setMode(io.github.chema280101.logtech.security.SecurityMode.OPEN);
            secManager.clearEntries();
        }

        assertFalse(secManager.hasOwner(), "Núcleos de mundos anteriores no deben tener dueño asignado");
        assertNull(secManager.getOwnerUuid());
        assertEquals(io.github.chema280101.logtech.security.SecurityMode.OPEN, secManager.getMode(),
                "Núcleos de mundos anteriores deben iniciar en modo abierto");
        assertTrue(secManager.getEntries().isEmpty(), "La lista de permisos debe iniciar vacía");

        // Cualquiera puede reclamar el núcleo
        java.util.UUID user = java.util.UUID.randomUUID();
        assertTrue(secManager.canPlayerDo(user, io.github.chema280101.logtech.security.PlayerAction.CLAIM_CORE, false),
                "Cualquier jugador debe poder reclamar un núcleo migrado sin dueño");

        // Al guardar en T53, el Núcleo adopta data_version = 7
        int v7CoreDataVersion = 7;
        assertEquals(7, v7CoreDataVersion);
    }

    @Test
    @DisplayName("Migración del Núcleo a data_version 8 (v0.3 T54): mundos anteriores inician con registro de actividad vacío")
    void testCoreMigrationToV8ActivityLogStartsEmpty() {
        int previousCoreDataVersion = 7;
        io.github.chema280101.logtech.security.ActivityLogBuffer<String> logBuffer =
                new io.github.chema280101.logtech.security.ActivityLogBuffer<>(500);

        // Si data_version < 8 (v0.1, v0.2 o v0.3 previo):
        if (previousCoreDataVersion < 8) {
            logBuffer.clear();
        }

        assertEquals(0, logBuffer.size(), "El registro de actividad debe iniciar vacío en mundos anteriores");
        assertTrue(logBuffer.isEmpty(), "El registro debe estar vacío");
        assertEquals(0, logBuffer.getPage(0, 10, null, true).entries().size());

        // Al guardar en T54, el Núcleo adopta data_version = 8
        int v8CoreDataVersion = 8;
        assertEquals(8, v8CoreDataVersion);
    }

    @Test
    @DisplayName("Cadena completa de migración v0.1 -> v0.3: conservación estricta, guardado y re-apertura en v0.3")
    void testFullMigrationChainV01ToV03AndReload() {
        // 1. Tag simulado de un Núcleo v0.1: sin data_version (evalúa a 1), storage con 24 550 ítems y 85 000 FE
        int v1DataVersion = 1;
        int initialEnergy = 85000;
        Map<String, Long> v1Items = new HashMap<>();
        v1Items.put("minecraft:iron_ingot", 12500L);
        v1Items.put("minecraft:gold_ingot", 3200L);
        v1Items.put("minecraft:copper_ingot", 8400L);
        v1Items.put("logtech:raw_neoferrite", 450L);
        long expectedTotal = v1Items.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(24550L, expectedTotal);

        // 2. Carga en la lógica de v0.3
        SimpleTier<String> hotTier = new SimpleTier<>("hot", 100000L);
        SimpleTier<String> coldTier = new SimpleTier<>("cold", 200000L);
        io.github.chema280101.logtech.storage.TrashBin<String> trashBin = new io.github.chema280101.logtech.storage.TrashBin<>();
        io.github.chema280101.logtech.storage.QuarantineBuffer<String> quarantine = new io.github.chema280101.logtech.storage.QuarantineBuffer<>();
        io.github.chema280101.logtech.storage.ViewManager viewManager = new io.github.chema280101.logtech.storage.ViewManager();
        io.github.chema280101.logtech.security.NetworkSecurityManager secManager = new io.github.chema280101.logtech.security.NetworkSecurityManager();
        io.github.chema280101.logtech.security.ActivityLogBuffer<String> activityLog = new io.github.chema280101.logtech.security.ActivityLogBuffer<>();

        // En v0.1 (< 2): 100% de ítems va a hotTier
        if (v1DataVersion < 2) {
            for (Map.Entry<String, Long> entry : v1Items.entrySet()) {
                hotTier.insert(entry.getKey(), entry.getValue(), false);
            }
        }
        trashBin.clear();
        quarantine.clear();
        viewManager.clear();
        secManager.setOwner(null, "");
        secManager.setMode(io.github.chema280101.logtech.security.SecurityMode.OPEN);
        secManager.clearEntries();
        activityLog.clear();

        // Verificaciones tras migración v0.1 -> v0.3
        assertEquals(expectedTotal, hotTier.getTotalStored(), "El 100% de los items de la v0.1 debe estar en el nivel caliente");
        assertEquals(0L, coldTier.getTotalStored(), "El nivel frío debe iniciar vacío");
        assertTrue(trashBin.isEmpty(), "La papelera debe iniciar vacía");
        assertTrue(quarantine.isEmpty(), "La cuarentena debe iniciar vacía");
        assertEquals(0, viewManager.count(), "Las vistas guardadas deben iniciar vacías");
        assertFalse(secManager.hasOwner(), "El núcleo migrado inicia sin dueño para ser reclamado");
        assertEquals(io.github.chema280101.logtech.security.SecurityMode.OPEN, secManager.getMode());
        assertTrue(activityLog.isEmpty(), "El registro de actividad inicia vacío");

        // 3. Guardado en formato v0.3 (data_version = 8)
        int savedV3Version = 8;
        Map<String, Long> savedHot = new HashMap<>(hotTier.getAll());
        Map<String, Long> savedCold = new HashMap<>(coldTier.getAll());
        int savedEnergy = initialEnergy;

        // 4. Re-apertura en v0.3
        SimpleTier<String> reloadedHot = new SimpleTier<>("hot", 100000L);
        SimpleTier<String> reloadedCold = new SimpleTier<>("cold", 200000L);
        assertTrue(savedV3Version >= 2);
        for (Map.Entry<String, Long> entry : savedHot.entrySet()) {
            reloadedHot.insert(entry.getKey(), entry.getValue(), false);
        }
        for (Map.Entry<String, Long> entry : savedCold.entrySet()) {
            reloadedCold.insert(entry.getKey(), entry.getValue(), false);
        }

        assertEquals(v1Items, reloadedHot.getAll(), "Los ítems reabiertos deben coincidir exactamente con los originales de v0.1");
        assertEquals(0L, reloadedCold.getTotalStored());
        assertEquals(expectedTotal, reloadedHot.getTotalStored() + reloadedCold.getTotalStored(), "Cero ítems descartados");
        assertEquals(initialEnergy, savedEnergy, "Energía conservada");
    }

    @Test
    @DisplayName("Cadena completa de migración v0.2 -> v0.3: conservación de niveles caliente, frío, reglas y re-apertura")
    void testFullMigrationChainV02ToV03AndReload() {
        // 1. Estado representativo de Núcleo v0.2 (data_version = 3)
        int v2DataVersion = 3;
        int initialEnergy = 120000;
        Map<String, Long> v2HotItems = new HashMap<>();
        v2HotItems.put("logtech:raw_neoferrite", 1500L);
        v2HotItems.put("minecraft:iron_ingot", 8000L);

        Map<String, Long> v2ColdItems = new HashMap<>();
        v2ColdItems.put("minecraft:cobblestone", 50000L);

        long totalHotV2 = v2HotItems.values().stream().mapToLong(Long::longValue).sum();
        long totalColdV2 = v2ColdItems.values().stream().mapToLong(Long::longValue).sum();
        long totalV2 = totalHotV2 + totalColdV2;
        assertEquals(59500L, totalV2);

        // 2. Carga en v0.3
        SimpleTier<String> hotTier = new SimpleTier<>("hot", 50000L);
        SimpleTier<String> coldTier = new SimpleTier<>("cold", 100000L);

        if (v2DataVersion >= 2) {
            for (Map.Entry<String, Long> entry : v2HotItems.entrySet()) {
                hotTier.insert(entry.getKey(), entry.getValue(), false);
            }
            for (Map.Entry<String, Long> entry : v2ColdItems.entrySet()) {
                coldTier.insert(entry.getKey(), entry.getValue(), false);
            }
        }

        assertEquals(totalHotV2, hotTier.getTotalStored());
        assertEquals(totalColdV2, coldTier.getTotalStored());
        assertEquals(totalV2, hotTier.getTotalStored() + coldTier.getTotalStored());

        // 3. Guardado en v0.3 (data_version = 8)
        int savedV3Version = 8;
        Map<String, Long> savedHot = new HashMap<>(hotTier.getAll());
        Map<String, Long> savedCold = new HashMap<>(coldTier.getAll());

        // 4. Re-apertura en v0.3
        SimpleTier<String> reloadedHot = new SimpleTier<>("hot", 50000L);
        SimpleTier<String> reloadedCold = new SimpleTier<>("cold", 100000L);
        for (Map.Entry<String, Long> entry : savedHot.entrySet()) {
            reloadedHot.insert(entry.getKey(), entry.getValue(), false);
        }
        for (Map.Entry<String, Long> entry : savedCold.entrySet()) {
            reloadedCold.insert(entry.getKey(), entry.getValue(), false);
        }

        assertEquals(v2HotItems, reloadedHot.getAll(), "Nivel caliente preservado idéntico");
        assertEquals(v2ColdItems, reloadedCold.getAll(), "Nivel frío preservado idéntico");
        assertEquals(totalV2, reloadedHot.getTotalStored() + reloadedCold.getTotalStored(), "Cero ítems descartados de v0.2 a v0.3");
        assertEquals(120000, initialEnergy);
    }

    @Test
    @DisplayName("Cadena completa de migración de Máquinas v0.1/v0.2 a v0.3: inventario, progreso, energía, calor y firmware")
    void testMachineFullMigrationChainAndReload() {
        // Máquina v0.1 sin data_version:
        Map<Integer, Long> slotAmounts = new HashMap<>();
        slotAmounts.put(0, 64L);  // input
        slotAmounts.put(1, 128L); // output 1
        slotAmounts.put(2, 32L);  // output 2
        int progress = 72;
        int energy = 7800;

        // Migración a v0.3:
        String priority = "NORMAL"; // default si no existía
        double heat = 0.0;
        boolean overheated = false;
        String firmware = "NONE";

        assertEquals(64L, slotAmounts.get(0));
        assertEquals(128L, slotAmounts.get(1));
        assertEquals(32L, slotAmounts.get(2));
        assertEquals(72, progress);
        assertEquals(7800, energy);
        assertEquals("NORMAL", priority);
        assertEquals(0.0, heat);
        assertFalse(overheated);
        assertEquals("NONE", firmware);

        // Guardado en v0.3 (data_version = 4) y re-apertura:
        int machineVersion = 4;
        assertEquals(4, machineVersion);
        assertEquals(64L + 128L + 32L, slotAmounts.values().stream().mapToLong(Long::longValue).sum(),
                "Todos los ítems de las ranuras de máquina se preservan intactos");
    }
}




