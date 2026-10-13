package io.github.chema280101.logtech.machine;

import io.github.chema280101.logtech.network.NetworkManager;
import io.github.chema280101.logtech.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

public class WasherBlock extends Block implements EntityBlock {
    public WasherBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WasherBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return blockEntityType == ModBlockEntities.WASHER.get()
                ? (lvl, pos, st, be) -> ((WasherBlockEntity) be).serverTick(lvl, pos, st)
                : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof WasherBlockEntity washerBe) {
            if (FluidUtil.interactWithFluidHandler(player, hand, washerBe.getFluidTank())) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof WasherBlockEntity washerBe) {
                serverPlayer.openMenu(washerBe, pos);
                return InteractionResult.CONSUME;
            }
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
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof WasherBlockEntity washerBe) {
                washerBe.dropContents(level, pos);
            }
            super.onRemove(state, level, pos, newState, isMovedByPiston);
            if (!level.isClientSide()) {
                NetworkManager manager = NetworkManager.get(level);
                if (manager != null) {
                    manager.onComponentRemoved(pos);
                }
            }
        } else {
            super.onRemove(state, level, pos, newState, isMovedByPiston);
        }
    }
}
