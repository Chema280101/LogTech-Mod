package io.github.chema280101.logtech.machine.recipe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CentrifugingSimulationTest {

    @Test
    @DisplayName("Simulación Monte Carlo (100.000 ciclos): probabilidades de escoria según pureza")
    void testSlagProbabilitiesSimulation() {
        int cycles = 100_000;
        Random random = new Random(42); // Semilla fija para reproducibilidad

        float baseSlagChance = 0.8f;
        double[] expectedRates = {0.80, 0.64, 0.48, 0.32, 0.16};

        for (int p = 1; p <= 5; p++) {
            float effectiveChance = ProcessingLogic.calculateEffectiveChance(baseSlagChance, OutputScaling.INVERSE_PURITY, p);
            double expected = expectedRates[p - 1];

            assertEquals(expected, effectiveChance, 0.0001, "La probabilidad efectiva teórica para pureza " + p + " debe ser " + expected);

            int producedCount = 0;
            for (int i = 0; i < cycles; i++) {
                if (ProcessingLogic.shouldProduce(effectiveChance, random.nextDouble())) {
                    producedCount++;
                }
            }

            double actualRate = (double) producedCount / cycles;
            // Tolerancia de 0.01 (1%) para 100.000 iteraciones
            assertEquals(expected, actualRate, 0.01,
                    String.format("La tasa simulada para pureza %d (%.4f) debe estar cerca de la esperada (%.4f)", p, actualRate, expected));
        }
    }

    @Test
    @DisplayName("Simulación Monte Carlo (100.000 ciclos): fragmento de cristal desde lodo")
    void testSludgeCrystalShardSimulation() {
        int cycles = 100_000;
        Random random = new Random(1337);

        float baseCrystalChance = 0.25f;
        float effectiveChance = ProcessingLogic.calculateEffectiveChance(baseCrystalChance, OutputScaling.NONE, 1);
        assertEquals(0.25f, effectiveChance, 0.0001f);

        int producedCount = 0;
        for (int i = 0; i < cycles; i++) {
            if (ProcessingLogic.shouldProduce(effectiveChance, random.nextDouble())) {
                producedCount++;
            }
        }

        double actualRate = (double) producedCount / cycles;
        assertEquals(0.25, actualRate, 0.01,
                String.format("La tasa simulada de crystal_shard (%.4f) debe estar cerca del 25%%", actualRate));
    }

    @Test
    @DisplayName("Prueba a escala de juego (100 polvos pureza 1 producen ~80 escorias)")
    void testGameScale100Powders() {
        Random random = new Random(2026);
        float effectiveChance = ProcessingLogic.calculateEffectiveChance(0.8f, OutputScaling.INVERSE_PURITY, 1);

        int slagCount = 0;
        for (int i = 0; i < 100; i++) {
            if (ProcessingLogic.shouldProduce(effectiveChance, random.nextDouble())) {
                slagCount++;
            }
        }

        // Con 100 tiradas y p=0.8, casi siempre está en [68, 92]
        assertTrue(slagCount >= 65 && slagCount <= 95,
                "100 polvos de pureza 1 deben dar alrededor de 80 escorias (obtenido: " + slagCount + ")");
    }

    @Test
    @DisplayName("Generación de texturas pixel-art para la Centrífuga, Escoria, Lodo y Fragmento")
    void testGenerateTextures() throws IOException {
        File blockDir = new File("src/main/resources/assets/logtech/textures/block");
        File itemDir = new File("src/main/resources/assets/logtech/textures/item");
        blockDir.mkdirs();
        itemDir.mkdirs();

        // 1. centrifuge.png
        BufferedImage centrifugeImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int cDarkBorder = 0xFF1E293B;
        int cMetalLight = 0xFF475569;
        int cMetalBase = 0xFF334155;
        int cRotorRim = 0xFF0F172A;
        int cRotorCyan = 0xFF06B6D4;
        int cRotorLight = 0xFF22D3EE;
        int cHub = 0xFFE2E8F0;

        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                centrifugeImg.setRGB(x, y, cMetalBase);
            }
        }
        for (int i = 0; i < 16; i++) {
            centrifugeImg.setRGB(i, 0, cDarkBorder);
            centrifugeImg.setRGB(i, 15, cDarkBorder);
            centrifugeImg.setRGB(0, i, cDarkBorder);
            centrifugeImg.setRGB(15, i, cDarkBorder);
        }
        for (int i = 1; i < 15; i++) {
            centrifugeImg.setRGB(i, 1, cMetalLight);
            centrifugeImg.setRGB(1, i, cMetalLight);
        }
        // Tambor circular central
        for (int y = 4; y <= 11; y++) {
            for (int x = 4; x <= 11; x++) {
                double dist = Math.sqrt((x - 7.5) * (x - 7.5) + (y - 7.5) * (y - 7.5));
                if (dist <= 3.8) {
                    centrifugeImg.setRGB(x, y, cRotorRim);
                }
            }
        }
        // Aspas de la centrífuga en cruz girada
        centrifugeImg.setRGB(7, 5, cRotorCyan);
        centrifugeImg.setRGB(8, 5, cRotorLight);
        centrifugeImg.setRGB(7, 10, cRotorCyan);
        centrifugeImg.setRGB(8, 10, cRotorLight);
        centrifugeImg.setRGB(5, 7, cRotorCyan);
        centrifugeImg.setRGB(5, 8, cRotorLight);
        centrifugeImg.setRGB(10, 7, cRotorCyan);
        centrifugeImg.setRGB(10, 8, cRotorLight);
        // Eje central
        centrifugeImg.setRGB(7, 7, cHub);
        centrifugeImg.setRGB(8, 7, cHub);
        centrifugeImg.setRGB(7, 8, cHub);
        centrifugeImg.setRGB(8, 8, cHub);

        File centrifugeFile = new File(blockDir, "centrifuge.png");
        ImageIO.write(centrifugeImg, "PNG", centrifugeFile);
        assertTrue(centrifugeFile.exists());

        // 2. slag_block.png
        BufferedImage slagBlockImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int[] slagPalette = {0xFF1C1917, 0xFF262626, 0xFF2E2E2E, 0xFF383838, 0xFF44403C, 0xFF78350F, 0xFF52525B};
        Random pRng = new Random(777);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int col = slagPalette[pRng.nextInt(slagPalette.length)];
                slagBlockImg.setRGB(x, y, col);
            }
        }
        // Bordes de bloque
        for (int i = 0; i < 16; i++) {
            slagBlockImg.setRGB(i, 0, 0xFF18181B);
            slagBlockImg.setRGB(i, 15, 0xFF18181B);
            slagBlockImg.setRGB(0, i, 0xFF18181B);
            slagBlockImg.setRGB(15, i, 0xFF18181B);
        }
        File slagBlockFile = new File(blockDir, "slag_block.png");
        ImageIO.write(slagBlockImg, "PNG", slagBlockFile);
        assertTrue(slagBlockFile.exists());

        // 3. slag.png (ítem de escoria)
        BufferedImage slagItemImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int cSlagDark = 0xFF1C1917;
        int cSlagMid = 0xFF292524;
        int cSlagLight = 0xFF44403C;
        int cSlagFleck = 0xFFB45309;

        // Trozo irregular de escoria
        for (int y = 4; y <= 11; y++) {
            for (int x = 4; x <= 11; x++) {
                double dist = Math.hypot(x - 7.5, y - 7.5);
                if (dist <= 3.6) {
                    slagItemImg.setRGB(x, y, cSlagMid);
                }
            }
        }
        slagItemImg.setRGB(5, 5, cSlagLight);
        slagItemImg.setRGB(6, 6, cSlagLight);
        slagItemImg.setRGB(8, 8, cSlagFleck);
        slagItemImg.setRGB(9, 7, cSlagFleck);
        slagItemImg.setRGB(10, 10, cSlagDark);
        slagItemImg.setRGB(6, 11, cSlagDark);
        slagItemImg.setRGB(11, 8, cSlagDark);

        File slagItemFile = new File(itemDir, "slag.png");
        ImageIO.write(slagItemImg, "PNG", slagItemFile);
        assertTrue(slagItemFile.exists());

        // 4. crystal_shard.png (fragmento de cristal)
        BufferedImage crystalShardImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int cShardCyan = 0xFF06B6D4;
        int cShardBright = 0xFF67E8F9;
        int cShardGlow = 0xFFCFFAFE;
        int cShardDark = 0xFF0E7490;

        // Cristal afilado en diagonal
        crystalShardImg.setRGB(11, 4, cShardGlow);
        crystalShardImg.setRGB(10, 5, cShardBright);
        crystalShardImg.setRGB(9, 6, cShardBright);
        crystalShardImg.setRGB(8, 7, cShardCyan);
        crystalShardImg.setRGB(7, 8, cShardCyan);
        crystalShardImg.setRGB(6, 9, cShardDark);
        crystalShardImg.setRGB(5, 10, cShardDark);
        crystalShardImg.setRGB(4, 11, cShardDark);

        crystalShardImg.setRGB(10, 4, cShardBright);
        crystalShardImg.setRGB(9, 5, cShardCyan);
        crystalShardImg.setRGB(8, 6, cShardCyan);
        crystalShardImg.setRGB(7, 7, cShardDark);
        crystalShardImg.setRGB(6, 8, cShardDark);

        File crystalShardFile = new File(itemDir, "crystal_shard.png");
        ImageIO.write(crystalShardImg, "PNG", crystalShardFile);
        assertTrue(crystalShardFile.exists());

        // 5. sludge.png (lodo)
        BufferedImage sludgeImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int cMudDark = 0xFF271B14;
        int cMudMid = 0xFF3D2B1F;
        int cMudLight = 0xFF5C4033;
        int cMudWet = 0xFF704D3B;

        for (int y = 5; y <= 11; y++) {
            for (int x = 4; x <= 12; x++) {
                double dist = Math.hypot((x - 8.0) * 0.9, y - 8.0);
                if (dist <= 3.4) {
                    sludgeImg.setRGB(x, y, cMudMid);
                }
            }
        }
        sludgeImg.setRGB(6, 6, cMudLight);
        sludgeImg.setRGB(7, 6, cMudWet);
        sludgeImg.setRGB(8, 7, cMudLight);
        sludgeImg.setRGB(10, 8, cMudWet);
        sludgeImg.setRGB(5, 10, cMudDark);
        sludgeImg.setRGB(9, 10, cMudDark);
        sludgeImg.setRGB(11, 9, cMudDark);

        File sludgeFile = new File(itemDir, "sludge.png");
        ImageIO.write(sludgeImg, "PNG", sludgeFile);
        assertTrue(sludgeFile.exists());
    }
}
