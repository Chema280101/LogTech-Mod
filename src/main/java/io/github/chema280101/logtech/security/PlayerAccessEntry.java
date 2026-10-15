package io.github.chema280101.logtech.security;

import java.util.Objects;
import java.util.UUID;

/**
 * Registro de permisos de un jugador específico en una red de LogTech.
 * Clase Java pura desacoplada sin dependencias de Minecraft.
 */
public class PlayerAccessEntry {
    private final UUID playerUuid;
    private String lastKnownName;
    private boolean canView;
    private boolean canExtract;
    private boolean canInsert;
    private boolean canAdmin;

    public PlayerAccessEntry(UUID playerUuid, String lastKnownName, boolean canView, boolean canExtract, boolean canInsert, boolean canAdmin) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        this.lastKnownName = (lastKnownName != null && !lastKnownName.isBlank()) ? lastKnownName : playerUuid.toString().substring(0, 8);
        this.canView = canView;
        this.canExtract = canExtract;
        this.canInsert = canInsert;
        this.canAdmin = canAdmin;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        if (lastKnownName != null && !lastKnownName.isBlank()) {
            this.lastKnownName = lastKnownName;
        }
    }

    public boolean canView() {
        return canView;
    }

    public void setCanView(boolean canView) {
        this.canView = canView;
    }

    public boolean canExtract() {
        return canExtract;
    }

    public void setCanExtract(boolean canExtract) {
        this.canExtract = canExtract;
    }

    public boolean canInsert() {
        return canInsert;
    }

    public void setCanInsert(boolean canInsert) {
        this.canInsert = canInsert;
    }

    public boolean canAdmin() {
        return canAdmin;
    }

    public void setCanAdmin(boolean canAdmin) {
        this.canAdmin = canAdmin;
    }

    public boolean hasPermission(SecurityPermission permission) {
        if (permission == null) return false;
        return switch (permission) {
            case VIEW -> canView;
            case EXTRACT -> canExtract;
            case INSERT -> canInsert;
            case ADMIN -> canAdmin;
        };
    }

    public void setPermission(SecurityPermission permission, boolean granted) {
        if (permission == null) return;
        switch (permission) {
            case VIEW -> this.canView = granted;
            case EXTRACT -> this.canExtract = granted;
            case INSERT -> this.canInsert = granted;
            case ADMIN -> this.canAdmin = granted;
        }
    }

    public void togglePermission(SecurityPermission permission) {
        if (permission == null) return;
        setPermission(permission, !hasPermission(permission));
    }
}
