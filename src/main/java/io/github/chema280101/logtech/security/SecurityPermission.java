package io.github.chema280101.logtech.security;

/**
 * Permisos individuales asignables por jugador en una red de LogTech.
 */
public enum SecurityPermission {
    /**
     * Permiso para abrir el terminal y visualizar el inventario de la red y estado del núcleo.
     */
    VIEW,

    /**
     * Permiso para extraer ítems del almacenamiento, papelera o alterar fijado de niveles.
     */
    EXTRACT,

    /**
     * Permiso para insertar ítems en el almacenamiento de la red.
     */
    INSERT,

    /**
     * Permiso para configurar reglas, vistas, accesos, ajustes del núcleo y romper el bloque en modo privado.
     */
    ADMIN
}
