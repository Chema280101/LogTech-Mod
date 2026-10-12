package io.github.chema280101.logtech.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record StorageEntry(ItemKey key, long amount) {
    public static final Codec<StorageEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ItemKey.CODEC.fieldOf("item").forGetter(StorageEntry::key),
                    Codec.LONG.fieldOf("amount").forGetter(StorageEntry::amount)
            ).apply(instance, StorageEntry::new)
    );
}
