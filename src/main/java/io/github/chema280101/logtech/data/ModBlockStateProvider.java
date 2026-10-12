package io.github.chema280101.logtech.data;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, LogTech.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(ModBlocks.CORE.get(), cubeAll(ModBlocks.CORE.get()));
        simpleBlockWithItem(ModBlocks.BACKBONE.get(), cubeAll(ModBlocks.BACKBONE.get()));
        simpleBlockWithItem(ModBlocks.RACK.get(), cubeAll(ModBlocks.RACK.get()));
        simpleBlockWithItem(ModBlocks.TERMINAL.get(), cubeAll(ModBlocks.TERMINAL.get()));
        simpleBlockWithItem(ModBlocks.CRUSHER.get(), cubeAll(ModBlocks.CRUSHER.get()));

        net.neoforged.neoforge.client.model.generators.ModelFile inactiveDrill = cubeAll(ModBlocks.DRILL.get());
        net.neoforged.neoforge.client.model.generators.ModelFile activeDrill = models().cubeAll("drill_active", modLoc("block/drill_active"));
        getVariantBuilder(ModBlocks.DRILL.get())
                .partialState().with(io.github.chema280101.logtech.machine.DrillBlock.ACTIVE, false).modelForState().modelFile(inactiveDrill).addModel()
                .partialState().with(io.github.chema280101.logtech.machine.DrillBlock.ACTIVE, true).modelForState().modelFile(activeDrill).addModel();
        simpleBlockItem(ModBlocks.DRILL.get(), inactiveDrill);
    }
}
