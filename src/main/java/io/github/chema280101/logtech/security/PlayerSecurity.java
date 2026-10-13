package io.github.chema280101.logtech.security;

import org.jetbrains.annotations.Nullable;

/**
 * Punto de entrada alternativo/alias para verificación de permisos de jugadores.
 * Delega en {@link NetworkSecurity#canPlayerDo(Object, PlayerAction)}.
 */
public final class PlayerSecurity {

    private PlayerSecurity() {
    }

    /**
     * Evalúa si un jugador tiene permiso para ejecutar una acción dada.
     *
     * @param player El jugador solicitante.
     * @param action La acción a verificar.
     * @return true si la acción está permitida.
     */
    public static boolean canPlayerDo(@Nullable Object player, PlayerAction action) {
        return NetworkSecurity.canPlayerDo(player, action);
    }
}
