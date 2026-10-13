package io.github.chema280101.logtech.data;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.registry.ModBlocks;
import io.github.chema280101.logtech.registry.ModTags;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, LogTech.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.Blocks.NETWORK_COMPONENT)
                .add(ModBlocks.CORE.get())
                .add(ModBlocks.BACKBONE.get())
                .add(ModBlocks.RACK.get())
                .add(ModBlocks.COLD_RACK.get())
                .add(ModBlocks.TERMINAL.get())
                .add(ModBlocks.CRUSHER.get())
                .add(ModBlocks.DRILL.get())
                .add(ModBlocks.ALARM.get())
                .add(ModBlocks.COAL_GENERATOR.get())
                .add(ModBlocks.CENTRIFUGE.get())
                .add(ModBlocks.WASHER.get());

        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.CORE.get())
                .add(ModBlocks.BACKBONE.get())
                .add(ModBlocks.RACK.get())
                .add(ModBlocks.COLD_RACK.get())
                .add(ModBlocks.TERMINAL.get())
                .add(ModBlocks.CRUSHER.get())
                .add(ModBlocks.DRILL.get())
                .add(ModBlocks.ALARM.get())
                .add(ModBlocks.COAL_GENERATOR.get())
                .add(ModBlocks.CENTRIFUGE.get())
                .add(ModBlocks.WASHER.get())
                .add(ModBlocks.SLAG_BLOCK.get());
    }
}
