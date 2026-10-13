package io.github.chema280101.logtech.machine.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record ProcessingOutput(Item item, int count, float chance, OutputScaling scaling) {

    public static final Codec<OutputScaling> SCALING_CODEC = Codec.STRING.xmap(
            OutputScaling::fromName,
            OutputScaling::getSerializedName
    );

    public static final StreamCodec<ByteBuf, OutputScaling> SCALING_STREAM_CODEC = ByteBufCodecs.idMapper(
            id -> OutputScaling.values()[id],
            OutputScaling::ordinal
    );

    public ProcessingOutput(Item item, int count) {
        this(item, count, 1.0f, OutputScaling.NONE);
    }

    public static final Codec<ProcessingOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ProcessingOutput::item),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(ProcessingOutput::count),
            Codec.floatRange(0.0f, 1.0f).optionalFieldOf("chance", 1.0f).forGetter(ProcessingOutput::chance),
            SCALING_CODEC.optionalFieldOf("scaling", OutputScaling.NONE).forGetter(ProcessingOutput::scaling)
    ).apply(instance, ProcessingOutput::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessingOutput> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM), ProcessingOutput::item,
            ByteBufCodecs.VAR_INT, ProcessingOutput::count,
            ByteBufCodecs.FLOAT, ProcessingOutput::chance,
            SCALING_STREAM_CODEC, ProcessingOutput::scaling,
            ProcessingOutput::new
    );

    public ItemStack createStack() {
        return new ItemStack(this.item, this.count);
    }
}
