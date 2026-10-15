package io.github.chema280101.logtech.security;

/**
 * Representa el modo de acceso a la red de LogTech.
 */
public enum SecurityMode {
    /**
     * Cualquier jugador puede ver, extraer e insertar ítems libremente.
     */
    OPEN,

    /**
     * Solo los jugadores registrados en la lista de permisos pueden interactuar según sus roles.
     */
    PRIVATE
}
