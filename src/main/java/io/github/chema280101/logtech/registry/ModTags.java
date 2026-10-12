package io.github.chema280101.logtech.registry;

import io.github.chema280101.logtech.LogTech;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> NETWORK_COMPONENT =
                TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "network_component"));
    }
}
