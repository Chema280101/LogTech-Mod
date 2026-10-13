package io.github.chema280101.logtech.block;

import io.github.chema280101.logtech.registry.ModBlockEntities;
import io.github.chema280101.logtech.storage.StorageTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ColdRackBlockEntity extends RackBlockEntity {
    public ColdRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COLD_RACK.get(), pos, state, StorageTier.COLD);
    }
}
