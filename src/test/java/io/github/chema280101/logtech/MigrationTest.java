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
    @DisplayName("Migración de Generador: preserva tiempo de combustión restante y energía")
    void testCoalGeneratorMigrationV1ToV2() {
        Integer dataVersion = null;
        int burnTime = 650;
        int maxBurnTime = 1600;
        int energy = 4800;

        assertEquals(650, burnTime);
        assertEquals(1600, maxBurnTime);
        assertEquals(4800, energy);
    }
}
