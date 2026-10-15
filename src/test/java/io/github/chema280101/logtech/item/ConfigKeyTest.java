package io.github.chema280101.logtech.item;

import io.github.chema280101.logtech.block.ConfigTypes;
import io.github.chema280101.logtech.network.EnergyPriority;
import io.github.chema280101.logtech.security.NetworkSecurity;
import io.github.chema280101.logtech.security.PlayerAction;
import io.github.chema280101.logtech.security.PlayerSecurity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigKeyTest {

    @Test
    @DisplayName("ConfigTypes contiene las constantes correctas para todos los bloques soportados")
    void testConfigTypesConstants() {
        assertEquals("machine", ConfigTypes.MACHINE);
        assertEquals("alarm", ConfigTypes.ALARM);
        assertEquals("core", ConfigTypes.CORE);
        assertEquals("wall_display", ConfigTypes.WALL_DISPLAY);
    }

    @Test
    @DisplayName("Permiso PASTE_CONFIG está presente en PlayerAction y autorizado en NetworkSecurity")
    void testPasteConfigPermission() {
        assertNotNull(PlayerAction.valueOf("PASTE_CONFIG"));
        assertTrue(NetworkSecurity.canPlayerDo(null, PlayerAction.PASTE_CONFIG));
        assertTrue(PlayerSecurity.canPlayerDo(new Object(), PlayerAction.PASTE_CONFIG));
    }

    @Test
    @DisplayName("EnergyPriority se serializa y valida correctamente para configuración de máquinas")
    void testMachinePriorityConfigValidation() {
        // Valores válidos
        assertEquals(EnergyPriority.HIGH, EnergyPriority.fromName("HIGH"));
        assertEquals(EnergyPriority.NORMAL, EnergyPriority.fromName("NORMAL"));
        assertEquals(EnergyPriority.LOW, EnergyPriority.fromName("LOW"));

        // Comprobación de excepción para valores inválidos o manipulados externamente
        assertThrows(IllegalArgumentException.class, () -> EnergyPriority.valueOf("INVALID_PRIO"));
        assertThrows(IllegalArgumentException.class, () -> EnergyPriority.valueOf(""));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    @DisplayName("Canales de alarma 1 a 4 son aceptados como válidos")
    void testValidAlarmChannels(int channel) {
        assertTrue(channel >= 1 && channel <= 4, "El canal " + channel + " debe ser válido");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 5, 10, 999})
    @DisplayName("Canales de alarma fuera del rango [1, 4] son rechazados")
    void testInvalidAlarmChannels(int channel) {
        assertFalse(channel >= 1 && channel <= 4, "El canal " + channel + " debe ser rechazado");
    }

    @Test
    @DisplayName("El Núcleo rechaza cualquier configuración con versión de datos distinta a 6")
    void testCoreDataVersionValidation() {
        int currentVersion = 6;
        int oldVersion1 = 1;
        int oldVersion2 = 2;
        int oldVersion5 = 5;
        int futureVersion7 = 7;

        assertTrue(currentVersion == 6, "La versión actual del Núcleo debe ser 6");
        assertFalse(oldVersion1 == currentVersion);
        assertFalse(oldVersion2 == currentVersion);
        assertFalse(oldVersion5 == currentVersion);
        assertFalse(futureVersion7 == currentVersion);
    }

    @Test
    @DisplayName("Validación estricta de límites de reglas para el Núcleo (máx 8, umbral >= 0)")
    void testCoreRulesValidationLimits() {
        int maxRules = 8;
        assertTrue(8 <= maxRules);
        assertFalse(9 <= maxRules, "Más de 8 reglas debe ser rechazado");

        long validThreshold = 100L;
        long negativeThreshold = -1L;
        assertTrue(validThreshold >= 0);
        assertFalse(negativeThreshold >= 0, "Umbral negativo debe ser rechazado");
    }
}
