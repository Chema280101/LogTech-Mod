package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.TerminalMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TerminalActionPayload(
        int containerId,
        ActionType actionType,
        int slotIndex,
        ItemStack item
) implements CustomPacketPayload {

    public enum ActionType {
        EXTRACT_STACK,
        EXTRACT_HALF,
        EXTRACT_SHIFT,
        INSERT_CURSOR_ALL,
        INSERT_CURSOR_ONE,
        INSERT_SLOT
    }

    public static final Type<TerminalActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "terminal_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                buf.writeEnum(payload.actionType());
                buf.writeVarInt(payload.slotIndex());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.item());
            },
            buf -> {
                int containerId = buf.readVarInt();
                ActionType actionType = buf.readEnum(ActionType.class);
                int slotIndex = buf.readVarInt();
                ItemStack item = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                return new TerminalActionPayload(containerId, actionType, slotIndex, item);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player instanceof ServerPlayer serverPlayer
                    && serverPlayer.containerMenu instanceof TerminalMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.handleAction(serverPlayer, payload);
            }
        });
    }
}
