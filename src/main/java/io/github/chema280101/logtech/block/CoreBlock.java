package io.github.chema280101.logtech.block;

import io.github.chema280101.logtech.network.NetworkMember;
import io.github.chema280101.logtech.storage.ItemKey;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class CoreBlock extends Block implements EntityBlock {
    public CoreBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide()) {
            return null;
        }
        return blockEntityType == io.github.chema280101.logtech.registry.ModBlockEntities.CORE.get()
                ? (lvl, p, st, be) -> ((CoreBlockEntity) be).serverTick(lvl, p, st)
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
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CoreBlockEntity coreBe) {
            ItemKey key = ItemKey.of(stack);
            int count = stack.getCount();
            long inserted = coreBe.insert(key, count, false);
            if (inserted > 0) {
                stack.shrink((int) inserted);
                player.displayClientMessage(
                        Component.translatable(
                                "chat.logtech.core.inserted",
                                inserted,
                                key.toStack(1).getHoverName(),
                                coreBe.getAmount(key)
                        ),
                        true
                );
                return ItemInteractionResult.SUCCESS;
            } else {
                if (!coreBe.getOrRecalculateNetwork().isValid()) {
                    player.displayClientMessage(Component.translatable("chat.logtech.core.cannot_insert_invalid"), true);
                } else if (!coreBe.isPowered()) {
                    player.displayClientMessage(Component.translatable("chat.logtech.core.cannot_insert_no_energy"), true);
                } else if (coreBe.isOverloaded()) {
                    player.displayClientMessage(Component.translatable("chat.logtech.core.cannot_insert_overloaded"), true);
                } else if (coreBe.getCapacity() == 0) {
                    player.displayClientMessage(Component.translatable("chat.logtech.core.cannot_insert_no_capacity"), true);
                } else {
                    player.displayClientMessage(Component.translatable("chat.logtech.core.cannot_insert_full"), true);
                }
                return ItemInteractionResult.FAIL;
            }
        }

        return ItemInteractionResult.FAIL;
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

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    new net.minecraft.world.SimpleMenuProvider(
                            (containerId, inventory, p) -> new CoreMenu(containerId, inventory, pos),
                            Component.translatable("container.logtech.core")
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
            io.github.chema280101.logtech.network.NetworkManager manager = io.github.chema280101.logtech.network.NetworkManager.get(level);
            if (manager != null) {
                manager.onComponentAdded(pos);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMovedByPiston) {
        super.onRemove(state, level, pos, newState, isMovedByPiston);
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            io.github.chema280101.logtech.network.NetworkManager manager = io.github.chema280101.logtech.network.NetworkManager.get(level);
            if (manager != null) {
                manager.onComponentRemoved(pos);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMovedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMovedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CoreBlockEntity coreBe) {
            if (!level.isClientSide && player.isCreative() && !coreBe.isEmpty()) {
                ItemStack stack = new ItemStack(this);
                stack.applyComponents(coreBe.collectComponents());
                ItemEntity itemEntity = new ItemEntity(
                        level,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        stack
                );
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
