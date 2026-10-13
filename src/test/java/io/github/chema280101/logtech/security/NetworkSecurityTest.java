package io.github.chema280101.logtech.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class NetworkSecurityTest {

    @ParameterizedTest
    @EnumSource(PlayerAction.class)
    @DisplayName("canPlayerDo responde 'sí' (true) para todas las acciones con jugador nulo")
    void testCanPlayerDoReturnsTrueForNullPlayer(PlayerAction action) {
        assertTrue(NetworkSecurity.canPlayerDo(null, action), "Debe autorizar la acción " + action);
        assertTrue(PlayerSecurity.canPlayerDo(null, action), "Alias PlayerSecurity debe autorizar la acción " + action);
    }

    @ParameterizedTest
    @EnumSource(PlayerAction.class)
    @DisplayName("canPlayerDo responde 'sí' (true) para todas las acciones con objeto jugador presente")
    void testCanPlayerDoReturnsTrueForNonNullPlayer(PlayerAction action) {
        Object mockPlayer = new Object();
        assertTrue(NetworkSecurity.canPlayerDo(mockPlayer, action), "Debe autorizar la acción " + action);
        assertTrue(PlayerSecurity.canPlayerDo(mockPlayer, action), "Alias PlayerSecurity debe autorizar la acción " + action);
    }

    @Test
    @DisplayName("canPlayerDo lanza NullPointerException si la acción es nula")
    void testCanPlayerDoRejectsNullAction() {
        assertThrows(NullPointerException.class, () -> NetworkSecurity.canPlayerDo(null, null));
        assertThrows(NullPointerException.class, () -> PlayerSecurity.canPlayerDo(null, null));
    }

    @Test
    @DisplayName("Verifica que el enum PlayerAction contenga las acciones esperadas de la red")
    void testPlayerActionEnumContainsExpectedValues() {
        PlayerAction[] actions = PlayerAction.values();
        assertTrue(actions.length >= 8, "Debe contener al menos 8 acciones de jugador");
        assertNotNull(PlayerAction.valueOf("OPEN_TERMINAL"));
        assertNotNull(PlayerAction.valueOf("EXTRACT_ITEM"));
        assertNotNull(PlayerAction.valueOf("INSERT_ITEM"));
        assertNotNull(PlayerAction.valueOf("PIN_TIER"));
        assertNotNull(PlayerAction.valueOf("OPEN_CORE"));
        assertNotNull(PlayerAction.valueOf("CONFIGURE_RULES"));
        assertNotNull(PlayerAction.valueOf("CHANGE_PRIORITY"));
        assertNotNull(PlayerAction.valueOf("FLASH_FIRMWARE"));
    }
}
