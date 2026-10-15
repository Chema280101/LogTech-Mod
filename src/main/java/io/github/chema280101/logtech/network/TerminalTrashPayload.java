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

import java.util.ArrayList;
import java.util.List;

/**
 * Payload enviado del servidor al cliente con las entradas actuales de la papelera del Núcleo.
 * Solo se envía periódicamente mientras la pestaña Papelera está abierta en el Terminal o al cambiar ítems.
 */
public record TerminalTrashPayload(int containerId, List<TrashItemEntry> entries) implements CustomPacketPayload {

    public record TrashItemEntry(ItemStack stack, long amount, long gameTime, long ticksRemaining) {
    }

    public static final Type<TerminalTrashPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "terminal_trash"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalTrashPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                buf.writeVarInt(payload.entries().size());
                for (TrashItemEntry entry : payload.entries()) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.stack());
                    buf.writeVarLong(entry.amount());
                    buf.writeVarLong(entry.gameTime());
                    buf.writeVarLong(entry.ticksRemaining());
                }
            },
            buf -> {
                int containerId = buf.readVarInt();
                int size = buf.readVarInt();
                List<TrashItemEntry> entries = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                    long amount = buf.readVarLong();
                    long gameTime = buf.readVarLong();
                    long ticksRemaining = buf.readVarLong();
                    entries.add(new TrashItemEntry(stack, amount, gameTime, ticksRemaining));
                }
                return new TerminalTrashPayload(containerId, entries);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalTrashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof TerminalMenu menu && menu.containerId == payload.containerId()) {
                menu.receiveTrashSync(payload.entries());
            }
        });
    }
}
