package io.github.chema280101.logtech.storage;

import java.util.Objects;

/**
 * Representa un par reversible de compactación (ítem base ↔ ítem compacto) y su ratio de conversión.
 * Clase Java pura desacoplada de Minecraft.
 *
 * @param <K> tipo de clave del ítem
 */
public record CompactionPair<K>(K baseItem, K compactItem, int ratio) {
    public CompactionPair {
        Objects.requireNonNull(baseItem, "baseItem no puede ser nulo");
        Objects.requireNonNull(compactItem, "compactItem no puede ser nulo");
        if (baseItem.equals(compactItem)) {
            throw new IllegalArgumentException("baseItem y compactItem deben ser distintos");
        }
        if (ratio != 4 && ratio != 9) {
            throw new IllegalArgumentException("El ratio debe ser 4 o 9, recibido: " + ratio);
        }
    }
}
