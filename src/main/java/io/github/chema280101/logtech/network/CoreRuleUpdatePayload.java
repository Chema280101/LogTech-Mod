package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.CoreMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Paquete enviado desde el cliente al servidor cuando el jugador modifica una regla del Núcleo.
 */
public record CoreRuleUpdatePayload(
        int containerId,
        int ruleIndex,
        ItemStack item,
        int conditionOrdinal,
        long threshold,
        int actionOrdinal,
        boolean enabled
) implements CustomPacketPayload {

    public static final Type<CoreRuleUpdatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "core_rule_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoreRuleUpdatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                buf.writeVarInt(payload.ruleIndex());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.item());
                buf.writeVarInt(payload.conditionOrdinal());
                buf.writeVarLong(payload.threshold());
                buf.writeVarInt(payload.actionOrdinal());
                buf.writeBoolean(payload.enabled());
            },
            buf -> new CoreRuleUpdatePayload(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    buf.readVarInt(),
                    buf.readVarLong(),
                    buf.readVarInt(),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CoreRuleUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player instanceof ServerPlayer serverPlayer
                    && serverPlayer.containerMenu instanceof CoreMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.handleRuleUpdate(payload);
            }
        });
    }
}
