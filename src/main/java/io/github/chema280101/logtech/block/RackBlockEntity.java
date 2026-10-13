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

    public static boolean isDataModule(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.is(ModItems.DATA_MODULE.get())
                || stack.is(ModItems.DATA_MODULE_ADVANCED.get())
                || stack.is(ModItems.DATA_MODULE_MASTER.get());
    }

    public static int getModuleMultiplier(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        if (stack.is(ModItems.DATA_MODULE_MASTER.get())) {
            try {
                return io.github.chema280101.logtech.LogTechConfig.DATA_MODULE_MASTER_MULTIPLIER.get();
            } catch (Exception e) {
                return io.github.chema280101.logtech.storage.DataModuleLogic.DEFAULT_MASTER_MULTIPLIER;
            }
        }
        if (stack.is(ModItems.DATA_MODULE_ADVANCED.get())) {
            try {
                return io.github.chema280101.logtech.LogTechConfig.DATA_MODULE_ADVANCED_MULTIPLIER.get();
            } catch (Exception e) {
                return io.github.chema280101.logtech.storage.DataModuleLogic.DEFAULT_ADVANCED_MULTIPLIER;
            }
        }
        if (stack.is(ModItems.DATA_MODULE.get())) {
            return 1;
        }
        return 0;
    }

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isDataModule(stack);
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

    private final io.github.chema280101.logtech.storage.StorageTier tier;

    public RackBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.RACK.get(), pos, state, io.github.chema280101.logtech.storage.StorageTier.HOT);
    }

    public RackBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state, io.github.chema280101.logtech.storage.StorageTier tier) {
        super(type, pos, state);
        this.tier = tier != null ? tier : io.github.chema280101.logtech.storage.StorageTier.HOT;
    }

    public io.github.chema280101.logtech.storage.StorageTier getTier() {
        return this.tier;
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

    public long getCapacityPerModule() {
        try {
            return this.tier == io.github.chema280101.logtech.storage.StorageTier.COLD
                    ? (long) io.github.chema280101.logtech.LogTechConfig.COLD_DATA_MODULE_CAPACITY.get()
                    : (long) io.github.chema280101.logtech.LogTechConfig.DATA_MODULE_CAPACITY.get();
        } catch (Exception e) {
            return this.tier == io.github.chema280101.logtech.storage.StorageTier.COLD ? 16384L : 4096L;
        }
    }

    public long getCapacity() {
        long baseCap = getCapacityPerModule();
        long total = 0L;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                total += baseCap * getModuleMultiplier(stack);
            }
        }
        return total;
    }

    public boolean insertModule(ItemStack stack) {
        if (!isDataModule(stack)) {
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
