package io.github.chema280101.logtech.storage;

/**
 * Modo de fijación o política de almacenamiento para un ítem en el sistema caliente/frío.
 */
public enum ItemPinMode {
    AUTO,
    FIJAR_CALIENTE,
    SOLO_FRIO;

    /**
     * Devuelve el siguiente modo en el ciclo: AUTO -> FIJAR_CALIENTE -> SOLO_FRIO -> AUTO.
     */
    public ItemPinMode next() {
        return switch (this) {
            case AUTO -> FIJAR_CALIENTE;
            case FIJAR_CALIENTE -> SOLO_FRIO;
            case SOLO_FRIO -> AUTO;
        };
    }
}
