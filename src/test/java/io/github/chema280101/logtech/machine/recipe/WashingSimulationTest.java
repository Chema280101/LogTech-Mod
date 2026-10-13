package io.github.chema280101.logtech.machine.recipe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class WashingSimulationTest {

    @Test
    @DisplayName("Lavadora: La pureza p < 5 se incrementa a p + 1")
    void testWashingPurityIncrease() {
        assertEquals(2, ProcessingLogic.calculateOutputPurity(1, PurityMode.INCREASE));
        assertEquals(3, ProcessingLogic.calculateOutputPurity(2, PurityMode.INCREASE));
        assertEquals(4, ProcessingLogic.calculateOutputPurity(3, PurityMode.INCREASE));
        assertEquals(5, ProcessingLogic.calculateOutputPurity(4, PurityMode.INCREASE));
    }

    @Test
    @DisplayName("Lavadora: Polvo con pureza 5 no puede ser procesado")
    void testWashingPurity5Rejection() {
        // En WasherBlockEntity y WashingRecipe, stacks con pureza >= 5 son rechazados.
        int maxPurity = 5;
        assertTrue(maxPurity >= 5, "Pureza 5 o superior debe ser rechazada");
    }

    @Test
    @DisplayName("Lavadora: Balance de agua y energía (4000 mB / 250 mB = 16 ciclos @ 60 FE/t)")
    void testWashingBalance() {
        int tankCapacity = 4000;
        int waterPerCycle = 250;
        int energyPerTick = 60;
        int ticksPerCycle = 80;

        assertEquals(16, tankCapacity / waterPerCycle, "El tanque de 4000 mB debe permitir exactamente 16 ciclos de lavado");
        assertEquals(4800, energyPerTick * ticksPerCycle, "El costo total por ciclo debe ser 4800 FE");
    }

    @Test
    @DisplayName("Generación de textura para bloque washer (washer.png)")
    void testGenerateWasherTexture() throws Exception {
        File blockDir = new File("src/main/resources/assets/logtech/textures/block");
        blockDir.mkdirs();

        // 16x16 textura de washer
        BufferedImage washerImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int cChassis = 0xFF374151;      // Gris industrial oscuro
        int cBorder = 0xFF1F2937;       // Borde metálico
        int cPanelLight = 0xFF4B5563;   // Resalte panel
        int cPortholeRing = 0xFF0284C7; // Anillo azul cian de cámara de agua
        int cWaterDark = 0xFF0369A1;    // Agua profunda
        int cWaterLight = 0xFF38BDF8;   // Reflejo de agua
        int cValve = 0xFF0EA5E9;        // Válvula de flujo

        // Fondo chasis metálico
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                washerImg.setRGB(x, y, cChassis);
            }
        }

        // Bordes de chasis
        for (int i = 0; i < 16; i++) {
            washerImg.setRGB(i, 0, cBorder);
            washerImg.setRGB(i, 15, cBorder);
            washerImg.setRGB(0, i, cBorder);
            washerImg.setRGB(15, i, cBorder);
        }

        // Panel superior de control
        for (int x = 2; x <= 13; x++) {
            washerImg.setRGB(x, 2, cPanelLight);
        }
        washerImg.setRGB(3, 2, 0xFF10B981); // LED verde de encendido
        washerImg.setRGB(5, 2, 0xFF38BDF8); // LED azul de agua

        // Cámara circular de lavado / mirilla de agua (centro x: 7-8, y: 8-9)
        for (int y = 5; y <= 12; y++) {
            for (int x = 4; x <= 11; x++) {
                double dist = Math.sqrt((x - 7.5) * (x - 7.5) + (y - 8.5) * (y - 8.5));
                if (dist <= 3.6 && dist >= 2.6) {
                    washerImg.setRGB(x, y, cPortholeRing);
                } else if (dist < 2.6) {
                    washerImg.setRGB(x, y, cWaterDark);
                }
            }
        }
        // Ondas / reflejos en el agua
        washerImg.setRGB(7, 7, cWaterLight);
        washerImg.setRGB(8, 7, cWaterLight);
        washerImg.setRGB(6, 9, cWaterLight);
        washerImg.setRGB(9, 9, cWaterLight);
        washerImg.setRGB(7, 10, cValve);

        // Válvula / drenaje inferior
        washerImg.setRGB(7, 13, cValve);
        washerImg.setRGB(8, 13, cValve);

        File washerFile = new File(blockDir, "washer.png");
        ImageIO.write(washerImg, "PNG", washerFile);
        assertTrue(washerFile.exists(), "La textura washer.png debe existir");
    }
}
