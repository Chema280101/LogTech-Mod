package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.client.ClientVeinData;
import io.github.chema280101.logtech.ore.VeinInfo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Payload enviado del servidor al cliente con los datos de las vetas descubiertas por el escáner.
 */
public record DiscoveredVeinsPayload(List<VeinInfo> veins) implements CustomPacketPayload {

    public static final Type<DiscoveredVeinsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "discovered_veins"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiscoveredVeinsPayload> STREAM_CODEC = StreamCodec.of(
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
                return new DiscoveredVeinsPayload(list);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DiscoveredVeinsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientVeinData.addOrUpdate(payload.veins());
        });
    }
}
