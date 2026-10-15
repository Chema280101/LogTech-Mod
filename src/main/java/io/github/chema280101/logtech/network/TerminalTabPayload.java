package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.TerminalMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Payload enviado del cliente al servidor para notificar el cambio o cierre de pestaña en el Terminal.
 * activeTab: 0 = Ítems, 1 = Estadísticas, 2 = Energía.
 */
public record TerminalTabPayload(int containerId, int activeTab) implements CustomPacketPayload {
    public static final int TAB_ITEMS = 0;
    public static final int TAB_STATS = 1;
    public static final int TAB_ENERGY = 2;
    public static final int TAB_TRASH = 3;
    public static final int TAB_LOG = 4;

    public TerminalTabPayload(int containerId, boolean statsTabOpen) {
        this(containerId, statsTabOpen ? TAB_STATS : TAB_ITEMS);
    }

    public static final Type<TerminalTabPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "terminal_tab"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalTabPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                buf.writeVarInt(payload.activeTab());
            },
            buf -> new TerminalTabPayload(buf.readVarInt(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalTabPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player instanceof ServerPlayer serverPlayer
                    && serverPlayer.containerMenu instanceof TerminalMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.handleTabChange(payload.activeTab());
            }
        });
    }
}
