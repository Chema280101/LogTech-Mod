package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.TerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload enviado del servidor al cliente con la telemetría de energía de la red.
 * Solo se envía periódicamente mientras la pestaña de energía esté abierta en el Terminal.
 */
public record TerminalEnergyPayload(
        int containerId,
        int energyStored,
        int energyCapacity,
        int energyReceivedPerMinute,
        int energyConsumedPerMinute,
        int[] receivedHistory,
        int[] consumedHistory,
        long storageBranchConsumption,
        long mineralsBranchConsumption,
        List<MachineEnergyEntry> machines
) implements CustomPacketPayload {

    public static final Type<TerminalEnergyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "terminal_energy"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalEnergyPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                buf.writeVarInt(payload.energyStored());
                buf.writeVarInt(payload.energyCapacity());
                buf.writeVarInt(payload.energyReceivedPerMinute());
                buf.writeVarInt(payload.energyConsumedPerMinute());

                buf.writeVarInt(payload.receivedHistory().length);
                for (int val : payload.receivedHistory()) {
                    buf.writeVarInt(val);
                }

                buf.writeVarInt(payload.consumedHistory().length);
                for (int val : payload.consumedHistory()) {
                    buf.writeVarInt(val);
                }

                buf.writeVarLong(payload.storageBranchConsumption());
                buf.writeVarLong(payload.mineralsBranchConsumption());

                buf.writeVarInt(payload.machines().size());
                for (MachineEnergyEntry machine : payload.machines()) {
                    buf.writeBlockPos(machine.pos());
                    ComponentSerialization.STREAM_CODEC.encode(buf, machine.name());
                    buf.writeVarInt(machine.energyConsumed());
                    buf.writeEnum(machine.priority());
                    buf.writeBoolean(machine.isCore());
                }
            },
            buf -> {
                int containerId = buf.readVarInt();
                int energyStored = buf.readVarInt();
                int energyCapacity = buf.readVarInt();
                int energyReceivedPerMinute = buf.readVarInt();
                int energyConsumedPerMinute = buf.readVarInt();

                int recLen = buf.readVarInt();
                int[] receivedHistory = new int[recLen];
                for (int i = 0; i < recLen; i++) {
                    receivedHistory[i] = buf.readVarInt();
                }

                int conLen = buf.readVarInt();
                int[] consumedHistory = new int[conLen];
                for (int i = 0; i < conLen; i++) {
                    consumedHistory[i] = buf.readVarInt();
                }

                long storageBranchConsumption = buf.readVarLong();
                long mineralsBranchConsumption = buf.readVarLong();

                int machinesLen = buf.readVarInt();
                List<MachineEnergyEntry> machines = new ArrayList<>(machinesLen);
                for (int i = 0; i < machinesLen; i++) {
                    BlockPos pos = buf.readBlockPos();
                    Component name = ComponentSerialization.STREAM_CODEC.decode(buf);
                    int energyConsumed = buf.readVarInt();
                    EnergyPriority priority = buf.readEnum(EnergyPriority.class);
                    boolean isCore = buf.readBoolean();
                    machines.add(new MachineEnergyEntry(pos, name, energyConsumed, priority, isCore));
                }

                return new TerminalEnergyPayload(
                        containerId,
                        energyStored,
                        energyCapacity,
                        energyReceivedPerMinute,
                        energyConsumedPerMinute,
                        receivedHistory,
                        consumedHistory,
                        storageBranchConsumption,
                        mineralsBranchConsumption,
                        machines
                );
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalEnergyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof TerminalMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.receiveEnergyStats(payload);
            }
        });
    }

    public record MachineEnergyEntry(BlockPos pos, Component name, int energyConsumed, EnergyPriority priority, boolean isCore) {}
}
