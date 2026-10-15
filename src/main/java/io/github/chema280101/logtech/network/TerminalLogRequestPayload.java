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
 * Payload enviado del cliente al servidor para solicitar una página paginada del registro de actividad,
 * con índice de página y filtro opcional por jugador.
 */
public record TerminalLogRequestPayload(int containerId, int pageIndex, String playerFilter) implements CustomPacketPayload {

    public static final Type<TerminalLogRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "terminal_log_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalLogRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                buf.writeVarInt(payload.pageIndex());
                buf.writeUtf(payload.playerFilter() != null ? payload.playerFilter() : "");
            },
            buf -> new TerminalLogRequestPayload(buf.readVarInt(), buf.readVarInt(), buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalLogRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player instanceof ServerPlayer serverPlayer
                    && serverPlayer.containerMenu instanceof TerminalMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.handleLogRequest(payload.pageIndex(), payload.playerFilter());
            }
        });
    }
}
