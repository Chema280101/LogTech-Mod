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
        simpleBlockWithItem(ModBlocks.COLD_RACK.get(), cubeAll(ModBlocks.COLD_RACK.get()));
        simpleBlockWithItem(ModBlocks.TERMINAL.get(), cubeAll(ModBlocks.TERMINAL.get()));
        simpleBlockWithItem(ModBlocks.CRUSHER.get(), cubeAll(ModBlocks.CRUSHER.get()));
        simpleBlockWithItem(ModBlocks.CENTRIFUGE.get(), cubeAll(ModBlocks.CENTRIFUGE.get()));
        simpleBlockWithItem(ModBlocks.WASHER.get(), cubeAll(ModBlocks.WASHER.get()));
        simpleBlockWithItem(ModBlocks.SLAG_BLOCK.get(), cubeAll(ModBlocks.SLAG_BLOCK.get()));

        net.neoforged.neoforge.client.model.generators.ModelFile inactiveDrill = cubeAll(ModBlocks.DRILL.get());
        net.neoforged.neoforge.client.model.generators.ModelFile activeDrill = models().cubeAll("drill_active", modLoc("block/drill_active"));
        getVariantBuilder(ModBlocks.DRILL.get())
                .partialState().with(io.github.chema280101.logtech.machine.DrillBlock.ACTIVE, false).modelForState().modelFile(inactiveDrill).addModel()
                .partialState().with(io.github.chema280101.logtech.machine.DrillBlock.ACTIVE, true).modelForState().modelFile(activeDrill).addModel();
        simpleBlockItem(ModBlocks.DRILL.get(), inactiveDrill);

        net.neoforged.neoforge.client.model.generators.ModelFile inactiveAlarm = cubeAll(ModBlocks.ALARM.get());
        net.neoforged.neoforge.client.model.generators.ModelFile activeAlarm = models().cubeAll("alarm_powered", modLoc("block/alarm_powered"));
        var alarmBuilder = getVariantBuilder(ModBlocks.ALARM.get());
        for (int ch = 1; ch <= 4; ch++) {
            alarmBuilder
                    .partialState().with(io.github.chema280101.logtech.block.AlarmBlock.CHANNEL, ch).with(io.github.chema280101.logtech.block.AlarmBlock.POWERED, false)
                    .modelForState().modelFile(inactiveAlarm).addModel()
                    .partialState().with(io.github.chema280101.logtech.block.AlarmBlock.CHANNEL, ch).with(io.github.chema280101.logtech.block.AlarmBlock.POWERED, true)
                    .modelForState().modelFile(activeAlarm).addModel();
        }
        simpleBlockItem(ModBlocks.ALARM.get(), inactiveAlarm);

        net.neoforged.neoforge.client.model.generators.ModelFile inactiveCoalGen = cubeAll(ModBlocks.COAL_GENERATOR.get());
        net.neoforged.neoforge.client.model.generators.ModelFile activeCoalGen = models().cubeAll("coal_generator_active", modLoc("block/coal_generator_active"));
        getVariantBuilder(ModBlocks.COAL_GENERATOR.get())
                .partialState().with(io.github.chema280101.logtech.machine.CoalGeneratorBlock.LIT, false).modelForState().modelFile(inactiveCoalGen).addModel()
                .partialState().with(io.github.chema280101.logtech.machine.CoalGeneratorBlock.LIT, true).modelForState().modelFile(activeCoalGen).addModel();
        simpleBlockItem(ModBlocks.COAL_GENERATOR.get(), inactiveCoalGen);
    }
}
