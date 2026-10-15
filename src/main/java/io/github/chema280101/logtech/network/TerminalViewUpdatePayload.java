package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.TerminalMenu;
import io.github.chema280101.logtech.storage.SavedView;
import io.github.chema280101.logtech.storage.ViewManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Cliente → Servidor: actualización de vistas guardadas.
 *
 * <ul>
 *   <li>{@link Action#SAVE}   – guarda o sobrescribe una vista en {@code index}.</li>
 *   <li>{@link Action#RENAME} – renombra la vista del slot {@code index} con {@code viewName}.</li>
 *   <li>{@link Action#DELETE} – elimina la vista del slot {@code index}.</li>
 * </ul>
 */
public record TerminalViewUpdatePayload(
        int containerId,
        Action action,
        int index,
        SavedView view       // Solo usado en SAVE; null para RENAME y DELETE
) implements CustomPacketPayload {

    public enum Action { SAVE, RENAME, DELETE }

    public static final Type<TerminalViewUpdatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "terminal_view_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalViewUpdatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                buf.writeEnum(payload.action());
                buf.writeVarInt(payload.index());
                buf.writeBoolean(payload.view() != null);
                if (payload.view() != null) {
                    buf.writeNbt(io.github.chema280101.logtech.storage.ViewNbtAdapter.saveView(payload.view()));
                }
            },
            buf -> {
                int containerId = buf.readVarInt();
                Action action = buf.readEnum(Action.class);
                int index = buf.readVarInt();
                boolean hasView = buf.readBoolean();
                SavedView view = null;
                if (hasView) {
                    CompoundTag tag;
                    try {
                        tag = (CompoundTag) buf.readNbt();
                    } catch (Exception e) {
                        tag = null;
                    }
                    if (tag != null) {
                        view = io.github.chema280101.logtech.storage.ViewNbtAdapter.loadView(tag);
                    }
                }
                return new TerminalViewUpdatePayload(containerId, action, index, view);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalViewUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player instanceof ServerPlayer serverPlayer
                    && serverPlayer.containerMenu instanceof TerminalMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.handleViewUpdate(serverPlayer, payload);
            }
        });
    }
}
