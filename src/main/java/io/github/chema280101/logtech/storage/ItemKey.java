package io.github.chema280101.logtech.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record ItemKey(Item item, DataComponentPatch patch) {
    public static final Codec<ItemKey> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ItemKey::item),
                    DataComponentPatch.CODEC.optionalFieldOf("patch", DataComponentPatch.EMPTY).forGetter(ItemKey::patch)
            ).apply(instance, ItemKey::new)
    );

    public static ItemKey of(ItemStack stack) {
        return new ItemKey(stack.getItem(), stack.getComponentsPatch());
    }

    public ItemStack toStack(int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.applyComponentsAndValidate(patch);
        return stack;
    }
}
