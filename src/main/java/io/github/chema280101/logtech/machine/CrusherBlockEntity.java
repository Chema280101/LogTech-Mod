package io.github.chema280101.logtech.machine;

import io.github.chema280101.logtech.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import io.github.chema280101.logtech.network.NetworkMember;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class CrusherBlockEntity extends BlockEntity implements MenuProvider, NetworkMember {
    public static final int ENERGY_CAPACITY = 10_000;
    public static final int ENERGY_MAX_RECEIVE = 100;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == 0) {
                checkRecipeChanged();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                return CrusherRecipes.hasRecipe(stack);
            }
            return false;
        }
    };

    private final MachineEnergyStorage energyStorage = new MachineEnergyStorage(
            ENERGY_CAPACITY,
            ENERGY_MAX_RECEIVE,
            0,
            this::setChanged
    );

    private int progress = 0;
    private int maxProgress = 100;
    private MachineStatus status = MachineStatus.NO_INPUT;

    private CrusherRecipes.Recipe cachedRecipe = null;
    private Item lastInputItem = Items.AIR;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> CrusherBlockEntity.this.progress;
                case 1 -> CrusherBlockEntity.this.maxProgress;
                case 2 -> CrusherBlockEntity.this.energyStorage.getEnergyStored() & 0xFFFF;
                case 3 -> (CrusherBlockEntity.this.energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 4 -> CrusherBlockEntity.this.energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 5 -> (CrusherBlockEntity.this.energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 6 -> CrusherBlockEntity.this.status.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> CrusherBlockEntity.this.progress = value;
                case 1 -> CrusherBlockEntity.this.maxProgress = value;
                case 6 -> {
                    if (value >= 0 && value < MachineStatus.values().length) {
                        CrusherBlockEntity.this.status = MachineStatus.values()[value];
                    }
                }
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    // Sided wrappers for automation
    private final IItemHandler inputHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? itemHandler.getStackInSlot(0) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == 0 && CrusherRecipes.hasRecipe(stack)) {
                return itemHandler.insertItem(0, stack, simulate);
            }
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && CrusherRecipes.hasRecipe(stack);
        }
    };

    private final IItemHandler outputHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 1 ? itemHandler.getStackInSlot(1) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 1) {
                return itemHandler.extractItem(1, amount, simulate);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSHER.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == Direction.DOWN) {
            return this.outputHandler;
        }
        if (side != null) {
            return this.inputHandler;
        }
        return this.itemHandler;
    }

    public MachineEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    public MachineStatus getStatus() {
        return this.status;
    }

    private void checkRecipeChanged() {
        ItemStack currentInput = this.itemHandler.getStackInSlot(0);
        Item currentItem = currentInput.isEmpty() ? Items.AIR : currentInput.getItem();
        if (currentItem != this.lastInputItem) {
            this.lastInputItem = currentItem;
            this.cachedRecipe = CrusherRecipes.getRecipe(currentInput).orElse(null);
            this.progress = 0;
            if (this.cachedRecipe != null) {
                this.maxProgress = this.cachedRecipe.processingTime();
            }
        }
    }

    private final ActivityCounter activityCounter = new ActivityCounter();

    public ActivityCounter getActivityCounter() {
        return this.activityCounter;
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        ItemStack inputStack = this.itemHandler.getStackInSlot(0);
        if (inputStack.isEmpty()) {
            if (this.status != MachineStatus.NO_INPUT) {
                this.status = MachineStatus.NO_INPUT;
                this.progress = 0;
                setChanged();
            }
            this.activityCounter.tick(false);
            return;
        }

        if (this.cachedRecipe == null || this.lastInputItem != inputStack.getItem()) {
            this.lastInputItem = inputStack.getItem();
            this.cachedRecipe = CrusherRecipes.getRecipe(inputStack).orElse(null);
            if (this.cachedRecipe != null) {
                this.maxProgress = this.cachedRecipe.processingTime();
            }
        }

        if (this.cachedRecipe == null) {
            if (this.status != MachineStatus.NO_INPUT) {
                this.status = MachineStatus.NO_INPUT;
                this.progress = 0;
                setChanged();
            }
            this.activityCounter.tick(false);
            return;
        }

        ItemStack currentOutput = this.itemHandler.getStackInSlot(1);
        ItemStack recipeOutput = this.cachedRecipe.output();

        boolean canFitOutput = currentOutput.isEmpty()
                || (ItemStack.isSameItemSameComponents(currentOutput, recipeOutput)
                && currentOutput.getCount() + recipeOutput.getCount() <= currentOutput.getMaxStackSize());

        if (!canFitOutput) {
            if (this.status != MachineStatus.OUTPUT_FULL) {
                this.status = MachineStatus.OUTPUT_FULL;
                setChanged();
            }
            this.activityCounter.tick(false);
            return;
        }

        int energyCost = io.github.chema280101.logtech.LogTechConfig.CRUSHER_ENERGY_CONSUMPTION.get();
        if (this.energyStorage.getEnergyStored() < energyCost) {
            if (this.status != MachineStatus.NO_ENERGY) {
                this.status = MachineStatus.NO_ENERGY;
                setChanged();
            }
            this.activityCounter.tick(false);
            return;
        }

        // Si tenemos energía, espacio y receta: TRABAJANDO
        if (this.status != MachineStatus.WORKING) {
            this.status = MachineStatus.WORKING;
            setChanged();
        }

        this.energyStorage.consume(energyCost);
        this.progress++;
        this.activityCounter.tick(true);

        if (this.progress >= this.maxProgress) {
            this.itemHandler.extractItem(0, 1, false);
            this.activityCounter.recordItemIn(1);
            if (currentOutput.isEmpty()) {
                this.itemHandler.setStackInSlot(1, recipeOutput.copy());
            } else {
                currentOutput.grow(recipeOutput.getCount());
            }
            this.activityCounter.recordItemOut(recipeOutput.getCount());
            this.progress = 0;

            ItemStack remainingInput = this.itemHandler.getStackInSlot(0);
            if (remainingInput.isEmpty()) {
                this.cachedRecipe = null;
                this.lastInputItem = Items.AIR;
                this.status = MachineStatus.NO_INPUT;
            }
        }

        setChanged();
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < this.itemHandler.getSlots(); i++) {
            ItemStack stack = this.itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    @Override
    public Component getName() {
        return Component.translatable("block.logtech.crusher");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.logtech.crusher");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CrusherMenu(containerId, playerInventory, this.worldPosition, this.itemHandler, this.dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", this.itemHandler.serializeNBT(registries));
        tag.putInt("Energy", this.energyStorage.getEnergyStored());
        tag.putInt("Progress", this.progress);
        tag.putInt("MaxProgress", this.maxProgress);
        tag.putInt("Status", this.status.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            this.itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("Energy")) {
            this.energyStorage.setEnergy(tag.getInt("Energy"));
        }
        this.progress = tag.getInt("Progress");
        this.maxProgress = tag.contains("MaxProgress") ? tag.getInt("MaxProgress") : 100;
        if (tag.contains("Status")) {
            int ord = tag.getInt("Status");
            if (ord >= 0 && ord < MachineStatus.values().length) {
                this.status = MachineStatus.values()[ord];
            }
        }
        ItemStack input = this.itemHandler.getStackInSlot(0);
        this.lastInputItem = input.isEmpty() ? Items.AIR : input.getItem();
        this.cachedRecipe = CrusherRecipes.getRecipe(input).orElse(null);
    }
}
