package io.github.chema280101.logtech.storage;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/**
 * Gestiona el almacenamiento, capacidad y estado de un nivel específico (HOT o COLD) para ItemKey.
 * Hereda la lógica pura de almacenamiento y movimiento atómico de SimpleTier.
 */
public class TierStorage extends SimpleTier<ItemKey> {
    private final StorageTier tier;

    public TierStorage(StorageTier tier) {
        this(tier, 0L);
    }

    public TierStorage(StorageTier tier, long capacity) {
        super(Objects.requireNonNull(tier, "tier cannot be null").getName(), capacity);
        this.tier = tier;
    }

    public StorageTier getTier() {
        return this.tier;
    }

    public StorageContent getStorageContent() {
        List<StorageEntry> entries = getAll().entrySet().stream()
                .map(e -> new StorageEntry(e.getKey(), e.getValue()))
                .toList();
        return new StorageContent(entries);
    }

    public void setContent(StorageContent content) {
        clear();
        if (content != null) {
            for (StorageEntry entry : content.entries()) {
                if (entry.amount() > 0) {
                    setDirect(entry.key(), entry.amount());
                }
            }
        }
    }

    public Tag save(HolderLookup.Provider registries) {
        StorageContent content = getStorageContent();
        return StorageContent.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), content)
                .resultOrPartial()
                .orElseGet(CompoundTag::new);
    }

    public void load(Tag tag, HolderLookup.Provider registries) {
        if (tag == null) {
            clear();
            return;
        }
        StorageContent.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial()
                .ifPresent(this::setContent);
    }
}
