package io.github.chema280101.logtech.ore;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Datos inmutables y serializables de una veta descubierta para sincronización con el cliente.
 */
public record VeinInfo(
        String id,
        String typeId,
        int regionX,
        int regionZ,
        int veinIndex,
        int centerX,
        int centerZ,
        int radius,
        int purity,
        int remainingReserve,
        int initialReserve
) {
    public static VeinInfo from(DepositVein vein, DepositSavedData savedData) {
        return new VeinInfo(
                vein.getId(),
                vein.getType().getId(),
                vein.getRegionX(),
                vein.getRegionZ(),
                vein.getVeinIndex(),
                vein.getCenterX(),
                vein.getCenterZ(),
                vein.getRadius(),
                vein.getPurity(),
                savedData.getRemainingReserve(vein),
                vein.getInitialReserve()
        );
    }

    public DepositType getType() {
        return DepositType.fromId(typeId);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        buf.writeUtf(typeId);
        buf.writeVarInt(regionX);
        buf.writeVarInt(regionZ);
        buf.writeVarInt(veinIndex);
        buf.writeVarInt(centerX);
        buf.writeVarInt(centerZ);
        buf.writeVarInt(radius);
        buf.writeVarInt(purity);
        buf.writeVarInt(remainingReserve);
        buf.writeVarInt(initialReserve);
    }

    public static VeinInfo read(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        String typeId = buf.readUtf();
        int regionX = buf.readVarInt();
        int regionZ = buf.readVarInt();
        int veinIndex = buf.readVarInt();
        int centerX = buf.readVarInt();
        int centerZ = buf.readVarInt();
        int radius = buf.readVarInt();
        int purity = buf.readVarInt();
        int remainingReserve = buf.readVarInt();
        int initialReserve = buf.readVarInt();
        return new VeinInfo(id, typeId, regionX, regionZ, veinIndex, centerX, centerZ, radius, purity, remainingReserve, initialReserve);
    }
}
