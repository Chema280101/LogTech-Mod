package io.github.chema280101.logtech.block;

import io.github.chema280101.logtech.registry.ModBlockEntities;
import io.github.chema280101.logtech.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class RackBlockEntity extends BlockEntity {
    public static final int SLOTS = 4;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.DATA_MODULE.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public RackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RACK.get(), pos, state);
    }

    public int getModuleCount() {
        int count = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public long getCapacity() {
        return (long) getModuleCount() * io.github.chema280101.logtech.LogTechConfig.DATA_MODULE_CAPACITY.get();
    }

    public boolean insertModule(ItemStack stack) {
        if (!stack.is(ModItems.DATA_MODULE.get())) {
            return false;
        }
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                ItemStack singleModule = stack.copyWithCount(1);
                inventory.setStackInSlot(i, singleModule);
                setChanged();
                return true;
            }
        }
        return false;
    }

    public ItemStack extractModule() {
        for (int i = inventory.getSlots() - 1; i >= 0; i--) {
            ItemStack inSlot = inventory.getStackInSlot(i);
            if (!inSlot.isEmpty()) {
                inventory.setStackInSlot(i, ItemStack.EMPTY);
                setChanged();
                return inSlot;
            }
        }
        return ItemStack.EMPTY;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
    }
}
