package io.github.chema280101.logtech.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

/**
 * Paquete de configuración transferible mediante la llave de configuración.
 * Contiene el tipo de bloque al que pertenece y un CompoundTag con los datos.
 */
public record ConfigPackage(String type, CompoundTag data) {
    public ConfigPackage {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(data, "data cannot be null");
    }

    public static final Codec<ConfigPackage> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("type").forGetter(ConfigPackage::type),
                    CompoundTag.CODEC.fieldOf("data").forGetter(ConfigPackage::data)
            ).apply(instance, ConfigPackage::new)
    );

    public static final StreamCodec<ByteBuf, ConfigPackage> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
