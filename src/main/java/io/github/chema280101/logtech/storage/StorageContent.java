package io.github.chema280101.logtech.storage;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record StorageContent(List<StorageEntry> entries) {
    public static final StorageContent EMPTY = new StorageContent(List.of());

    public static final Codec<StorageContent> CODEC = StorageEntry.CODEC.listOf().xmap(
            StorageContent::new,
            StorageContent::entries
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageContent> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);
}
