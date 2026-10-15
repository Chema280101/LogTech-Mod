package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.TerminalMenu;
import io.github.chema280101.logtech.security.ActivityAction;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Payload enviado del servidor al cliente con una página acotada de entradas del registro de actividad.
 */
public record TerminalLogPagePayload(
        int containerId,
        int pageIndex,
        int totalPages,
        int totalEntries,
        List<LogEntryDto> entries
) implements CustomPacketPayload {

    public record LogEntryDto(
            long gameTime,
            UUID playerUuid,
            String playerName,
            ActivityAction action,
            String itemId,
            long amount
    ) {}

    public static final Type<TerminalLogPagePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "terminal_log_page"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalLogPagePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId());
                buf.writeVarInt(payload.pageIndex());
                buf.writeVarInt(payload.totalPages());
                buf.writeVarInt(payload.totalEntries());
                buf.writeVarInt(payload.entries().size());
                for (LogEntryDto entry : payload.entries()) {
                    buf.writeVarLong(entry.gameTime());
                    buf.writeUUID(entry.playerUuid());
                    buf.writeUtf(entry.playerName());
                    buf.writeEnum(entry.action());
                    buf.writeUtf(entry.itemId() != null ? entry.itemId() : "");
                    buf.writeVarLong(entry.amount());
                }
            },
            buf -> {
                int containerId = buf.readVarInt();
                int pageIndex = buf.readVarInt();
                int totalPages = buf.readVarInt();
                int totalEntries = buf.readVarInt();
                int size = buf.readVarInt();
                List<LogEntryDto> entries = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    long gameTime = buf.readVarLong();
                    UUID playerUuid = buf.readUUID();
                    String playerName = buf.readUtf();
                    ActivityAction action = buf.readEnum(ActivityAction.class);
                    String itemId = buf.readUtf();
                    long amount = buf.readVarLong();
                    entries.add(new LogEntryDto(gameTime, playerUuid, playerName, action, itemId, amount));
                }
                return new TerminalLogPagePayload(containerId, pageIndex, totalPages, totalEntries, entries);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalLogPagePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof TerminalMenu menu && menu.containerId == payload.containerId()) {
                menu.receiveLogPage(payload.pageIndex(), payload.totalPages(), payload.totalEntries(), payload.entries());
            }
        });
    }
}
