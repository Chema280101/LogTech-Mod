package io.github.chema280101.logtech.machine;

import io.github.chema280101.logtech.machine.recipe.WashingRecipe;
import io.github.chema280101.logtech.network.EnergyPriority;
import io.github.chema280101.logtech.network.NetworkMember;
import io.github.chema280101.logtech.registry.ModBlockEntities;
import io.github.chema280101.logtech.registry.ModDataComponents;
import io.github.chema280101.logtech.registry.ModItems;
import io.github.chema280101.logtech.registry.ModRecipeTypes;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class WasherBlockEntity extends BlockEntity implements MenuProvider, NetworkMember {
    public static final int ENERGY_CAPACITY = 10_000;
    public static final int ENERGY_MAX_RECEIVE = 100;
    public static final int WATER_CAPACITY = 4_000;
    public static final int DEFAULT_WATER_USAGE = 250;

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT_DUST = 1;
    public static final int SLOT_OUTPUT_SLUDGE = 2;
    public static final int TOTAL_SLOTS = 3;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == SLOT_INPUT) {
                checkRecipeChanged();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_INPUT) {
                return hasRecipe(stack);
            }
            return false;
        }
    };

    private final FluidTank fluidTank = new FluidTank(WATER_CAPACITY, fluidStack -> fluidStack.is(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final MachineEnergyStorage energyStorage = new MachineEnergyStorage(
            ENERGY_CAPACITY,
            ENERGY_MAX_RECEIVE,
            0,
            this::setChanged
    );

    private int progress = 0;
    private int maxProgress = 80;
    private MachineStatus status = MachineStatus.NO_INPUT;
    private EnergyPriority energyPriority = EnergyPriority.NORMAL;
    private boolean energyShed = false;

    private WashingRecipe cachedRecipe = null;
    private Item lastInputItem = Items.AIR;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> WasherBlockEntity.this.progress;
                case 1 -> WasherBlockEntity.this.maxProgress;
                case 2 -> WasherBlockEntity.this.energyStorage.getEnergyStored() & 0xFFFF;
                case 3 -> (WasherBlockEntity.this.energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 4 -> WasherBlockEntity.this.energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 5 -> (WasherBlockEntity.this.energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 6 -> WasherBlockEntity.this.status.ordinal();
                case 7 -> WasherBlockEntity.this.fluidTank.getFluidAmount() & 0xFFFF;
                case 8 -> (WasherBlockEntity.this.fluidTank.getFluidAmount() >> 16) & 0xFFFF;
                case 9 -> WasherBlockEntity.this.fluidTank.getCapacity() & 0xFFFF;
                case 10 -> (WasherBlockEntity.this.fluidTank.getCapacity() >> 16) & 0xFFFF;
                case 11 -> WasherBlockEntity.this.energyPriority.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> WasherBlockEntity.this.progress = value;
                case 1 -> WasherBlockEntity.this.maxProgress = value;
                case 6 -> {
                    if (value >= 0 && value < MachineStatus.values().length) {
                        WasherBlockEntity.this.status = MachineStatus.values()[value];
                    }
                }
                case 11 -> {
                    WasherBlockEntity.this.energyPriority = EnergyPriority.fromOrdinal(value);
                    WasherBlockEntity.this.setChanged();
                }
            }
        }

        @Override
        public int getCount() {
            return 12;
        }
    };

    private final IItemHandler inputHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return TOTAL_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == SLOT_INPUT ? itemHandler.getStackInSlot(SLOT_INPUT) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == SLOT_INPUT && (WasherBlockEntity.this.level == null || hasRecipe(stack))) {
                return itemHandler.insertItem(SLOT_INPUT, stack, simulate);
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
            return slot == SLOT_INPUT && (WasherBlockEntity.this.level == null || hasRecipe(stack));
        }
    };

    private final IItemHandler outputHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return TOTAL_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot == SLOT_OUTPUT_DUST || slot == SLOT_OUTPUT_SLUDGE) {
                return itemHandler.getStackInSlot(slot);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == SLOT_OUTPUT_DUST || slot == SLOT_OUTPUT_SLUDGE) {
                return itemHandler.extractItem(slot, amount, simulate);
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

    public WasherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WASHER.get(), pos, state);
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

    public FluidTank getFluidTank() {
        return this.fluidTank;
    }

    public MachineEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    @Override
    public MachineStatus getStatus() {
        return this.status;
    }

    @Override
    public int requestedEnergy() {
        return Math.max(0, this.energyStorage.getMaxEnergyStored() - this.energyStorage.getEnergyStored());
    }

    @Override
    public int receiveNetworkEnergy(int amount) {
        return this.energyStorage.receiveNetworkEnergy(amount);
    }

    @Override
    public EnergyPriority getEnergyPriority() {
        return this.energyPriority;
    }

    @Override
    public void setEnergyPriority(EnergyPriority priority) {
        this.energyPriority = priority != null ? priority : EnergyPriority.NORMAL;
        setChanged();
    }

    @Override
    public boolean isEnergyShed() {
        return this.energyShed;
    }

    @Override
    public void setEnergyShed(boolean shed) {
        this.energyShed = shed;
    }

    public int getProgress() {
        return this.progress;
    }

    public int getMaxProgress() {
        return this.maxProgress;
    }

    public boolean hasRecipe(ItemStack stack) {
        if (stack.isEmpty() || this.level == null) {
            return false;
        }
        if (stack.has(ModDataComponents.PURITY.get()) && ModDataComponents.getPurity(stack) >= 5) {
            return false;
        }
        if (this.cachedRecipe != null && stack.is(this.lastInputItem)) {
            return true;
        }
        return findRecipe(stack) != null;
    }

    private WashingRecipe findRecipe(ItemStack stack) {
        if (stack.isEmpty() || this.level == null) {
            return null;
        }
        if (stack.has(ModDataComponents.PURITY.get()) && ModDataComponents.getPurity(stack) >= 5) {
            return null;
        }
        return this.level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.WASHING.get(), new SingleRecipeInput(stack), this.level)
                .map(RecipeHolder::value)
                .orElse(null);
    }

    private void checkRecipeChanged() {
        ItemStack currentInput = this.itemHandler.getStackInSlot(SLOT_INPUT);
        Item currentItem = currentInput.isEmpty() ? Items.AIR : currentInput.getItem();
        if (currentItem != this.lastInputItem) {
            this.lastInputItem = currentItem;
            this.cachedRecipe = findRecipe(currentInput);
            this.progress = 0;
            if (this.cachedRecipe != null) {
                this.maxProgress = this.cachedRecipe.ticks();
            }
        }
    }

    private final ActivityCounter activityCounter = new ActivityCounter();

    public ActivityCounter getActivityCounter() {
        return this.activityCounter;
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        ItemStack inputStack = this.itemHandler.getStackInSlot(SLOT_INPUT);
        if (inputStack.isEmpty() || (inputStack.has(ModDataComponents.PURITY.get()) && ModDataComponents.getPurity(inputStack) >= 5)) {
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
            this.cachedRecipe = findRecipe(inputStack);
            if (this.cachedRecipe != null) {
                this.maxProgress = this.cachedRecipe.ticks();
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

        int waterRequired = this.cachedRecipe.fluidAmount() > 0 ? this.cachedRecipe.fluidAmount() : DEFAULT_WATER_USAGE;
        if (this.fluidTank.getFluidAmount() < waterRequired) {
            if (this.status != MachineStatus.NO_FLUID) {
                this.status = MachineStatus.NO_FLUID;
                setChanged();
            }
            this.activityCounter.tick(false);
            return;
        }

        ItemStack currentDust = this.itemHandler.getStackInSlot(SLOT_OUTPUT_DUST);
        ItemStack dustOutput = this.cachedRecipe.assemble(new SingleRecipeInput(inputStack), level.registryAccess());
        boolean canFitDust = currentDust.isEmpty()
                || (ItemStack.isSameItemSameComponents(currentDust, dustOutput)
                && currentDust.getCount() + dustOutput.getCount() <= currentDust.getMaxStackSize());

        ItemStack currentSludge = this.itemHandler.getStackInSlot(SLOT_OUTPUT_SLUDGE);
        ItemStack sludgeOutput = this.cachedRecipe.outputs().size() > 1
                ? this.cachedRecipe.outputs().get(1).createStack()
                : new ItemStack(ModItems.SLUDGE.get(), 1);
        boolean canFitSludge = currentSludge.isEmpty()
                || (ItemStack.isSameItemSameComponents(currentSludge, sludgeOutput)
                && currentSludge.getCount() + sludgeOutput.getCount() <= currentSludge.getMaxStackSize());

        if (!canFitDust || !canFitSludge) {
            if (this.status != MachineStatus.OUTPUT_FULL) {
                this.status = MachineStatus.OUTPUT_FULL;
                setChanged();
            }
            this.activityCounter.tick(false);
            return;
        }

        int energyCost = this.cachedRecipe != null && this.cachedRecipe.getEnergyPerTick() > 0
                ? this.cachedRecipe.getEnergyPerTick()
                : io.github.chema280101.logtech.LogTechConfig.WASHER_ENERGY_CONSUMPTION.get();
        if (this.energyStorage.getEnergyStored() < energyCost) {
            MachineStatus target = this.energyShed ? MachineStatus.SHED : MachineStatus.NO_ENERGY;
            if (this.status != target) {
                this.status = target;
                setChanged();
            }
            this.activityCounter.tick(false);
            return;
        }

        if (this.status != MachineStatus.WORKING) {
            this.status = MachineStatus.WORKING;
            setChanged();
        }

        this.energyStorage.consume(energyCost);
        this.activityCounter.recordEnergyConsumed(energyCost);
        this.progress++;
        this.activityCounter.tick(true);

        if (this.progress >= this.maxProgress) {
            this.itemHandler.extractItem(SLOT_INPUT, 1, false);
            this.fluidTank.drain(waterRequired, IFluidHandler.FluidAction.EXECUTE);

            if (currentDust.isEmpty()) {
                this.itemHandler.setStackInSlot(SLOT_OUTPUT_DUST, dustOutput.copy());
            } else {
                currentDust.grow(dustOutput.getCount());
            }

            if (currentSludge.isEmpty()) {
                this.itemHandler.setStackInSlot(SLOT_OUTPUT_SLUDGE, sludgeOutput.copy());
            } else {
                currentSludge.grow(sludgeOutput.getCount());
            }

            this.activityCounter.recordItemIn(1);
            this.activityCounter.recordItemOut(dustOutput.getCount() + sludgeOutput.getCount());
            this.progress = 0;

            ItemStack remainingInput = this.itemHandler.getStackInSlot(SLOT_INPUT);
            if (remainingInput.isEmpty() || (remainingInput.has(ModDataComponents.PURITY.get()) && ModDataComponents.getPurity(remainingInput) >= 5)) {
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
        return Component.translatable("block.logtech.washer");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.logtech.washer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WasherMenu(containerId, playerInventory, this.worldPosition, this.itemHandler, this.dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("data_version", 2);
        tag.put("Inventory", this.itemHandler.serializeNBT(registries));
        tag.put("Tank", this.fluidTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Energy", this.energyStorage.getEnergyStored());
        tag.putInt("Progress", this.progress);
        tag.putInt("MaxProgress", this.maxProgress);
        tag.putInt("Status", this.status.ordinal());
        tag.putString("energy_priority", this.energyPriority.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            this.itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("Tank")) {
            this.fluidTank.readFromNBT(registries, tag.getCompound("Tank"));
        }
        if (tag.contains("Energy")) {
            this.energyStorage.setEnergy(tag.getInt("Energy"));
        }
        this.progress = tag.getInt("Progress");
        this.maxProgress = tag.contains("MaxProgress") ? tag.getInt("MaxProgress") : 80;
        if (tag.contains("Status")) {
            int ord = tag.getInt("Status");
            if (ord >= 0 && ord < MachineStatus.values().length) {
                this.status = MachineStatus.values()[ord];
            }
        }
        if (tag.contains("energy_priority")) {
            this.energyPriority = EnergyPriority.fromName(tag.getString("energy_priority"));
        } else {
            this.energyPriority = EnergyPriority.NORMAL;
        }
        ItemStack input = this.itemHandler.getStackInSlot(SLOT_INPUT);
        this.lastInputItem = input.isEmpty() ? Items.AIR : input.getItem();
        this.cachedRecipe = null;
    }
}
