package io.github.chema280101.logtech.machine;

import io.github.chema280101.logtech.machine.recipe.CrushingRecipe;
import io.github.chema280101.logtech.registry.ModBlockEntities;
import io.github.chema280101.logtech.registry.ModDataComponents;
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
import io.github.chema280101.logtech.network.EnergyPriority;
import io.github.chema280101.logtech.network.NetworkMember;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import io.github.chema280101.logtech.registry.ModBlocks;
import io.github.chema280101.logtech.LogTechConfig;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public class CrusherBlockEntity extends BlockEntity implements MenuProvider, NetworkMember, HeatDissipatingMachine, MachineConfigurable {
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
                return hasRecipe(stack);
            }
            return false;
        }
    };

    private final ItemStackHandler firmwareHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof io.github.chema280101.logtech.item.FirmwareItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private final MachineEnergyStorage energyStorage = new MachineEnergyStorage(
            ENERGY_CAPACITY,
            ENERGY_MAX_RECEIVE,
            0,
            this::setChanged
    );

    private int progress = 0;
    private double progressAccumulator = 0.0;
    private int maxProgress = 100;
    private MachineStatus status = MachineStatus.NO_INPUT;
    private EnergyPriority energyPriority = EnergyPriority.NORMAL;
    private boolean energyShed = false;
    private final HeatModel heatModel = new HeatModel();
    private double cachedCooling = -1.0;
    private int cachedHeatsinks = 0;
    private int cachedWaterSides = 0;

    private CrushingRecipe cachedRecipe = null;
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
                case 7 -> CrusherBlockEntity.this.energyPriority.ordinal();
                case 8 -> (int) Math.round(CrusherBlockEntity.this.heatModel.getHeat());
                case 9 -> (int) Math.round(CrusherBlockEntity.this.getCachedCooling() * 10.0);
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
                case 7 -> {
                    CrusherBlockEntity.this.energyPriority = EnergyPriority.fromOrdinal(value);
                    CrusherBlockEntity.this.setChanged();
                }
                case 8 -> CrusherBlockEntity.this.heatModel.setHeat(value);
            }
        }

        @Override
        public int getCount() {
            return 10;
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
            if (slot == 0 && (CrusherBlockEntity.this.level == null || hasRecipe(stack))) {
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
            return slot == 0 && (CrusherBlockEntity.this.level == null || hasRecipe(stack));
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

    @Override
    public MachineStatus getStatus() {
        return this.status;
    }

    public ItemStackHandler getFirmwareHandler() {
        return this.firmwareHandler;
    }

    @Override
    public io.github.chema280101.logtech.machine.firmware.FirmwareType getFirmwareType() {
        ItemStack stack = this.firmwareHandler.getStackInSlot(0);
        if (stack.getItem() instanceof io.github.chema280101.logtech.item.FirmwareItem fw) {
            return fw.getFirmwareType();
        }
        return io.github.chema280101.logtech.machine.firmware.FirmwareType.NONE;
    }

    @Override
    public int requestedEnergy() {
        if (this.heatModel.isOverheated()) {
            return 0;
        }
        int deficit = Math.max(0, this.energyStorage.getMaxEnergyStored() - this.energyStorage.getEnergyStored());
        return io.github.chema280101.logtech.machine.firmware.FirmwareEffects.calculateRequestedEnergy(
                deficit,
                this.heatModel.getSpeedMultiplier(),
                getFirmwareType(),
                LogTechConfig.FIRMWARE_ECO_ENERGY.get(),
                LogTechConfig.FIRMWARE_TURBO_ENERGY.get()
        );
    }

    @Override
    public double getHeat() {
        return this.heatModel.getHeat();
    }

    @Override
    public HeatModel getHeatModel() {
        return this.heatModel;
    }

    @Override
    public void recalculateCooling() {
        if (this.level == null) {
            return;
        }
        int heatsinks = 0;
        int water = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = this.worldPosition.relative(dir);
            BlockState neighborState = this.level.getBlockState(neighborPos);
            if (neighborState.is(ModBlocks.HEATSINK.get())) {
                heatsinks++;
            } else {
                FluidState fluid = this.level.getFluidState(neighborPos);
                if (fluid.is(FluidTags.WATER) || fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER)) {
                    water++;
                }
            }
        }
        this.cachedHeatsinks = heatsinks;
        this.cachedWaterSides = water;
        double baseCooling = LogTechConfig.HEAT_COOLING_RATE.get();
        double sinkRate = LogTechConfig.HEAT_HEATSINK_COOLING.get();
        double waterRate = LogTechConfig.HEAT_WATER_COOLING.get();
        this.cachedCooling = DissipationLogic.calculateCooling(baseCooling, heatsinks, water, sinkRate, waterRate);
        setChanged();
    }

    @Override
    public double getCachedCooling() {
        if (this.cachedCooling < 0.0) {
            if (this.level != null) {
                recalculateCooling();
            } else {
                return LogTechConfig.HEAT_COOLING_RATE.get();
            }
        }
        return this.cachedCooling;
    }

    @Override
    public int getCachedHeatsinks() {
        return this.cachedHeatsinks;
    }

    @Override
    public int getCachedWaterSides() {
        return this.cachedWaterSides;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            recalculateCooling();
        }
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
        if (this.cachedRecipe != null && stack.is(this.lastInputItem)) {
            return true;
        }
        return findRecipe(stack) != null;
    }

    private CrushingRecipe findRecipe(ItemStack stack) {
        if (stack.isEmpty() || this.level == null) {
            return null;
        }
        return this.level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.CRUSHING.get(), new SingleRecipeInput(stack), this.level)
                .map(RecipeHolder::value)
                .orElse(null);
    }

    private void checkRecipeChanged() {
        ItemStack currentInput = this.itemHandler.getStackInSlot(0);
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
        io.github.chema280101.logtech.machine.firmware.FirmwareType fw = getFirmwareType();

        if (level.getGameTime() % 20 == 0) {
            double baseGen = io.github.chema280101.logtech.LogTechConfig.HEAT_BASE_GENERATION.get();
            double cooling = getCachedCooling();

            double normalSlowdown = io.github.chema280101.logtech.LogTechConfig.HEAT_SLOWDOWN_THRESHOLD.get();
            double normalOverheat = io.github.chema280101.logtech.LogTechConfig.HEAT_OVERHEAT_THRESHOLD.get();
            double normalResume = io.github.chema280101.logtech.LogTechConfig.HEAT_RESUME_THRESHOLD.get();
            double normalSlowdownSpeed = io.github.chema280101.logtech.LogTechConfig.HEAT_SLOWDOWN_SPEED.get();

            double safeOverheat = io.github.chema280101.logtech.LogTechConfig.FIRMWARE_SAFE_OVERHEAT_THRESHOLD.get();
            double safeResume = io.github.chema280101.logtech.LogTechConfig.FIRMWARE_SAFE_RESUME_THRESHOLD.get();

            this.heatModel.setSlowdownThreshold(io.github.chema280101.logtech.machine.firmware.FirmwareEffects.getSlowdownThreshold(fw, normalSlowdown, safeOverheat));
            this.heatModel.setOverheatThreshold(io.github.chema280101.logtech.machine.firmware.FirmwareEffects.getOverheatThreshold(fw, normalOverheat, safeOverheat));
            this.heatModel.setResumeThreshold(io.github.chema280101.logtech.machine.firmware.FirmwareEffects.getResumeThreshold(fw, normalResume, safeResume));
            this.heatModel.setSlowdownSpeed(io.github.chema280101.logtech.machine.firmware.FirmwareEffects.getSlowdownSpeed(fw, normalSlowdownSpeed));

            double ecoHeat = io.github.chema280101.logtech.LogTechConfig.FIRMWARE_ECO_HEAT.get();
            double turboHeat = io.github.chema280101.logtech.LogTechConfig.FIRMWARE_TURBO_HEAT.get();
            double heatMult = io.github.chema280101.logtech.machine.firmware.FirmwareEffects.getHeatMultiplier(fw, ecoHeat, turboHeat);

            boolean isWorking = (this.status == MachineStatus.WORKING);
            this.heatModel.tick(isWorking, baseGen, heatMult, cooling);
            setChanged();
        }

        if (this.heatModel.isOverheated()) {
            if (this.status != MachineStatus.OVERHEATED) {
                this.status = MachineStatus.OVERHEATED;
                setChanged();
            }
            this.activityCounter.tick(false);
            return;
        } else if (this.status == MachineStatus.OVERHEATED) {
            this.status = MachineStatus.IDLE;
            setChanged();
        }

        ItemStack inputStack = this.itemHandler.getStackInSlot(0);
        if (inputStack.isEmpty()) {
            if (this.status != MachineStatus.NO_INPUT) {
                this.status = MachineStatus.NO_INPUT;
                this.progress = 0;
                this.progressAccumulator = 0.0;
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
                this.progressAccumulator = 0.0;
                setChanged();
            }
            this.activityCounter.tick(false);
            return;
        }

        ItemStack currentOutput = this.itemHandler.getStackInSlot(1);
        ItemStack recipeOutput = this.cachedRecipe.assemble(new SingleRecipeInput(inputStack), level.registryAccess());

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

        int baseEnergyCost = this.cachedRecipe.getEnergyPerTick();
        int energyCost = io.github.chema280101.logtech.machine.firmware.FirmwareEffects.calculateEnergyCost(
                baseEnergyCost,
                fw,
                io.github.chema280101.logtech.LogTechConfig.FIRMWARE_ECO_ENERGY.get(),
                io.github.chema280101.logtech.LogTechConfig.FIRMWARE_TURBO_ENERGY.get()
        );

        if (this.energyStorage.getEnergyStored() < energyCost) {
            MachineStatus target = this.energyShed ? MachineStatus.SHED : MachineStatus.NO_ENERGY;
            if (this.status != target) {
                this.status = target;
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

        double effectiveSpeed = io.github.chema280101.logtech.machine.firmware.FirmwareEffects.calculateEffectiveSpeed(
                fw,
                this.heatModel.getSpeedMultiplier(),
                io.github.chema280101.logtech.LogTechConfig.FIRMWARE_ECO_SPEED.get(),
                io.github.chema280101.logtech.LogTechConfig.FIRMWARE_TURBO_SPEED.get()
        );

        this.energyStorage.consume(energyCost);
        this.activityCounter.recordEnergyConsumed(energyCost);
        this.progressAccumulator += effectiveSpeed;
        int steps = (int) this.progressAccumulator;
        if (steps > 0) {
            this.progress += steps;
            this.progressAccumulator -= steps;
        }
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
            this.progressAccumulator = 0.0;

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
        ItemStack fwStack = this.firmwareHandler.getStackInSlot(0);
        if (!fwStack.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), fwStack);
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
        return new CrusherMenu(containerId, playerInventory, this.worldPosition, this.itemHandler, this.firmwareHandler, this.dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("data_version", 4);
        tag.put("Inventory", this.itemHandler.serializeNBT(registries));
        tag.put("Firmware", this.firmwareHandler.serializeNBT(registries));
        tag.putInt("Energy", this.energyStorage.getEnergyStored());
        tag.putInt("Progress", this.progress);
        tag.putDouble("ProgressAccumulator", this.progressAccumulator);
        tag.putInt("MaxProgress", this.maxProgress);
        tag.putInt("Status", this.status.ordinal());
        tag.putString("energy_priority", this.energyPriority.name());
        tag.putDouble("Heat", this.heatModel.getHeat());
        tag.putBoolean("Overheated", this.heatModel.isOverheated());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            this.itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("Firmware")) {
            this.firmwareHandler.deserializeNBT(registries, tag.getCompound("Firmware"));
        } else {
            this.firmwareHandler.setStackInSlot(0, ItemStack.EMPTY);
        }
        if (tag.contains("Energy")) {
            this.energyStorage.setEnergy(tag.getInt("Energy"));
        }
        this.progress = tag.getInt("Progress");
        if (tag.contains("ProgressAccumulator")) {
            this.progressAccumulator = tag.getDouble("ProgressAccumulator");
        } else {
            this.progressAccumulator = 0.0;
        }
        this.maxProgress = tag.contains("MaxProgress") ? tag.getInt("MaxProgress") : 100;
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
        if (tag.contains("Heat")) {
            this.heatModel.setHeat(tag.getDouble("Heat"));
        }
        if (tag.contains("Overheated")) {
            this.heatModel.setOverheated(tag.getBoolean("Overheated"));
        }
        ItemStack input = this.itemHandler.getStackInSlot(0);
        this.lastInputItem = input.isEmpty() ? Items.AIR : input.getItem();
        this.cachedRecipe = null;
    }
}
