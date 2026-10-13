package io.github.chema280101.logtech.block;

import io.github.chema280101.logtech.machine.ActivityCounter;
import io.github.chema280101.logtech.machine.MachineStatus;
import io.github.chema280101.logtech.network.Network;
import io.github.chema280101.logtech.network.NetworkManager;
import io.github.chema280101.logtech.network.NetworkMember;
import io.github.chema280101.logtech.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Entidad de bloque para la Alarma.
 * Implementa NetworkMember para integrarse con la telemetría y las reglas de red.
 */
public class AlarmBlockEntity extends BlockEntity implements NetworkMember {
    private final ActivityCounter activityCounter = new ActivityCounter();

    public AlarmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALARM.get(), pos, state);
    }

    public int getChannel() {
        BlockState state = getBlockState();
        return state.hasProperty(AlarmBlock.CHANNEL) ? state.getValue(AlarmBlock.CHANNEL) : 1;
    }

    public boolean isPowered() {
        BlockState state = getBlockState();
        return state.hasProperty(AlarmBlock.POWERED) && state.getValue(AlarmBlock.POWERED);
    }

    public void setPowered(boolean powered) {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(AlarmBlock.POWERED) && state.getValue(AlarmBlock.POWERED) != powered) {
            level.setBlock(worldPosition, state.setValue(AlarmBlock.POWERED, powered), Block.UPDATE_ALL);
            level.updateNeighborsAt(worldPosition, state.getBlock());
            setChanged();
        }
    }

    public void onChannelChanged(int newChannel) {
        if (level == null || level.isClientSide()) {
            return;
        }
        NetworkManager manager = NetworkManager.get(level);
        if (manager != null) {
            Network net = manager.scan(worldPosition, null);
            if (net.isValid() && !net.getCores().isEmpty()) {
                BlockPos corePos = net.getCores().iterator().next();
                if (level.getBlockEntity(corePos) instanceof CoreBlockEntity core && core.isPowered()) {
                    boolean active = core.getRuleManager().isRedstoneSignalActive(newChannel);
                    setPowered(active);
                    return;
                }
            }
        }
        setPowered(false);
    }

    @Override
    public Component getName() {
        return Component.translatable("block.logtech.alarm");
    }

    @Override
    public MachineStatus getStatus() {
        return isPowered() ? MachineStatus.WORKING : MachineStatus.IDLE;
    }

    @Override
    public ActivityCounter getActivityCounter() {
        return this.activityCounter;
    }
}
