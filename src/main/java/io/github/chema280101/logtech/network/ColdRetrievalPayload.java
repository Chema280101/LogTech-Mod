package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.TerminalMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ColdRetrievalPayload(
        int containerId,
        ItemStack item,
        int amount,
        int remainingTicks,
        int totalTicks,
        int queueSize
) implements CustomPacketPayload {

    public static final Type<ColdRetrievalPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "cold_retrieval"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ColdRetrievalPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.item());
                buf.writeVarInt(payload.amount());
                buf.writeVarInt(payload.remainingTicks());
                buf.writeVarInt(payload.totalTicks());
                buf.writeVarInt(payload.queueSize());
            },
            buf -> {
                int containerId = buf.readVarInt();
                ItemStack item = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                int amount = buf.readVarInt();
                int remainingTicks = buf.readVarInt();
                int totalTicks = buf.readVarInt();
                int queueSize = buf.readVarInt();
                return new ColdRetrievalPayload(containerId, item, amount, remainingTicks, totalTicks, queueSize);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ColdRetrievalPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player != null && player.containerMenu instanceof TerminalMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.handleColdRetrievalUpdate(payload);
            }
        });
    }
}
