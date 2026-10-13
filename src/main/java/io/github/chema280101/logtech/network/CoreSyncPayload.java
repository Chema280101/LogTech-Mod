package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.CoreMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Paquete enviado desde el servidor al cliente para sincronizar el estado completo del Núcleo y sus reglas.
 */
public record CoreSyncPayload(
        int containerId,
        boolean networkValid,
        int blockCount,
        int energyStored,
        int maxEnergyStored,
        int energyConsumption,
        long totalStored,
        long totalCapacity,
        long hotStored,
        long hotCapacity,
        long coldStored,
        long coldCapacity,
        List<MachineInfo> machines,
        List<RuleDto> rules
) implements CustomPacketPayload {

    public record MachineInfo(net.minecraft.network.chat.Component name, String statusKey, String causeKey) {}

    public record RuleDto(
            int id,
            ItemStack item,
            int conditionOrdinal,
            long threshold,
            int actionOrdinal,
            boolean enabled,
            boolean active
    ) {}

    public static final Type<CoreSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "core_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoreSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                buf.writeBoolean(payload.networkValid());
                buf.writeVarInt(payload.blockCount());
                buf.writeVarInt(payload.energyStored());
                buf.writeVarInt(payload.maxEnergyStored());
                buf.writeVarInt(payload.energyConsumption());
                buf.writeVarLong(payload.totalStored());
                buf.writeVarLong(payload.totalCapacity());
                buf.writeVarLong(payload.hotStored());
                buf.writeVarLong(payload.hotCapacity());
                buf.writeVarLong(payload.coldStored());
                buf.writeVarLong(payload.coldCapacity());

                // Máquinas
                buf.writeVarInt(payload.machines().size());
                for (MachineInfo m : payload.machines()) {
                    net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(buf, m.name());
                    buf.writeUtf(m.statusKey());
                    buf.writeUtf(m.causeKey() != null ? m.causeKey() : "");
                }

                // Reglas
                buf.writeVarInt(payload.rules().size());
                for (RuleDto r : payload.rules()) {
                    buf.writeVarInt(r.id());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, r.item());
                    buf.writeVarInt(r.conditionOrdinal());
                    buf.writeVarLong(r.threshold());
                    buf.writeVarInt(r.actionOrdinal());
                    buf.writeBoolean(r.enabled());
                    buf.writeBoolean(r.active());
                }
            },
            buf -> {
                int containerId = buf.readVarInt();
                boolean networkValid = buf.readBoolean();
                int blockCount = buf.readVarInt();
                int energyStored = buf.readVarInt();
                int maxEnergyStored = buf.readVarInt();
                int energyConsumption = buf.readVarInt();
                long totalStored = buf.readVarLong();
                long totalCapacity = buf.readVarLong();
                long hotStored = buf.readVarLong();
                long hotCapacity = buf.readVarLong();
                long coldStored = buf.readVarLong();
                long coldCapacity = buf.readVarLong();

                int machineSize = buf.readVarInt();
                List<MachineInfo> machines = new ArrayList<>(machineSize);
                for (int i = 0; i < machineSize; i++) {
                    machines.add(new MachineInfo(
                            net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.decode(buf),
                            buf.readUtf(),
                            buf.readUtf()
                    ));
                }

                int ruleSize = buf.readVarInt();
                List<RuleDto> rules = new ArrayList<>(ruleSize);
                for (int i = 0; i < ruleSize; i++) {
                    rules.add(new RuleDto(
                            buf.readVarInt(),
                            ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                            buf.readVarInt(),
                            buf.readVarLong(),
                            buf.readVarInt(),
                            buf.readBoolean(),
                            buf.readBoolean()
                    ));
                }

                return new CoreSyncPayload(
                        containerId, networkValid, blockCount,
                        energyStored, maxEnergyStored, energyConsumption,
                        totalStored, totalCapacity,
                        hotStored, hotCapacity,
                        coldStored, coldCapacity,
                        machines, rules
                );
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CoreSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof CoreMenu menu && menu.containerId == payload.containerId()) {
                menu.receiveSync(payload);
            }
        });
    }
}
