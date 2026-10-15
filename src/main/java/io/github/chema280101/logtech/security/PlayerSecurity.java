package io.github.chema280101.logtech.security;

import org.jetbrains.annotations.Nullable;

/**
 * Punto de entrada alternativo/alias para verificación de permisos de jugadores.
 * Delega en {@link NetworkSecurity#canPlayerDo(Object, PlayerAction, Object)}.
 */
public final class PlayerSecurity {

    private PlayerSecurity() {
    }

    /**
     * Evalúa si un jugador tiene permiso para ejecutar una acción dada en un contexto opcional.
     *
     * @param player  El jugador solicitante.
     * @param action  La acción a verificar.
     * @param context El contexto de red o bloque.
     * @return true si la acción está permitida.
     */
    public static boolean canPlayerDo(@Nullable Object player, PlayerAction action, @Nullable Object context) {
        return NetworkSecurity.canPlayerDo(player, action, context);
    }

    /**
     * Evalúa si un jugador tiene permiso para ejecutar una acción dada.
     *
     * @param player El jugador solicitante.
     * @param action La acción a verificar.
     * @return true si la acción está permitida.
     */
    public static boolean canPlayerDo(@Nullable Object player, PlayerAction action) {
        return NetworkSecurity.canPlayerDo(player, action, null);
    }
}
