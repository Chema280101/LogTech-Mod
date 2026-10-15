package io.github.chema280101.logtech.machine;

import io.github.chema280101.logtech.network.NetworkManager;
import io.github.chema280101.logtech.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class DrillBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public DrillBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DrillBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return blockEntityType == ModBlockEntities.DRILL.get()
                ? (lvl, pos, st, be) -> ((DrillBlockEntity) be).serverTick(lvl, pos, st)
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DrillBlockEntity drillBe) {
            if (player.isShiftKeyDown()) {
                if (!io.github.chema280101.logtech.security.NetworkSecurity.canPlayerDo(player, io.github.chema280101.logtech.security.PlayerAction.CHANGE_PRIORITY)) {
                    return InteractionResult.CONSUME;
                }
                io.github.chema280101.logtech.network.EnergyPriority next = drillBe.getEnergyPriority().next();
                drillBe.setEnergyPriority(next);
                player.displayClientMessage(
                        Component.translatable("chat.logtech.drill.priority_changed", Component.translatable(next.getTranslationKey())),
                        true
                );
                return InteractionResult.CONSUME;
            }

            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                if (io.github.chema280101.logtech.security.NetworkSecurity.canPlayerDo(player, io.github.chema280101.logtech.security.PlayerAction.INTERACT_MACHINE)) {
                    serverPlayer.openMenu(drillBe, pos);
                    return InteractionResult.CONSUME;
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMovedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMovedByPiston);
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HeatDissipatingMachine machine) {
                machine.recalculateCooling();
            }
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, net.minecraft.world.level.LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (level instanceof Level lvl && !lvl.isClientSide()) {
            BlockEntity be = lvl.getBlockEntity(currentPos);
            if (be instanceof HeatDissipatingMachine machine) {
                machine.recalculateCooling();
            }
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMovedByPiston) {
        super.onPlace(state, level, pos, oldState, isMovedByPiston);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            NetworkManager manager = NetworkManager.get(level);
            if (manager != null) {
                manager.onComponentAdded(pos);
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HeatDissipatingMachine machine) {
                machine.recalculateCooling();
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMovedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DrillBlockEntity drillBe) {
                drillBe.dropContents(level, pos);
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
