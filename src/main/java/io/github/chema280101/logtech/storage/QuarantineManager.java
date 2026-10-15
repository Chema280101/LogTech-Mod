package io.github.chema280101.logtech.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.Map;

/**
 * Gestor de cuarentena adaptado a ItemKey y persistencia NBT de Minecraft para el Núcleo.
 */
public class QuarantineManager extends QuarantineBuffer<ItemKey> {

    public Tag saveQuarantine(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<ItemKey, Long> entry : getEntries().entrySet()) {
            ItemKey item = entry.getKey();
            Long amount = entry.getValue();
            if (item != null && amount != null && amount > 0) {
                CompoundTag tag = new CompoundTag();
                Tag itemTag = ItemKey.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), item)
                        .resultOrPartial()
                        .orElse(null);
                if (itemTag != null) {
                    tag.put("item", itemTag);
                    tag.putLong("amount", amount);
                    list.add(tag);
                }
            }
        }
        return list;
    }

    public void loadQuarantine(Tag tag, HolderLookup.Provider registries, long maxCapacity) {
        clear();
        if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entryTag = list.getCompound(i);
                if (entryTag.contains("item")) {
                    ItemKey item = ItemKey.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), entryTag.get("item"))
                            .resultOrPartial()
                            .orElse(null);
                    if (item != null) {
                        long amount = entryTag.getLong("amount");
                        if (amount > 0) {
                            insert(item, amount, false, maxCapacity);
                        }
                    }
                }
            }
        }
    }
}
