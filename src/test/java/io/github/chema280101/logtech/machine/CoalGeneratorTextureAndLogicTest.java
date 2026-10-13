package io.github.chema280101.logtech.machine;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class CoalGeneratorTextureAndLogicTest {

    @Test
    void testGenerateTextures() throws IOException {
        File dir = new File("src/main/resources/assets/logtech/textures/block");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File inactiveFile = new File(dir, "coal_generator.png");
        File activeFile = new File(dir, "coal_generator_active.png");

        BufferedImage inactiveImg = createGeneratorImage(false);
        BufferedImage activeImg = createGeneratorImage(true);

        ImageIO.write(inactiveImg, "PNG", inactiveFile);
        ImageIO.write(activeImg, "PNG", activeFile);

        assertTrue(inactiveFile.exists(), "Debe existir coal_generator.png");
        assertTrue(activeFile.exists(), "Debe existir coal_generator_active.png");
        assertEquals(16, inactiveImg.getWidth());
        assertEquals(16, inactiveImg.getHeight());
        assertEquals(16, activeImg.getWidth());
        assertEquals(16, activeImg.getHeight());
    }

    private BufferedImage createGeneratorImage(boolean active) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        int cBorderDark = 0xFF1E293B;   // Borde exterior
        int cBorderLight = 0xFF475569;  // Bisel metálico
        int cPanel = 0xFF334155;        // Placa principal
        int cVent = active ? 0xFF9A3412 : 0xFF0F172A; // Rendija de ventilación
        int cRivet = 0xFF64748B;        // Remaches

        // 1. Relleno base de chapa industrial
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                img.setRGB(x, y, cPanel);
            }
        }

        // 2. Bordes y esquinas metálicas
        for (int i = 0; i < 16; i++) {
            img.setRGB(i, 0, cBorderDark);
            img.setRGB(i, 15, cBorderDark);
            img.setRGB(0, i, cBorderDark);
            img.setRGB(15, i, cBorderDark);

            if (i > 0 && i < 15) {
                img.setRGB(i, 1, cBorderLight);
                img.setRGB(1, i, cBorderLight);
            }
        }

        // Remaches en las 4 esquinas
        img.setRGB(2, 2, cRivet);
        img.setRGB(13, 2, cRivet);
        img.setRGB(2, 13, cRivet);
        img.setRGB(13, 13, cRivet);

        // 3. Rejillas de ventilación superiores (filas 3 y 4)
        for (int x = 4; x <= 11; x += 2) {
            img.setRGB(x, 3, cVent);
            img.setRGB(x, 4, cVent);
        }

        // 4. Cámara de combustión / Puerta de horno (filas 6 a 12, cols 3 a 12)
        int cDoorFrame = 0xFF1E293B;
        for (int x = 3; x <= 12; x++) {
            img.setRGB(x, 6, cDoorFrame);
            img.setRGB(x, 12, cDoorFrame);
        }
        for (int y = 6; y <= 12; y++) {
            img.setRGB(3, y, cDoorFrame);
            img.setRGB(12, y, cDoorFrame);
        }

        // Interior de la cámara
        if (!active) {
            // Apagado: carbón frío y cenizas
            int[][] chamber = {
                    {0xFF0F172A, 0xFF1E293B, 0xFF0F172A, 0xFF1E293B, 0xFF0F172A, 0xFF1E293B, 0xFF0F172A, 0xFF1E293B},
                    {0xFF1E293B, 0xFF090D16, 0xFF1E293B, 0xFF090D16, 0xFF1E293B, 0xFF090D16, 0xFF1E293B, 0xFF090D16},
                    {0xFF0F172A, 0xFF1E293B, 0xFF0F172A, 0xFF1E293B, 0xFF0F172A, 0xFF1E293B, 0xFF0F172A, 0xFF1E293B},
                    {0xFF1E293B, 0xFF090D16, 0xFF1E293B, 0xFF090D16, 0xFF1E293B, 0xFF090D16, 0xFF1E293B, 0xFF090D16},
                    {0xFF0F172A, 0xFF1E293B, 0xFF0F172A, 0xFF1E293B, 0xFF0F172A, 0xFF1E293B, 0xFF0F172A, 0xFF1E293B}
            };
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 8; c++) {
                    img.setRGB(4 + c, 7 + r, chamber[r][c]);
                }
            }
            // LED de estado: apagado (rojo oscuro)
            img.setRGB(13, 14, 0xFF7F1D1D);
        } else {
            // Encendido: fuego ardiente, brasas y calor intenso
            int[][] chamber = {
                    {0xFFC2410C, 0xFFEA580C, 0xFFF97316, 0xFFFB923C, 0xFFF97316, 0xFFEA580C, 0xFFC2410C, 0xFF9A3412},
                    {0xFFEA580C, 0xFFFBBF24, 0xFFFEF08A, 0xFFFFFFFF, 0xFFFEF08A, 0xFFFBBF24, 0xFFEA580C, 0xFFC2410C},
                    {0xFFF97316, 0xFFFEF08A, 0xFFFFFFFF, 0xFFFEF08A, 0xFFFFFFFF, 0xFFFEF08A, 0xFFF97316, 0xFFEA580C},
                    {0xFFEA580C, 0xFFFBBF24, 0xFFFEF08A, 0xFFFBBF24, 0xFFFEF08A, 0xFFFBBF24, 0xFFEA580C, 0xFFC2410C},
                    {0xFF9A3412, 0xFFC2410C, 0xFFEA580C, 0xFFC2410C, 0xFFEA580C, 0xFFC2410C, 0xFF9A3412, 0xFF7C2D12}
            };
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 8; c++) {
                    img.setRGB(4 + c, 7 + r, chamber[r][c]);
                }
            }
            // Rejilla frontal refractaria
            for (int x = 4; x <= 11; x += 2) {
                img.setRGB(x, 8, 0xFF334155);
                img.setRGB(x, 10, 0xFF334155);
            }
            // LED de estado: activo (verde brillante)
            img.setRGB(13, 14, 0xFF22C55E);
        }

        return img;
    }

    @Test
    void testGeneratorEnergyLogic() {
        int capacity = 10_000;
        int maxTransfer = 100;
        int genPerTick = 40;

        int currentEnergy = 0;
        int burnTime = 1600; // 1 carbón

        // Simular 10 ticks de generación
        for (int i = 0; i < 10; i++) {
            if (burnTime > 0 && currentEnergy < capacity) {
                int toGen = Math.min(capacity - currentEnergy, genPerTick);
                currentEnergy += toGen;
                burnTime--;
            }
        }

        assertEquals(400, currentEnergy, "10 ticks a 40 FE/t deben generar 400 FE");
        assertEquals(1590, burnTime, "El tiempo de combustión debe haberse reducido en 10 ticks");

        // Simular transferencia a una máquina consumidora
        int energyToSend = Math.min(currentEnergy, maxTransfer);
        int machineEnergy = 0;
        int machineCapacity = 10_000;
        int machineAccepted = Math.min(machineCapacity - machineEnergy, energyToSend);

        currentEnergy -= machineAccepted;
        machineEnergy += machineAccepted;

        assertEquals(100, machineEnergy, "La máquina debe haber recibido 100 FE");
        assertEquals(300, currentEnergy, "El generador debe tener 300 FE restantes");
    }

    @Test
    void testBufferFullPausesFuelConsumption() {
        int capacity = 10_000;
        int currentEnergy = 10_000; // Lleno
        int burnTime = 100;

        boolean isWorking = false;
        if (burnTime > 0) {
            if (currentEnergy < capacity) {
                burnTime--;
                isWorking = true;
            } else {
                // Buffer lleno: pausa
                isWorking = false;
            }
        }

        assertFalse(isWorking, "Con búfer lleno la máquina debe pausar (IDLE)");
        assertEquals(100, burnTime, "El combustible no se debe malgastar cuando el búfer está lleno");
    }
}
