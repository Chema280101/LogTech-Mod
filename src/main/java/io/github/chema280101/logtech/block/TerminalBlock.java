package io.github.chema280101.logtech.block;

import io.github.chema280101.logtech.network.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class TerminalBlock extends Block {
    public TerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, inventory, p) -> new TerminalMenu(containerId, inventory, pos),
                            Component.translatable("container.logtech.terminal")
                    ),
                    buf -> buf.writeBlockPos(pos)
            );
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
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
