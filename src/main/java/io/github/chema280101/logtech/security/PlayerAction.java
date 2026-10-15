package io.github.chema280101.logtech.security;

/**
 * Representa las diferentes acciones que un jugador puede solicitar ejecutar
 * dentro del ecosistema de redes, almacenamiento, máquinas y configuración de LogTech.
 */
public enum PlayerAction {
    OPEN_TERMINAL,
    EXTRACT_ITEM,
    INSERT_ITEM,
    PIN_TIER,
    OPEN_CORE,
    CONFIGURE_RULES,
    CHANGE_PRIORITY,
    CONFIGURE_ALARM,
    MODIFY_MODULES,
    FLASH_FIRMWARE,
    INTERACT_MACHINE,
    TRASH_ITEM,
    RESTORE_ITEM,
    MANAGE_VIEWS,
    PASTE_CONFIG,
    CLAIM_CORE,
    MANAGE_ACCESS,
    BREAK_CORE,
    VIEW_LOG
}
