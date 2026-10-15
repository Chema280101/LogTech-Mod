package io.github.chema280101.logtech.storage;

import java.util.Objects;

/**
 * Representa una entrada de ítem en la papelera de reciclaje del Núcleo.
 * Clase Java pura desacoplada de Minecraft.
 *
 * @param item clave del ítem
 * @param amount cantidad de ítems guardados
 * @param gameTime tick del juego (gameTime) en el que se destruyó o actualizó la entrada
 * @param <K> tipo de clave del ítem
 */
public record TrashEntry<K>(K item, long amount, long gameTime) {

    public TrashEntry {
        Objects.requireNonNull(item, "El ítem no puede ser nulo");
        if (amount < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa: " + amount);
        }
    }

    public TrashEntry<K> withAmount(long newAmount) {
        return new TrashEntry<>(item, newAmount, gameTime);
    }

    public TrashEntry<K> withAmountAndGameTime(long newAmount, long newGameTime) {
        return new TrashEntry<>(item, newAmount, newGameTime);
    }
}
