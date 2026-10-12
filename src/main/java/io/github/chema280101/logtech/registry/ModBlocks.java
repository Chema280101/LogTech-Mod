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

    // Rack: servidor de almacenamiento que aloja modulos de datos
    public static final DeferredBlock<io.github.chema280101.logtech.block.RackBlock> RACK = BLOCKS.registerBlock(
            "rack",
            io.github.chema280101.logtech.block.RackBlock::new,
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
}
