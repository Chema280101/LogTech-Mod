package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.machine.PriorityConfigurableMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Paquete enviado desde el cliente al servidor cuando el jugador pulsa el botón de prioridad en la pantalla de una máquina.
 */
public record MachinePriorityPayload(int containerId, int priorityOrdinal) implements CustomPacketPayload {

    public static final Type<MachinePriorityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "machine_priority"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachinePriorityPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                buf.writeVarInt(payload.priorityOrdinal());
            },
            buf -> new MachinePriorityPayload(buf.readVarInt(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MachinePriorityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player instanceof ServerPlayer serverPlayer
                    && serverPlayer.containerMenu.containerId == payload.containerId()
                    && serverPlayer.containerMenu instanceof PriorityConfigurableMenu menu) {
                menu.setEnergyPriority(EnergyPriority.fromOrdinal(payload.priorityOrdinal()));
            }
        });
    }
}
