package io.github.chema280101.logtech.block;

import io.github.chema280101.logtech.network.NetworkManager;
import io.github.chema280101.logtech.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class RackBlock extends Block implements EntityBlock {
    public RackBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RackBlockEntity(pos, state);
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
        if (stack.is(ModItems.DATA_MODULE.get())) {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RackBlockEntity rackBe) {
                if (rackBe.getModuleCount() < RackBlockEntity.SLOTS) {
                    if (rackBe.insertModule(stack)) {
                        stack.shrink(1);
                        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                        player.displayClientMessage(
                                Component.translatable(
                                        "chat.logtech.rack.inserted",
                                        rackBe.getModuleCount(),
                                        RackBlockEntity.SLOTS,
                                        rackBe.getCapacity()
                                ),
                                true
                        );
                        return ItemInteractionResult.SUCCESS;
                    }
                } else {
                    player.displayClientMessage(
                            Component.translatable(
                                    "chat.logtech.rack.full",
                                    RackBlockEntity.SLOTS,
                                    RackBlockEntity.SLOTS
                            ),
                            true
                    );
                    return ItemInteractionResult.FAIL;
                }
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RackBlockEntity rackBe) {
            if (player.isShiftKeyDown()) {
                ItemStack extracted = rackBe.extractModule();
                if (!extracted.isEmpty()) {
                    if (!player.getInventory().add(extracted)) {
                        player.drop(extracted, false);
                    }
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.displayClientMessage(
                            Component.translatable(
                                    "chat.logtech.rack.extracted",
                                    rackBe.getModuleCount(),
                                    RackBlockEntity.SLOTS,
                                    rackBe.getCapacity()
                                ),
                            true
                    );
                    return InteractionResult.SUCCESS;
                } else {
                    player.displayClientMessage(Component.translatable("chat.logtech.rack.empty"), true);
                    return InteractionResult.FAIL;
                }
            } else {
                player.displayClientMessage(
                        Component.translatable(
                                "chat.logtech.rack.status",
                                rackBe.getModuleCount(),
                                RackBlockEntity.SLOTS,
                                rackBe.getCapacity()
                        ),
                        true
                );
                return InteractionResult.SUCCESS;
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
            if (be instanceof RackBlockEntity rackBe) {
                rackBe.dropContents(level, pos);
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

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMovedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMovedByPiston);
    }
}
