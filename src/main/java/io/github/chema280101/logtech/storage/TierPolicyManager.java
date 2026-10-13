package io.github.chema280101.logtech.storage;

import com.mojang.logging.LogUtils;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;

/**
 * Gestor de la política de niveles caliente y frío adaptado a ItemKey y NeoForge.
 * Hereda la lógica algorítmica pura de TierPolicyLogic y añade la persistencia NBT de Minecraft.
 */
public class TierPolicyManager extends TierPolicyLogic<ItemKey> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public TierPolicyManager() {
        super();
        setDebugLogger(LOGGER::info);
    }

    public Tag savePinnedPolicies(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<ItemKey, ItemPinMode> entry : getPinnedPolicies().entrySet()) {
            if (entry.getValue() != ItemPinMode.AUTO) {
                CompoundTag tag = new CompoundTag();
                Tag keyTag = ItemKey.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), entry.getKey())
                        .resultOrPartial()
                        .orElse(null);
                if (keyTag != null) {
                    tag.put("key", keyTag);
                    tag.putString("mode", entry.getValue().name());
                    list.add(tag);
                }
            }
        }
        return list;
    }

    public void loadPinnedPolicies(Tag tag, HolderLookup.Provider registries) {
        clear();
        if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
                if (itemTag.contains("key") && itemTag.contains("mode")) {
                    ItemKey.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), itemTag.get("key"))
                            .resultOrPartial()
                            .ifPresent(key -> {
                                String modeName = itemTag.getString("mode");
                                try {
                                    ItemPinMode mode = ItemPinMode.valueOf(modeName);
                                    if (mode != ItemPinMode.AUTO) {
                                        setPinMode(key, mode);
                                    }
                                } catch (IllegalArgumentException ignored) {
                                }
                            });
                }
            }
        }
    }
}
