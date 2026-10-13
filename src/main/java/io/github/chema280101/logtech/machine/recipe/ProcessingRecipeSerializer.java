package io.github.chema280101.logtech.machine.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ProcessingRecipeSerializer<T extends ProcessingRecipe> implements RecipeSerializer<T> {

    @FunctionalInterface
    public interface Factory<T extends ProcessingRecipe> {
        T create(
                Ingredient ingredient,
                List<ProcessingOutput> outputs,
                int energy,
                int ticks,
                PurityMode purityMode,
                int fluidAmount
        );
    }

    public static final Codec<PurityMode> PURITY_MODE_CODEC = Codec.STRING.xmap(
            PurityMode::fromName,
            PurityMode::getSerializedName
    );

    public static final StreamCodec<ByteBuf, PurityMode> PURITY_MODE_STREAM_CODEC = ByteBufCodecs.idMapper(
            id -> PurityMode.values()[id],
            PurityMode::ordinal
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, List<ProcessingOutput>> OUTPUTS_STREAM_CODEC =
            ProcessingOutput.STREAM_CODEC.apply(ByteBufCodecs.list());

    private final MapCodec<T> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

    public ProcessingRecipeSerializer(Factory<T> factory) {
        this.codec = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(ProcessingRecipe::ingredient),
                ProcessingOutput.CODEC.listOf().fieldOf("outputs").forGetter(ProcessingRecipe::outputs),
                Codec.INT.fieldOf("energy").forGetter(ProcessingRecipe::energy),
                Codec.INT.fieldOf("ticks").forGetter(ProcessingRecipe::ticks),
                PURITY_MODE_CODEC.optionalFieldOf("purity_mode", PurityMode.PRESERVE).forGetter(ProcessingRecipe::purityMode),
                Codec.INT.optionalFieldOf("fluid_amount", 0).forGetter(ProcessingRecipe::fluidAmount)
        ).apply(inst, factory::create));

        this.streamCodec = StreamCodec.of(
                (buf, recipe) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient());
                    OUTPUTS_STREAM_CODEC.encode(buf, recipe.outputs());
                    buf.writeVarInt(recipe.energy());
                    buf.writeVarInt(recipe.ticks());
                    PURITY_MODE_STREAM_CODEC.encode(buf, recipe.purityMode());
                    buf.writeVarInt(recipe.fluidAmount());
                },
                buf -> {
                    Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                    List<ProcessingOutput> outputs = OUTPUTS_STREAM_CODEC.decode(buf);
                    int energy = buf.readVarInt();
                    int ticks = buf.readVarInt();
                    PurityMode purityMode = PURITY_MODE_STREAM_CODEC.decode(buf);
                    int fluidAmount = buf.readVarInt();
                    return factory.create(ingredient, outputs, energy, ticks, purityMode, fluidAmount);
                }
        );
    }

    @Override
    public MapCodec<T> codec() {
        return this.codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
        return this.streamCodec;
    }
}
