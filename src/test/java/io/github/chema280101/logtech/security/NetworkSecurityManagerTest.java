package io.github.chema280101.logtech.security;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkSecurityManagerTest {

    private NetworkSecurityManager security;
    private UUID ownerId;
    private UUID userB;
    private UUID userC;

    @BeforeEach
    void setUp() {
        security = new NetworkSecurityManager();
        ownerId = UUID.randomUUID();
        userB = UUID.randomUUID();
        userC = UUID.randomUUID();
    }

    @Test
    @DisplayName("Núcleo antiguo o sin dueño permite reclamar y autoriza acciones de uso en modo abierto")
    void testUnownedCoreBehavior() {
        assertFalse(security.hasOwner());
        assertEquals(SecurityMode.OPEN, security.getMode());

        // Cualquiera puede reclamar un núcleo sin dueño
        assertTrue(security.canPlayerDo(userB, PlayerAction.CLAIM_CORE, false));

        // Acciones normales de uso están permitidas
        assertTrue(security.canPlayerDo(userB, PlayerAction.OPEN_TERMINAL, false));
        assertTrue(security.canPlayerDo(userB, PlayerAction.EXTRACT_ITEM, false));
        assertTrue(security.canPlayerDo(userB, PlayerAction.INSERT_ITEM, false));

        // Pero administrar accesos de una red sin dueño requiere primero reclamarla
        assertFalse(security.canPlayerDo(userB, PlayerAction.MANAGE_ACCESS, false));
    }

    @Test
    @DisplayName("Reclamar el núcleo asigna propietario y le concede todos los permisos")
    void testClaimingCore() {
        security.setOwner(ownerId, "OwnerPlayer");
        assertTrue(security.hasOwner());
        assertTrue(security.isOwner(ownerId));

        // El propietario tiene permiso absoluto para cualquier acción
        for (PlayerAction action : PlayerAction.values()) {
            assertTrue(security.canPlayerDo(ownerId, action, false), "El dueño debe poder ejecutar " + action);
        }

        // Otro jugador no puede volver a reclamarlo a menos que sea operador
        assertFalse(security.canPlayerDo(userB, PlayerAction.CLAIM_CORE, false));
        assertTrue(security.canPlayerDo(userB, PlayerAction.CLAIM_CORE, true));
    }

    @Test
    @DisplayName("En modo Abierta con dueño, usuarios sin permiso admin pueden ver/usar pero no administrar")
    void testOpenModePermissions() {
        security.setOwner(ownerId, "OwnerPlayer");
        security.setMode(SecurityMode.OPEN);

        // Usuario B sin rol en la lista
        assertTrue(security.canPlayerDo(userB, PlayerAction.OPEN_TERMINAL, false));
        assertTrue(security.canPlayerDo(userB, PlayerAction.EXTRACT_ITEM, false));
        assertTrue(security.canPlayerDo(userB, PlayerAction.INSERT_ITEM, false));
        assertFalse(security.canPlayerDo(userB, PlayerAction.CONFIGURE_RULES, false));
        assertFalse(security.canPlayerDo(userB, PlayerAction.MANAGE_ACCESS, false));

        // Usuario C con rol de Administrador
        security.addOrUpdatePlayer(userC, "PlayerC", true, true, true, true);
        assertTrue(security.canPlayerDo(userC, PlayerAction.CONFIGURE_RULES, false));
        assertTrue(security.canPlayerDo(userC, PlayerAction.MANAGE_ACCESS, false));
    }

    @Test
    @DisplayName("En modo Privada, usuario no registrado no puede realizar ninguna acción")
    void testPrivateModeUnregisteredUserBlocked() {
        security.setOwner(ownerId, "OwnerPlayer");
        security.setMode(SecurityMode.PRIVATE);

        for (PlayerAction action : PlayerAction.values()) {
            assertFalse(security.canPlayerDo(userB, action, false), "Usuario no registrado no debe poder " + action);
        }

        // Operador siempre puede incluso en modo privado
        for (PlayerAction action : PlayerAction.values()) {
            assertTrue(security.canPlayerDo(userB, action, true), "Operador debe poder ejecutar " + action);
        }
    }

    @Test
    @DisplayName("En modo Privada, se respetan los permisos individuales de Ver, Extraer, Insertar y Administrar")
    void testPrivateModeIndividualPermissions() {
        security.setOwner(ownerId, "OwnerPlayer");
        security.setMode(SecurityMode.PRIVATE);

        // B solo tiene permiso de VER
        security.addOrUpdatePlayer(userB, "PlayerB", true, false, false, false);
        assertTrue(security.canPlayerDo(userB, PlayerAction.OPEN_TERMINAL, false));
        assertTrue(security.canPlayerDo(userB, PlayerAction.OPEN_CORE, false));
        assertFalse(security.canPlayerDo(userB, PlayerAction.EXTRACT_ITEM, false));
        assertFalse(security.canPlayerDo(userB, PlayerAction.INSERT_ITEM, false));
        assertFalse(security.canPlayerDo(userB, PlayerAction.MANAGE_ACCESS, false));

        // B obtiene permiso de EXTRAER
        security.addOrUpdatePlayer(userB, "PlayerB", true, true, false, false);
        assertTrue(security.canPlayerDo(userB, PlayerAction.EXTRACT_ITEM, false));
        assertFalse(security.canPlayerDo(userB, PlayerAction.INSERT_ITEM, false));

        // B obtiene permiso de INSERTAR
        security.addOrUpdatePlayer(userB, "PlayerB", true, true, true, false);
        assertTrue(security.canPlayerDo(userB, PlayerAction.INSERT_ITEM, false));
        assertFalse(security.canPlayerDo(userB, PlayerAction.MANAGE_ACCESS, false));
        assertFalse(security.canPlayerDo(userB, PlayerAction.BREAK_CORE, false));

        // B obtiene rol de ADMINISTRADOR
        security.addOrUpdatePlayer(userB, "PlayerB", true, true, true, true);
        assertTrue(security.canPlayerDo(userB, PlayerAction.MANAGE_ACCESS, false));
        assertTrue(security.canPlayerDo(userB, PlayerAction.CONFIGURE_RULES, false));
        assertTrue(security.canPlayerDo(userB, PlayerAction.BREAK_CORE, false));
    }

    @Test
    @DisplayName("Gestión y conmutación interactiva de permisos de jugadores")
    void testPlayerEntryManagementAndToggles() {
        security.setOwner(ownerId, "OwnerAlex");
        security.setMode(SecurityMode.PRIVATE);
        security.addOrUpdatePlayer(userB, "UserB", true, false, true, false);

        PlayerAccessEntry entryB = security.getEntry(userB);
        assertNotNull(entryB);
        assertEquals("UserB", entryB.getLastKnownName());
        assertTrue(entryB.canView());
        assertFalse(entryB.canExtract());
        assertTrue(entryB.canInsert());
        assertFalse(entryB.canAdmin());

        // Conmutación individual
        entryB.togglePermission(SecurityPermission.EXTRACT);
        assertTrue(entryB.canExtract());
        entryB.togglePermission(SecurityPermission.VIEW);
        assertFalse(entryB.canView());

        // Eliminación de jugador
        assertTrue(security.removePlayer(userB));
        assertNull(security.getEntry(userB));
        assertFalse(security.canPlayerDo(userB, PlayerAction.OPEN_TERMINAL, false));
    }
}
