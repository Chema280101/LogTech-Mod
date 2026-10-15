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

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Entidad de bloque para la Alarma.
 * Implementa NetworkMember para integrarse con la telemetría y las reglas de red,
 * y Configurable para permitir copiar y pegar su canal asignado.
 */
public class AlarmBlockEntity extends BlockEntity implements NetworkMember, Configurable {
    private final ActivityCounter activityCounter = new ActivityCounter();

    public AlarmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALARM.get(), pos, state);
    }

    @Override
    public String getConfigType() {
        return ConfigTypes.ALARM;
    }

    @Override
    public CompoundTag exportConfig(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("channel", getChannel());
        return tag;
    }

    @Override
    public boolean importConfig(CompoundTag tag, HolderLookup.Provider registries, @Nullable Player player) {
        if (tag == null || !tag.contains("channel")) {
            return false;
        }
        int channel = tag.getInt("channel");
        if (channel < 1 || channel > 4) {
            return false;
        }
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.hasProperty(AlarmBlock.CHANNEL)) {
                level.setBlock(worldPosition, state.setValue(AlarmBlock.CHANNEL, channel), Block.UPDATE_ALL);
                level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.LEVER_CLICK, net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 1.0f + 0.1f * channel);
                onChannelChanged(channel);
                setChanged();
            }
        }
        return true;
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
