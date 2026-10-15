package io.github.chema280101.logtech.security;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Gestor centralizado de permisos y autorización para acciones realizadas por jugadores en LogTech.
 * Conecta el contexto del jugador con el gestor de seguridad de la red {@link NetworkSecurityManager}.
 * Desacoplado para compatibilidad plena con pruebas unitarias aisladas.
 */
public final class NetworkSecurity {

    private NetworkSecurity() {
    }

    /**
     * Verifica si un jugador está autorizado para ejecutar la acción especificada con un contexto opcional.
     *
     * @param player  El jugador (o referencia de jugador) que intenta ejecutar la acción.
     * @param action  La acción solicitada (no debe ser nula).
     * @param context El contexto de red o bloque (por ejemplo CoreBlockEntity o NetworkSecurityManager).
     * @return {@code true} si la acción está autorizada, {@code false} en caso contrario.
     */
    public static boolean canPlayerDo(@Nullable Object player, PlayerAction action, @Nullable Object context) {
        Objects.requireNonNull(action, "action cannot be null");

        UUID playerUuid = extractPlayerUuid(player);
        boolean isOperator = extractIsOperator(player);

        NetworkSecurityManager secManager = extractSecurityManager(context);
        if (secManager != null) {
            return secManager.canPlayerDo(playerUuid, action, isOperator);
        }

        if (isOperator) {
            return true;
        }

        return true;
    }

    /**
     * Verifica si un jugador está autorizado para ejecutar la acción especificada.
     *
     * @param player El jugador (o referencia de jugador) que intenta ejecutar la acción.
     * @param action La acción solicitada (no debe ser nula).
     * @return {@code true} si la acción está autorizada, {@code false} en caso contrario.
     */
    public static boolean canPlayerDo(@Nullable Object player, PlayerAction action) {
        return canPlayerDo(player, action, null);
    }

    private static @Nullable NetworkSecurityManager extractSecurityManager(@Nullable Object context) {
        if (context == null) return null;
        if (context instanceof NetworkSecurityManager nsm) return nsm;
        try {
            Method m = context.getClass().getMethod("getSecurityManager");
            Object res = m.invoke(context);
            if (res instanceof NetworkSecurityManager nsm) {
                return nsm;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static @Nullable UUID extractPlayerUuid(@Nullable Object player) {
        if (player == null) return null;
        if (player instanceof UUID uuid) return uuid;
        try {
            Method m = player.getClass().getMethod("getUUID");
            Object res = m.invoke(player);
            if (res instanceof UUID uuid) {
                return uuid;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean extractIsOperator(@Nullable Object player) {
        if (player == null) return false;
        try {
            Method m = player.getClass().getMethod("hasPermissions", int.class);
            Object res = m.invoke(player, 2);
            if (res instanceof Boolean b) {
                return b;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
