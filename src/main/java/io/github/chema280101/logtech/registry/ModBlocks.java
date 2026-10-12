package io.github.chema280101.logtech.registry;

import io.github.chema280101.logtech.LogTech;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LogTech.MOD_ID);

    // Nucleo: el cerebro de la red. Por ahora es solo un bloque.
    public static final DeferredBlock<Block> CORE = BLOCKS.registerSimpleBlock(
            "core",
            BlockBehaviour.Properties.of().destroyTime(3.0f).explosionResistance(6.0f)
    );
}
