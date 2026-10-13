package io.github.chema280101.logtech.block;

import io.github.chema280101.logtech.network.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Bloque de alarma de la red LogTech.
 * Miembro de red conectado (hoja no conductora) con un canal configurable del 1 al 4.
 * Emite señal de redstone 15 cuando la red activa el canal correspondiente.
 */
public class AlarmBlock extends Block implements EntityBlock {
    public static final IntegerProperty CHANNEL = IntegerProperty.create("channel", 1, 4);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public AlarmBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(CHANNEL, 1)
                .setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHANNEL, POWERED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlarmBlockEntity(pos, state);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(POWERED) ? 15 : 0;
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

        int currentChannel = state.getValue(CHANNEL);
        int nextChannel = currentChannel >= 4 ? 1 : currentChannel + 1;
        BlockState newState = state.setValue(CHANNEL, nextChannel);
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, 1.0f + 0.1f * nextChannel);
        player.displayClientMessage(
                Component.translatable("chat.logtech.alarm.channel_set", nextChannel),
                true
        );

        if (level.getBlockEntity(pos) instanceof AlarmBlockEntity alarmBe) {
            alarmBe.onChannelChanged(nextChannel);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMovedByPiston) {
        super.onPlace(state, level, pos, oldState, isMovedByPiston);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            NetworkManager manager = NetworkManager.get(level);
            if (manager != null) {
                manager.onComponentAdded(pos);
            }
            if (state.getValue(POWERED)) {
                level.updateNeighborsAt(pos, this);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMovedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (state.getValue(POWERED)) {
                level.updateNeighborsAt(pos, this);
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
