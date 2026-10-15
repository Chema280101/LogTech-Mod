package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.CoreBlockEntity;
import io.github.chema280101.logtech.block.CoreMenu;
import io.github.chema280101.logtech.security.NetworkSecurity;
import io.github.chema280101.logtech.security.PlayerAction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Paquete C2S para reclamar la propiedad de un Núcleo sin dueño.
 */
public record CoreClaimPayload(int containerId) implements CustomPacketPayload {

    public static final Type<CoreClaimPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "core_claim"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoreClaimPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.containerId()),
            buf -> new CoreClaimPayload(buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CoreClaimPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player instanceof ServerPlayer sp && sp.containerMenu instanceof CoreMenu menu && menu.containerId == payload.containerId()) {
                CoreBlockEntity coreBe = menu.getCoreBlockEntity(sp);
                if (coreBe != null && NetworkSecurity.canPlayerDo(sp, PlayerAction.CLAIM_CORE, coreBe)) {
                    coreBe.getSecurityManager().setOwner(sp.getUUID(), sp.getName().getString());
                    coreBe.logActivity(sp, io.github.chema280101.logtech.security.ActivityAction.CHANGE_ACCESS, "", 1L);
                    coreBe.setChanged();
                    menu.triggerImmediateSync();
                }
            }
        });
    }
}
