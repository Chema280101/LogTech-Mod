package io.github.chema280101.logtech.block;

import io.github.chema280101.logtech.network.Network;
import io.github.chema280101.logtech.network.NetworkManager;
import io.github.chema280101.logtech.registry.ModBlocks;
import io.github.chema280101.logtech.storage.ItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Item handler de solo inserción para el bloque Backbone.
 * Permite que tolvas, tuberías u otras máquinas inserten ítems directamente en el Núcleo
 * conectado a la red, respetando la capacidad, energía y estado de sobrecarga.
 */
public class BackboneItemHandler implements IItemHandler {
    private final Level level;
    private final BlockPos pos;

    public BackboneItemHandler(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != 0 || stack == null || stack.isEmpty()) {
            return stack == null ? ItemStack.EMPTY : stack;
        }

        if (level == null || level.isClientSide() || !level.hasChunkAt(pos)) {
            return stack;
        }

        if (!level.getBlockState(pos).is(ModBlocks.BACKBONE.get())) {
            return stack;
        }

        NetworkManager manager = NetworkManager.get(level);
        if (manager == null) {
            return stack;
        }

        Network network = manager.scan(pos, null);
        if (!network.isValid() || network.getCores().isEmpty()) {
            return stack;
        }

        BlockPos corePos = network.getCores().iterator().next();
        if (!level.hasChunkAt(corePos)) {
            return stack;
        }

        BlockEntity be = level.getBlockEntity(corePos);
        if (!(be instanceof CoreBlockEntity coreBe)) {
            return stack;
        }

        if (!coreBe.isPowered() || coreBe.isOverloaded()) {
            return stack;
        }

        ItemKey key = ItemKey.of(stack);
        long inserted = coreBe.insert(key, stack.getCount(), simulate);
        if (inserted <= 0) {
            return stack;
        }

        if (inserted >= stack.getCount()) {
            return ItemStack.EMPTY;
        }

        return stack.copyWithCount((int) (stack.getCount() - inserted));
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 && stack != null && !stack.isEmpty();
    }
}
