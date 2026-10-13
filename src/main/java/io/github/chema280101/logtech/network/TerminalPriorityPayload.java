package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.TerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Payload enviado del cliente al servidor para alternar la prioridad de energía de una máquina
 * directamente desde la lista de máquinas en la pestaña de Energía del Terminal.
 */
public record TerminalPriorityPayload(int containerId, BlockPos machinePos, int priorityOrdinal) implements CustomPacketPayload {

    public static final Type<TerminalPriorityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "terminal_priority"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalPriorityPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                buf.writeBlockPos(payload.machinePos());
                buf.writeVarInt(payload.priorityOrdinal());
            },
            buf -> new TerminalPriorityPayload(buf.readVarInt(), buf.readBlockPos(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalPriorityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player instanceof ServerPlayer serverPlayer
                    && serverPlayer.containerMenu instanceof TerminalMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.handleChangeMachinePriority(serverPlayer, payload.machinePos(), EnergyPriority.fromOrdinal(payload.priorityOrdinal()));
            }
        });
    }
}
