package io.github.chema280101.logtech.block;

import io.github.chema280101.logtech.network.Network;
import io.github.chema280101.logtech.network.NetworkManager;
import io.github.chema280101.logtech.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Energy storage de solo entrada para el bloque Backbone.
 * Permite que cualquier generador, cable o productor externo conectado a cualquier Backbone
 * inyecte energía directamente al Núcleo de la red.
 *
 * Invariantes de diseño:
 * - Solo entrada (canExtract() == false, extractEnergy() == 0).
 * - La energía viaja desde los generadores a través del Backbone hacia el Núcleo.
 * - Evita cualquier bucle impidiendo que las máquinas u otros bloques extraigan energía del Backbone.
 */
public class BackboneEnergyStorage implements IEnergyStorage {
    private final Level level;
    private final BlockPos pos;

    public BackboneEnergyStorage(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    private CoreBlockEntity findConnectedCore() {
        if (this.level == null || this.level.isClientSide() || !this.level.hasChunkAt(this.pos)) {
            return null;
        }

        if (!this.level.getBlockState(this.pos).is(ModBlocks.BACKBONE.get())) {
            return null;
        }

        NetworkManager manager = NetworkManager.get(this.level);
        if (manager == null) {
            return null;
        }

        Network network = manager.scan(this.pos, null);
        if (!network.isValid() || network.getCores().isEmpty()) {
            return null;
        }

        BlockPos corePos = network.getCores().iterator().next();
        if (!this.level.hasChunkAt(corePos)) {
            return null;
        }

        BlockEntity be = this.level.getBlockEntity(corePos);
        if (be instanceof CoreBlockEntity coreBe && !be.isRemoved()) {
            return coreBe;
        }

        return null;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (maxReceive <= 0) {
            return 0;
        }

        CoreBlockEntity core = findConnectedCore();
        if (core == null) {
            return 0;
        }

        return core.getEnergyStorage().receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        // Solo entrada: nunca se extrae energía del Backbone
        return 0;
    }

    @Override
    public int getEnergyStored() {
        CoreBlockEntity core = findConnectedCore();
        return core != null ? core.getEnergyStored() : 0;
    }

    @Override
    public int getMaxEnergyStored() {
        CoreBlockEntity core = findConnectedCore();
        return core != null ? core.getMaxEnergyStored() : 0;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        CoreBlockEntity core = findConnectedCore();
        return core != null && core.getEnergyStorage().canReceive();
    }
}
