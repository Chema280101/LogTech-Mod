package io.github.chema280101.logtech.data;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.block.AlarmBlock;
import io.github.chema280101.logtech.machine.CoalGeneratorBlock;
import io.github.chema280101.logtech.machine.DrillBlock;
import io.github.chema280101.logtech.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, LogTech.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Core: Servidor central con disipador superior, base reforzada y bus de datos en laterales
        ModelFile coreModel = models().orientableWithBottom("core", modLoc("block/core_side"), modLoc("block/core_front"), modLoc("block/machine_bottom"), modLoc("block/core_top"));
        horizontalBlock(ModBlocks.CORE.get(), coreModel);
        simpleBlockItem(ModBlocks.CORE.get(), coreModel);

        // Terminal: Pantalla de telemetría y consola digital en el frente
        ModelFile terminalModel = models().orientableWithBottom("terminal", modLoc("block/terminal_side"), modLoc("block/terminal_front"), modLoc("block/machine_bottom"), modLoc("block/terminal_top"));
        horizontalBlock(ModBlocks.TERMINAL.get(), terminalModel);
        simpleBlockItem(ModBlocks.TERMINAL.get(), terminalModel);

        // Rack Caliente: Servidor de almacenamiento con 4 bahías frontales
        ModelFile rackModel = models().orientableWithBottom("rack", modLoc("block/rack_side"), modLoc("block/rack_front"), modLoc("block/machine_bottom"), modLoc("block/machine_top"));
        horizontalBlock(ModBlocks.RACK.get(), rackModel);
        simpleBlockItem(ModBlocks.RACK.get(), rackModel);

        // Rack Frío: Servidor criogénico de archivo con bahías de módulos y disipación fría
        ModelFile coldRackModel = models().orientableWithBottom("cold_rack", modLoc("block/cold_rack_side"), modLoc("block/cold_rack_front"), modLoc("block/machine_bottom"), modLoc("block/machine_top"));
        horizontalBlock(ModBlocks.COLD_RACK.get(), coldRackModel);
        simpleBlockItem(ModBlocks.COLD_RACK.get(), coldRackModel);

        // Triturador: Mandíbulas trituradoras al frente y tolva superior de alimentación
        ModelFile crusherModel = models().orientableWithBottom("crusher", modLoc("block/machine_side"), modLoc("block/crusher_front"), modLoc("block/machine_bottom"), modLoc("block/crusher_top"));
        horizontalBlock(ModBlocks.CRUSHER.get(), crusherModel);
        simpleBlockItem(ModBlocks.CRUSHER.get(), crusherModel);

        // Lavadora: Visor con cámara de lavado al frente y válvula superior de entrada de fluidos
        ModelFile washerModel = models().orientableWithBottom("washer", modLoc("block/machine_side"), modLoc("block/washer_front"), modLoc("block/machine_bottom"), modLoc("block/washer_top"));
        horizontalBlock(ModBlocks.WASHER.get(), washerModel);
        simpleBlockItem(ModBlocks.WASHER.get(), washerModel);

        // Centrífuga: Tambor giratorio de separación al frente con salidas de subproductos
        ModelFile centrifugeModel = models().orientableWithBottom("centrifuge", modLoc("block/machine_side"), modLoc("block/centrifuge_front"), modLoc("block/machine_bottom"), modLoc("block/machine_top"));
        horizontalBlock(ModBlocks.CENTRIFUGE.get(), centrifugeModel);
        simpleBlockItem(ModBlocks.CENTRIFUGE.get(), centrifugeModel);

        // Generador de Carbón: Puerta de combustión apagada / encendida con fuego y chimenea superior
        ModelFile inactiveCoalGen = models().orientableWithBottom("coal_generator", modLoc("block/machine_side"), modLoc("block/coal_generator_front"), modLoc("block/machine_bottom"), modLoc("block/coal_generator_top"));
        ModelFile activeCoalGen = models().orientableWithBottom("coal_generator_active", modLoc("block/machine_side"), modLoc("block/coal_generator_front_active"), modLoc("block/machine_bottom"), modLoc("block/coal_generator_top"));
        horizontalBlock(ModBlocks.COAL_GENERATOR.get(), state -> state.getValue(CoalGeneratorBlock.LIT) ? activeCoalGen : inactiveCoalGen);
        simpleBlockItem(ModBlocks.COAL_GENERATOR.get(), inactiveCoalGen);

        // Perforadora: Cabezal de perforación inferior, motor superior y pistones hidráulicos
        ModelFile inactiveDrill = models().orientableWithBottom("drill", modLoc("block/drill_side"), modLoc("block/drill_side"), modLoc("block/drill_bottom"), modLoc("block/drill_top"));
        ModelFile activeDrill = models().orientableWithBottom("drill_active", modLoc("block/drill_side_active"), modLoc("block/drill_side_active"), modLoc("block/drill_bottom"), modLoc("block/drill_top"));
        horizontalBlock(ModBlocks.DRILL.get(), state -> state.getValue(DrillBlock.ACTIVE) ? activeDrill : inactiveDrill);
        simpleBlockItem(ModBlocks.DRILL.get(), inactiveDrill);

        // Backbone y Bloque de Escoria (Bloques simétricos multidireccionales)
        simpleBlockWithItem(ModBlocks.BACKBONE.get(), cubeAll(ModBlocks.BACKBONE.get()));
        simpleBlockWithItem(ModBlocks.SLAG_BLOCK.get(), cubeAll(ModBlocks.SLAG_BLOCK.get()));

        // Disipador: bloque pasivo de disipación con aletas superiores y laterales
        ModelFile heatsinkModel = models().cubeBottomTop("heatsink", modLoc("block/heatsink_side"), modLoc("block/machine_bottom"), modLoc("block/heatsink_top"));
        simpleBlockWithItem(ModBlocks.HEATSINK.get(), heatsinkModel);

        // Alarma: Baliza indicadora con canales 1-4 y estado encendido/apagado
        ModelFile inactiveAlarm = cubeAll(ModBlocks.ALARM.get());
        ModelFile activeAlarm = models().cubeAll("alarm_powered", modLoc("block/alarm_powered"));
        var alarmBuilder = getVariantBuilder(ModBlocks.ALARM.get());
        for (int ch = 1; ch <= 4; ch++) {
            alarmBuilder
                    .partialState().with(AlarmBlock.CHANNEL, ch).with(AlarmBlock.POWERED, false)
                    .modelForState().modelFile(inactiveAlarm).addModel()
                    .partialState().with(AlarmBlock.CHANNEL, ch).with(AlarmBlock.POWERED, true)
                    .modelForState().modelFile(activeAlarm).addModel();
        }
        simpleBlockItem(ModBlocks.ALARM.get(), inactiveAlarm);
    }
}
