package io.github.chema280101.logtech.security;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Gestor centralizado de permisos y autorización para acciones realizadas por jugadores en LogTech.
 * Desacoplado como clase Java pura para permitir pruebas unitarias y máxima interoperabilidad.
 * En la fase de preparación T43, toda acción es autorizada de forma incondicional (respuesta fija de "sí").
 */
public final class NetworkSecurity {

    private NetworkSecurity() {
    }

    /**
     * Verifica si un jugador está autorizado para ejecutar la acción especificada.
     *
     * @param player El jugador (o referencia de jugador) que intenta ejecutar la acción (puede ser {@code null}).
     * @param action La acción solicitada (no debe ser nula).
     * @return {@code true} si la acción está autorizada, {@code false} en caso contrario.
     */
    public static boolean canPlayerDo(@Nullable Object player, PlayerAction action) {
        Objects.requireNonNull(action, "action cannot be null");
        // Respuesta fija afirmativa ("sí") para la fase de preparación T43
        return true;
    }
}
