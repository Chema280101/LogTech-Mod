package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.CoreBlockEntity;
import io.github.chema280101.logtech.block.CoreMenu;
import io.github.chema280101.logtech.security.NetworkSecurity;
import io.github.chema280101.logtech.security.NetworkSecurityManager;
import io.github.chema280101.logtech.security.PlayerAccessEntry;
import io.github.chema280101.logtech.security.PlayerAction;
import io.github.chema280101.logtech.security.SecurityMode;
import io.github.chema280101.logtech.security.SecurityPermission;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Paquete C2S para modificar ajustes de seguridad y permisos de red en el Núcleo.
 */
public record CoreAccessUpdatePayload(
        int containerId,
        ActionType actionType,
        int modeOrdinal,
        UUID targetUuid,
        String targetName,
        int permissionOrdinal
) implements CustomPacketPayload {

    public enum ActionType {
        SET_MODE,
        TOGGLE_PERMISSION,
        ADD_PLAYER,
        REMOVE_PLAYER
    }

    public static final Type<CoreAccessUpdatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "core_access_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoreAccessUpdatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                buf.writeVarInt(payload.actionType().ordinal());
                buf.writeVarInt(payload.modeOrdinal());
                buf.writeBoolean(payload.targetUuid() != null);
                if (payload.targetUuid() != null) {
                    buf.writeUUID(payload.targetUuid());
                }
                buf.writeUtf(payload.targetName() != null ? payload.targetName() : "");
                buf.writeVarInt(payload.permissionOrdinal());
            },
            buf -> {
                int containerId = buf.readVarInt();
                ActionType action = ActionType.values()[buf.readVarInt()];
                int modeOrdinal = buf.readVarInt();
                UUID targetUuid = buf.readBoolean() ? buf.readUUID() : null;
                String targetName = buf.readUtf();
                int permissionOrdinal = buf.readVarInt();
                return new CoreAccessUpdatePayload(containerId, action, modeOrdinal, targetUuid, targetName, permissionOrdinal);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CoreAccessUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player instanceof ServerPlayer sp && sp.containerMenu instanceof CoreMenu menu && menu.containerId == payload.containerId()) {
                CoreBlockEntity coreBe = menu.getCoreBlockEntity(sp);
                if (coreBe != null && NetworkSecurity.canPlayerDo(sp, PlayerAction.MANAGE_ACCESS, coreBe)) {
                    NetworkSecurityManager sec = coreBe.getSecurityManager();
                    switch (payload.actionType()) {
                        case SET_MODE -> {
                            if (payload.modeOrdinal() >= 0 && payload.modeOrdinal() < SecurityMode.values().length) {
                                sec.setMode(SecurityMode.values()[payload.modeOrdinal()]);
                            }
                        }
                        case TOGGLE_PERMISSION -> {
                            if (payload.targetUuid() != null && payload.permissionOrdinal() >= 0 && payload.permissionOrdinal() < SecurityPermission.values().length) {
                                PlayerAccessEntry entry = sec.getEntry(payload.targetUuid());
                                if (entry != null) {
                                    entry.togglePermission(SecurityPermission.values()[payload.permissionOrdinal()]);
                                }
                            }
                        }
                        case ADD_PLAYER -> {
                            if (payload.targetUuid() != null) {
                                String name = !payload.targetName().isBlank() ? payload.targetName() : payload.targetUuid().toString().substring(0, 8);
                                sec.addOrUpdatePlayer(payload.targetUuid(), name, true, true, true, false);
                            }
                        }
                        case REMOVE_PLAYER -> {
                            if (payload.targetUuid() != null) {
                                sec.removePlayer(payload.targetUuid());
                            }
                        }
                    }
                    coreBe.logActivity(sp, io.github.chema280101.logtech.security.ActivityAction.CHANGE_ACCESS, "", 1L);
                    coreBe.setChanged();
                    coreBe.closeRevokedPlayerMenus();
                    menu.triggerImmediateSync();
                }
            }
        });
    }
}
