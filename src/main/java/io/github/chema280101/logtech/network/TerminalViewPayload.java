package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.TerminalMenu;
import io.github.chema280101.logtech.storage.SavedView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Servidor → Cliente: lista completa de vistas guardadas de la red.
 * Cada elemento puede ser null (slot vacío, representado con {@code occupied=false} en el wire).
 */
public record TerminalViewPayload(int containerId, List<SavedView> views) implements CustomPacketPayload {

    public static final Type<TerminalViewPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "terminal_view"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalViewPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                List<SavedView> views = payload.views();
                buf.writeVarInt(views.size());
                for (SavedView v : views) {
                    buf.writeBoolean(v != null);
                    if (v != null) {
                        // Serializar SavedView via NBT compacto
                        buf.writeNbt(io.github.chema280101.logtech.storage.ViewNbtAdapter.saveView(v));
                    }
                }
            },
            buf -> {
                int containerId = buf.readVarInt();
                int size = buf.readVarInt();
                List<SavedView> views = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    boolean occupied = buf.readBoolean();
                    if (occupied) {
                        CompoundTag tag;
                        try {
                            tag = (CompoundTag) buf.readNbt();
                        } catch (Exception e) {
                            tag = null;
                        }
                        views.add(tag != null ? io.github.chema280101.logtech.storage.ViewNbtAdapter.loadView(tag) : null);
                    } else {
                        views.add(null);
                    }
                }
                return new TerminalViewPayload(containerId, views);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalViewPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof TerminalMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.receiveViewSync(payload.views());
            }
        });
    }
}
