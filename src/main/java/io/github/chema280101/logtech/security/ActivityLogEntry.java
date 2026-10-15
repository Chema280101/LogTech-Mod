package io.github.chema280101.logtech.security;

import java.util.Objects;
import java.util.UUID;

/**
 * Entrada inmutable en el registro de actividad de la red.
 *
 * @param gameTime   Hora de juego (en ticks) en la que ocurrió (o más recientemente se agrupó) la acción.
 * @param playerUuid Identificador único del jugador que realizó la acción.
 * @param playerName Nombre del jugador en el momento de la acción.
 * @param action     Tipo de acción ejecutada.
 * @param item       Clave o identificador del ítem asociado (o valor representativo/vacío si no aplica).
 * @param amount     Cantidad de ítems o repeticiones de la acción.
 * @param <T>        Tipo del identificador de ítem.
 */
public record ActivityLogEntry<T>(
        long gameTime,
        UUID playerUuid,
        String playerName,
        ActivityAction action,
        T item,
        long amount
) {
    public ActivityLogEntry {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        Objects.requireNonNull(playerName, "playerName cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
    }

    public ActivityLogEntry<T> withAmountAndGameTime(long newAmount, long newGameTime) {
        return new ActivityLogEntry<>(newGameTime, this.playerUuid, this.playerName, this.action, this.item, newAmount);
    }
}
