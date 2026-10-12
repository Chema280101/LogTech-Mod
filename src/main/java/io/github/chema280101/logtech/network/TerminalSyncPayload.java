package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.TerminalMenu;
import io.github.chema280101.logtech.storage.TerminalItemEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TerminalSyncPayload(
        int containerId,
        boolean networkValid,
        boolean powered,
        int batchIndex,
        int totalBatches,
        List<TerminalItemEntry> items
) implements CustomPacketPayload {

    public static final Type<TerminalSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "terminal_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                buf.writeBoolean(payload.networkValid());
                buf.writeBoolean(payload.powered());
                buf.writeVarInt(payload.batchIndex());
                buf.writeVarInt(payload.totalBatches());
                buf.writeVarInt(payload.items().size());
                for (TerminalItemEntry entry : payload.items()) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.stack());
                    buf.writeVarLong(entry.amount());
                }
            },
            buf -> {
                int containerId = buf.readVarInt();
                boolean networkValid = buf.readBoolean();
                boolean powered = buf.readBoolean();
                int batchIndex = buf.readVarInt();
                int totalBatches = buf.readVarInt();
                int size = buf.readVarInt();
                List<TerminalItemEntry> items = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                    long amount = buf.readVarLong();
                    items.add(new TerminalItemEntry(stack, amount));
                }
                return new TerminalSyncPayload(containerId, networkValid, powered, batchIndex, totalBatches, items);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof TerminalMenu menu && menu.containerId == payload.containerId()) {
                menu.receiveSync(payload.networkValid(), payload.powered(), payload.batchIndex(), payload.totalBatches(), payload.items());
            }
        });
    }
}
