package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.client.ClientVeinMapHandler;
import io.github.chema280101.logtech.ore.VeinInfo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Payload enviado del servidor al cliente para abrir la pantalla del mapa 2D con las vetas descubiertas.
 */
public record OpenVeinMapPayload(List<VeinInfo> veins) implements CustomPacketPayload {

    public static final Type<OpenVeinMapPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "open_vein_map"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenVeinMapPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.veins().size());
                for (VeinInfo vein : payload.veins()) {
                    vein.write(buf);
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<VeinInfo> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(VeinInfo.read(buf));
                }
                return new OpenVeinMapPayload(list);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenVeinMapPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientVeinMapHandler.handle(payload);
        });
    }
}
