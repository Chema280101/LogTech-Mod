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

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class RackBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final io.github.chema280101.logtech.storage.StorageTier tier;

    public RackBlock(Properties properties) {
        this(io.github.chema280101.logtech.storage.StorageTier.HOT, properties);
    }

    public RackBlock(io.github.chema280101.logtech.storage.StorageTier tier, Properties properties) {
        super(properties);
        this.tier = tier != null ? tier : io.github.chema280101.logtech.storage.StorageTier.HOT;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public io.github.chema280101.logtech.storage.StorageTier getTier() {
        return this.tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.tier == io.github.chema280101.logtech.storage.StorageTier.COLD
                ? new ColdRackBlockEntity(pos, state)
                : new RackBlockEntity(pos, state);
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
        if (RackBlockEntity.isDataModule(stack)) {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RackBlockEntity rackBe) {
                String prefix = rackBe.getTier() == io.github.chema280101.logtech.storage.StorageTier.COLD
                        ? "chat.logtech.cold_rack." : "chat.logtech.rack.";
                if (rackBe.getModuleCount() < RackBlockEntity.SLOTS) {
                    if (rackBe.insertModule(stack)) {
                        stack.shrink(1);
                        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                        player.displayClientMessage(
                                Component.translatable(
                                        prefix + "inserted",
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
                                    prefix + "full",
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
            String prefix = rackBe.getTier() == io.github.chema280101.logtech.storage.StorageTier.COLD
                    ? "chat.logtech.cold_rack." : "chat.logtech.rack.";
            if (player.isShiftKeyDown()) {
                ItemStack extracted = rackBe.extractModule();
                if (!extracted.isEmpty()) {
                    if (!player.getInventory().add(extracted)) {
                        player.drop(extracted, false);
                    }
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.displayClientMessage(
                            Component.translatable(
                                    prefix + "extracted",
                                    rackBe.getModuleCount(),
                                    RackBlockEntity.SLOTS,
                                    rackBe.getCapacity()
                                ),
                            true
                    );
                    return InteractionResult.SUCCESS;
                } else {
                    player.displayClientMessage(Component.translatable(prefix + "empty"), true);
                    return InteractionResult.FAIL;
                }
            } else {
                player.displayClientMessage(
                        Component.translatable(
                                prefix + "status",
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
