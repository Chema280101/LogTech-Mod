package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.TerminalMenu;
import io.github.chema280101.logtech.machine.MachineStatus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Payload enviado del servidor al cliente con la telemetría y estadísticas de la red.
 * Solo se envía cada 20 ticks mientras la pestaña de estadísticas esté abierta.
 */
public record TerminalStatsPayload(
        int containerId,
        long usedCapacity,
        long totalCapacity,
        int itemsPerMinuteLast,
        float itemsPerMinute5MinAvg,
        int energyStored,
        int energyCapacity,
        int energyConsumption,
        int[] hourlyHistory,
        List<MachineStatEntry> machines,
        long quarantineCount,
        long hotUsedCapacity,
        long hotTotalCapacity,
        long coldUsedCapacity,
        long coldTotalCapacity,
        long hotNetRate,
        long coldNetRate,
        long totalNetRate
) implements CustomPacketPayload {

    public TerminalStatsPayload(
            int containerId,
            long usedCapacity,
            long totalCapacity,
            int itemsPerMinuteLast,
            float itemsPerMinute5MinAvg,
            int energyStored,
            int energyCapacity,
            int energyConsumption,
            int[] hourlyHistory,
            List<MachineStatEntry> machines,
            long quarantineCount
    ) {
        this(
                containerId,
                usedCapacity,
                totalCapacity,
                itemsPerMinuteLast,
                itemsPerMinute5MinAvg,
                energyStored,
                energyCapacity,
                energyConsumption,
                hourlyHistory,
                machines,
                quarantineCount,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L
        );
    }

    public static final Type<TerminalStatsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "terminal_stats"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalStatsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                buf.writeVarLong(payload.usedCapacity());
                buf.writeVarLong(payload.totalCapacity());
                buf.writeVarInt(payload.itemsPerMinuteLast());
                buf.writeFloat(payload.itemsPerMinute5MinAvg());
                buf.writeVarInt(payload.energyStored());
                buf.writeVarInt(payload.energyCapacity());
                buf.writeVarInt(payload.energyConsumption());

                buf.writeVarInt(payload.hourlyHistory().length);
                for (int val : payload.hourlyHistory()) {
                    buf.writeVarInt(val);
                }

                buf.writeVarInt(payload.machines().size());
                for (MachineStatEntry machine : payload.machines()) {
                    ComponentSerialization.STREAM_CODEC.encode(buf, machine.name());
                    buf.writeEnum(machine.status());
                    buf.writeVarInt(machine.itemsPerMinute());
                    buf.writeVarInt(machine.heat());
                    buf.writeEnum(machine.firmware());
                }

                buf.writeVarLong(payload.quarantineCount());
                buf.writeVarLong(payload.hotUsedCapacity());
                buf.writeVarLong(payload.hotTotalCapacity());
                buf.writeVarLong(payload.coldUsedCapacity());
                buf.writeVarLong(payload.coldTotalCapacity());
                buf.writeLong(payload.hotNetRate());
                buf.writeLong(payload.coldNetRate());
                buf.writeLong(payload.totalNetRate());
            },
            buf -> {
                int containerId = buf.readVarInt();
                long usedCapacity = buf.readVarLong();
                long totalCapacity = buf.readVarLong();
                int itemsPerMinuteLast = buf.readVarInt();
                float itemsPerMinute5MinAvg = buf.readFloat();
                int energyStored = buf.readVarInt();
                int energyCapacity = buf.readVarInt();
                int energyConsumption = buf.readVarInt();

                int histLen = buf.readVarInt();
                int[] history = new int[histLen];
                for (int i = 0; i < histLen; i++) {
                    history[i] = buf.readVarInt();
                }

                int machinesLen = buf.readVarInt();
                List<MachineStatEntry> machines = new ArrayList<>(machinesLen);
                for (int i = 0; i < machinesLen; i++) {
                    Component name = ComponentSerialization.STREAM_CODEC.decode(buf);
                    MachineStatus status = buf.readEnum(MachineStatus.class);
                    int itemsPerMinute = buf.readVarInt();
                    int heat = buf.readVarInt();
                    io.github.chema280101.logtech.machine.firmware.FirmwareType firmware =
                            buf.readEnum(io.github.chema280101.logtech.machine.firmware.FirmwareType.class);
                    machines.add(new MachineStatEntry(name, status, itemsPerMinute, heat, firmware));
                }

                long quarantineCount = buf.readVarLong();
                long hotUsedCapacity = buf.readVarLong();
                long hotTotalCapacity = buf.readVarLong();
                long coldUsedCapacity = buf.readVarLong();
                long coldTotalCapacity = buf.readVarLong();
                long hotNetRate = buf.readLong();
                long coldNetRate = buf.readLong();
                long totalNetRate = buf.readLong();

                return new TerminalStatsPayload(
                        containerId,
                        usedCapacity,
                        totalCapacity,
                        itemsPerMinuteLast,
                        itemsPerMinute5MinAvg,
                        energyStored,
                        energyCapacity,
                        energyConsumption,
                        history,
                        machines,
                        quarantineCount,
                        hotUsedCapacity,
                        hotTotalCapacity,
                        coldUsedCapacity,
                        coldTotalCapacity,
                        hotNetRate,
                        coldNetRate,
                        totalNetRate
                );
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalStatsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof TerminalMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.receiveStats(payload);
            }
        });
    }

    public record MachineStatEntry(Component name, MachineStatus status, int itemsPerMinute, int heat, io.github.chema280101.logtech.machine.firmware.FirmwareType firmware) {
        public MachineStatEntry(Component name, MachineStatus status, int itemsPerMinute, int heat) {
            this(name, status, itemsPerMinute, heat, io.github.chema280101.logtech.machine.firmware.FirmwareType.NONE);
        }

        public MachineStatEntry(Component name, MachineStatus status, int itemsPerMinute) {
            this(name, status, itemsPerMinute, 0, io.github.chema280101.logtech.machine.firmware.FirmwareType.NONE);
        }
    }
}
