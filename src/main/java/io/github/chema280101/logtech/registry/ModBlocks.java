package io.github.chema280101.logtech.registry;

import io.github.chema280101.logtech.LogTech;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LogTech.MOD_ID);

    // Nucleo: el cerebro de la red.
    public static final DeferredBlock<io.github.chema280101.logtech.block.CoreBlock> CORE = BLOCKS.registerBlock(
            "core",
            io.github.chema280101.logtech.block.CoreBlock::new,
            BlockBehaviour.Properties.of().destroyTime(3.0f).explosionResistance(6.0f)
    );

    // Backbone: cable/bus de la red de tecnologia
    public static final DeferredBlock<io.github.chema280101.logtech.block.BackboneBlock> BACKBONE = BLOCKS.registerBlock(
            "backbone",
            io.github.chema280101.logtech.block.BackboneBlock::new,
            BlockBehaviour.Properties.of().destroyTime(2.0f).explosionResistance(3.0f)
    );

    // Rack: servidor de almacenamiento que aloja modulos de datos (nivel caliente)
    public static final DeferredBlock<io.github.chema280101.logtech.block.RackBlock> RACK = BLOCKS.registerBlock(
            "rack",
            io.github.chema280101.logtech.block.RackBlock::new,
            BlockBehaviour.Properties.of().destroyTime(2.5f).explosionResistance(5.0f)
    );

    // Rack frio: servidor de almacenamiento que aloja modulos de datos (nivel frio, 4x capacidad)
    public static final DeferredBlock<io.github.chema280101.logtech.block.RackBlock> COLD_RACK = BLOCKS.registerBlock(
            "cold_rack",
            props -> new io.github.chema280101.logtech.block.RackBlock(io.github.chema280101.logtech.storage.StorageTier.COLD, props),
            BlockBehaviour.Properties.of().destroyTime(2.5f).explosionResistance(5.0f)
    );

    // Terminal: interfaz de acceso y visualización de items de la red
    public static final DeferredBlock<io.github.chema280101.logtech.block.TerminalBlock> TERMINAL = BLOCKS.registerBlock(
            "terminal",
            io.github.chema280101.logtech.block.TerminalBlock::new,
            BlockBehaviour.Properties.of().destroyTime(2.5f).explosionResistance(5.0f)
    );

    // Triturador: maquina que procesa minerales brutos en polvos
    public static final DeferredBlock<io.github.chema280101.logtech.machine.CrusherBlock> CRUSHER = BLOCKS.registerBlock(
            "crusher",
            io.github.chema280101.logtech.machine.CrusherBlock::new,
            BlockBehaviour.Properties.of().destroyTime(3.0f).explosionResistance(6.0f)
    );

    // Perforadora: maquina que extrae mineral crudo de una veta descubierta
    public static final DeferredBlock<io.github.chema280101.logtech.machine.DrillBlock> DRILL = BLOCKS.registerBlock(
            "drill",
            io.github.chema280101.logtech.machine.DrillBlock::new,
            BlockBehaviour.Properties.of().destroyTime(3.0f).explosionResistance(6.0f)
    );

    // Alarma: emite señal de redstone según reglas de la red
    public static final DeferredBlock<io.github.chema280101.logtech.block.AlarmBlock> ALARM = BLOCKS.registerBlock(
            "alarm",
            io.github.chema280101.logtech.block.AlarmBlock::new,
            BlockBehaviour.Properties.of().destroyTime(2.0f).explosionResistance(3.0f)
    );

    // Generador temporal de carbon
    public static final DeferredBlock<io.github.chema280101.logtech.machine.CoalGeneratorBlock> COAL_GENERATOR = BLOCKS.registerBlock(
            "coal_generator",
            io.github.chema280101.logtech.machine.CoalGeneratorBlock::new,
            BlockBehaviour.Properties.of().destroyTime(3.0f).explosionResistance(6.0f).lightLevel(state -> state.getValue(io.github.chema280101.logtech.machine.CoalGeneratorBlock.LIT) ? 13 : 0)
    );

    // Centrífuga: máquina que separa subproductos según pureza
    public static final DeferredBlock<io.github.chema280101.logtech.machine.CentrifugeBlock> CENTRIFUGE = BLOCKS.registerBlock(
            "centrifuge",
            io.github.chema280101.logtech.machine.CentrifugeBlock::new,
            BlockBehaviour.Properties.of().destroyTime(3.0f).explosionResistance(6.0f)
    );

    // Bloque de escoria industrial (bloque decorativo obtenido de 4 escorias)
    public static final DeferredBlock<Block> SLAG_BLOCK = BLOCKS.registerBlock(
            "slag_block",
            Block::new,
            BlockBehaviour.Properties.of().destroyTime(2.0f).explosionResistance(6.0f)
    );

    // Lavadora: maquina que mejora la pureza de polvos consumiendo agua y produciendo lodo
    public static final DeferredBlock<io.github.chema280101.logtech.machine.WasherBlock> WASHER = BLOCKS.registerBlock(
            "washer",
            io.github.chema280101.logtech.machine.WasherBlock::new,
            BlockBehaviour.Properties.of().destroyTime(3.0f).explosionResistance(6.0f)
    );

    // Disipador: bloque pasivo de disipacion termica adyacente a maquinas
    public static final DeferredBlock<io.github.chema280101.logtech.block.HeatsinkBlock> HEATSINK = BLOCKS.registerBlock(
            "heatsink",
            io.github.chema280101.logtech.block.HeatsinkBlock::new,
            BlockBehaviour.Properties.of().destroyTime(2.0f).explosionResistance(4.0f).sound(net.minecraft.world.level.block.SoundType.METAL)
    );
}
