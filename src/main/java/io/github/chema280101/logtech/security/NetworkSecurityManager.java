package io.github.chema280101.logtech.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Gestor del control de accesos, propietario y permisos por jugador para una red LogTech.
 * Desacoplado como clase Java pura para pruebas unitarias aisladas sin dependencias de Minecraft.
 */
public class NetworkSecurityManager {
    private @Nullable UUID ownerUuid;
    private String ownerName = "";
    private SecurityMode mode = SecurityMode.OPEN;
    private final Map<UUID, PlayerAccessEntry> entries = new LinkedHashMap<>();

    public NetworkSecurityManager() {
    }

    public @Nullable UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwner(@Nullable UUID ownerUuid, String ownerName) {
        this.ownerUuid = ownerUuid;
        this.ownerName = (ownerName != null) ? ownerName : "";
    }

    public void setOwner(@Nullable UUID ownerUuid) {
        setOwner(ownerUuid, ownerUuid != null ? ownerUuid.toString().substring(0, 8) : "");
    }

    public boolean hasOwner() {
        return ownerUuid != null;
    }

    public boolean isOwner(@Nullable UUID playerUuid) {
        return playerUuid != null && ownerUuid != null && ownerUuid.equals(playerUuid);
    }

    public SecurityMode getMode() {
        return mode;
    }

    public void setMode(SecurityMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode cannot be null");
    }

    public Map<UUID, PlayerAccessEntry> getEntries() {
        return Collections.unmodifiableMap(entries);
    }

    public List<PlayerAccessEntry> getEntryList() {
        return new ArrayList<>(entries.values());
    }

    public @Nullable PlayerAccessEntry getEntry(UUID playerUuid) {
        return entries.get(playerUuid);
    }

    public void addOrUpdatePlayer(UUID playerUuid, String playerName, boolean view, boolean extract, boolean insert, boolean admin) {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        PlayerAccessEntry entry = entries.get(playerUuid);
        if (entry != null) {
            entry.setLastKnownName(playerName);
            entry.setCanView(view);
            entry.setCanExtract(extract);
            entry.setCanInsert(insert);
            entry.setCanAdmin(admin);
        } else {
            entries.put(playerUuid, new PlayerAccessEntry(playerUuid, playerName, view, extract, insert, admin));
        }
    }

    public boolean removePlayer(UUID playerUuid) {
        return entries.remove(playerUuid) != null;
    }

    public void clearEntries() {
        entries.clear();
    }

    /**
     * Evalúa si un jugador tiene autorización para ejecutar una acción dada.
     *
     * @param playerUuid Identificador del jugador (puede ser null para acciones anónimas o de prueba).
     * @param action     Acción requerida.
     * @param isOperator Si el jugador posee privilegios de operador de servidor (nivel 2+).
     * @return true si la acción está autorizada.
     */
    public boolean canPlayerDo(@Nullable UUID playerUuid, PlayerAction action, boolean isOperator) {
        Objects.requireNonNull(action, "action cannot be null");

        // Operadores de servidor (nivel 2+) siempre tienen autorización incondicional
        if (isOperator) {
            return true;
        }

        // Reclamar núcleo
        if (action == PlayerAction.CLAIM_CORE) {
            if (ownerUuid == null) {
                return playerUuid != null;
            }
            return isOwner(playerUuid);
        }

        // El propietario de la red siempre tiene autorización absoluta
        if (isOwner(playerUuid)) {
            return true;
        }

        // En modo PRIVADA (PRIVATE):
        if (mode == SecurityMode.PRIVATE) {
            if (playerUuid == null) {
                return false;
            }

            PlayerAccessEntry entry = entries.get(playerUuid);
            if (entry == null) {
                return false;
            }

            if (entry.canAdmin()) {
                return true;
            }

            return switch (action) {
                case OPEN_TERMINAL, OPEN_CORE, INTERACT_MACHINE -> entry.canView();
                case INSERT_ITEM -> entry.canInsert();
                case EXTRACT_ITEM, PIN_TIER, TRASH_ITEM, RESTORE_ITEM -> entry.canExtract();
                case CONFIGURE_RULES, MANAGE_VIEWS, PASTE_CONFIG,
                     CHANGE_PRIORITY, CONFIGURE_ALARM, MODIFY_MODULES,
                     FLASH_FIRMWARE, MANAGE_ACCESS, BREAK_CORE, CLAIM_CORE,
                     VIEW_LOG -> false;
            };
        }

        // Si el núcleo no tiene dueño (huérfano / de v0.1-v0.2 sin reclamar):
        if (ownerUuid == null) {
            return action != PlayerAction.MANAGE_ACCESS && action != PlayerAction.VIEW_LOG;
        }

        // En modo ABIERTA (OPEN):
        return switch (action) {
            case OPEN_TERMINAL, OPEN_CORE, INTERACT_MACHINE,
                 INSERT_ITEM, EXTRACT_ITEM, PIN_TIER,
                 TRASH_ITEM, RESTORE_ITEM -> true;
            case BREAK_CORE -> true;
            case CONFIGURE_RULES, MANAGE_VIEWS, PASTE_CONFIG,
                 CHANGE_PRIORITY, CONFIGURE_ALARM, MODIFY_MODULES,
                 FLASH_FIRMWARE, MANAGE_ACCESS, VIEW_LOG -> {
                PlayerAccessEntry entry = (playerUuid != null) ? entries.get(playerUuid) : null;
                yield entry != null && entry.canAdmin();
            }
            case CLAIM_CORE -> false;
        };
    }
}
