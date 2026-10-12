package io.github.chema280101.logtech.block;

import io.github.chema280101.logtech.network.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BackboneBlock extends Block {
    public BackboneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMovedByPiston) {
        super.onPlace(state, level, pos, oldState, isMovedByPiston);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            NetworkManager manager = NetworkManager.get(level);
            if (manager != null) {
                manager.onComponentAdded(pos);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMovedByPiston) {
        super.onRemove(state, level, pos, newState, isMovedByPiston);
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            NetworkManager manager = NetworkManager.get(level);
            if (manager != null) {
                manager.onComponentRemoved(pos);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMovedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMovedByPiston);
    }
}
