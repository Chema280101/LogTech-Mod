package io.github.chema280101.logtech.machine;

import io.github.chema280101.logtech.machine.recipe.CentrifugingRecipe;
import io.github.chema280101.logtech.machine.recipe.ProcessingLogic;
import io.github.chema280101.logtech.machine.recipe.ProcessingOutput;
import io.github.chema280101.logtech.network.EnergyPriority;
import io.github.chema280101.logtech.network.NetworkMember;
import io.github.chema280101.logtech.registry.ModBlockEntities;
import io.github.chema280101.logtech.registry.ModDataComponents;
import io.github.chema280101.logtech.registry.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FluidState;
import io.github.chema280101.logtech.registry.ModBlocks;
import io.github.chema280101.logtech.LogTechConfig;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CentrifugeBlockEntity extends BlockEntity implements MenuProvider, NetworkMember, HeatDissipatingMachine, MachineConfigurable {
    public static final int ENERGY_CAPACITY = 20_000;
    public static final int ENERGY_MAX_RECEIVE = 200;

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT_1 = 1;
    public static final int OUTPUT_SLOT_2 = 2;
    public static final int OUTPUT_SLOT_3 = 3;
    public static final int TOTAL_SLOTS = 4;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == INPUT_SLOT) {
                checkRecipeChanged();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == INPUT_SLOT) {
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
    private int maxProgress = 120;
    private MachineStatus status = MachineStatus.NO_INPUT;
    private EnergyPriority energyPriority = EnergyPriority.NORMAL;
    private boolean energyShed = false;
    private final HeatModel heatModel = new HeatModel();
    private double cachedCooling = -1.0;
    private int cachedHeatsinks = 0;
    private int cachedWaterSides = 0;

    private CentrifugingRecipe cachedRecipe = null;
    private Item lastInputItem = Items.AIR;
    private List<ItemStack> pendingOutputs = null;

    private final ActivityCounter activityCounter = new ActivityCounter();

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> CentrifugeBlockEntity.this.progress;
                case 1 -> CentrifugeBlockEntity.this.maxProgress;
                case 2 -> CentrifugeBlockEntity.this.energyStorage.getEnergyStored() & 0xFFFF;
                case 3 -> (CentrifugeBlockEntity.this.energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 4 -> CentrifugeBlockEntity.this.energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 5 -> (CentrifugeBlockEntity.this.energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 6 -> CentrifugeBlockEntity.this.status.ordinal();
                case 7 -> CentrifugeBlockEntity.this.energyPriority.ordinal();
                case 8 -> (int) Math.round(CentrifugeBlockEntity.this.heatModel.getHeat());
                case 9 -> (int) Math.round(CentrifugeBlockEntity.this.getCachedCooling() * 10.0);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> CentrifugeBlockEntity.this.progress = value;
                case 1 -> CentrifugeBlockEntity.this.maxProgress = value;
                case 6 -> {
                    if (value >= 0 && value < MachineStatus.values().length) {
                        CentrifugeBlockEntity.this.status = MachineStatus.values()[value];
                    }
                }
                case 7 -> {
                    CentrifugeBlockEntity.this.energyPriority = EnergyPriority.fromOrdinal(value);
                    CentrifugeBlockEntity.this.setChanged();
                }
                case 8 -> CentrifugeBlockEntity.this.heatModel.setHeat(value);
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
            return TOTAL_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == INPUT_SLOT ? itemHandler.getStackInSlot(INPUT_SLOT) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == INPUT_SLOT && (CentrifugeBlockEntity.this.level == null || hasRecipe(stack))) {
                return itemHandler.insertItem(INPUT_SLOT, stack, simulate);
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
            return slot == INPUT_SLOT && (CentrifugeBlockEntity.this.level == null || hasRecipe(stack));
        }
    };

    private boolean canExtractAutomated(ItemStack stack) {
        if (stack.isEmpty()) return false;
        io.github.chema280101.logtech.machine.firmware.FirmwareType fw = getFirmwareType();
        if (fw != io.github.chema280101.logtech.machine.firmware.FirmwareType.SELECTIVE) return true;
        ItemStack fwStack = this.firmwareHandler.getStackInSlot(0);
        net.minecraft.world.item.Item filterItem = io.github.chema280101.logtech.registry.ModDataComponents.getFirmwareFilter(fwStack);
        if (filterItem == null) return true;
        return stack.getItem() == filterItem;
    }

    private final IItemHandler outputHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return TOTAL_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (isOutputSlot(slot)) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (canExtractAutomated(stack)) {
                    return stack;
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (isOutputSlot(slot)) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (!canExtractAutomated(stack)) {
                    return ItemStack.EMPTY;
                }
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

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CENTRIFUGE.get(), pos, state);
    }

    private static boolean isOutputSlot(int slot) {
        return slot >= OUTPUT_SLOT_1 && slot <= OUTPUT_SLOT_3;
    }

    public ItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    public ItemStackHandler getFirmwareHandler() {
        return this.firmwareHandler;
    }

    @Override
    public io.github.chema280101.logtech.machine.firmware.FirmwareType getFirmwareType() {
        ItemStack stack = this.firmwareHandler.getStackInSlot(0);
        if (stack.getItem() instanceof io.github.chema280101.logtech.item.FirmwareItem fwItem) {
            return fwItem.getFirmwareType();
        }
        return io.github.chema280101.logtech.machine.firmware.FirmwareType.NONE;
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

    @Override
    public ActivityCounter getActivityCounter() {
        return this.activityCounter;
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

    private CentrifugingRecipe findRecipe(ItemStack stack) {
        if (stack.isEmpty() || this.level == null) {
            return null;
        }
        return this.level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.CENTRIFUGING.get(), new SingleRecipeInput(stack), this.level)
                .map(RecipeHolder::value)
                .orElse(null);
    }

    private void checkRecipeChanged() {
        ItemStack currentInput = this.itemHandler.getStackInSlot(INPUT_SLOT);
        Item currentItem = currentInput.isEmpty() ? Items.AIR : currentInput.getItem();
        if (currentItem != this.lastInputItem) {
            this.lastInputItem = currentItem;
            this.cachedRecipe = findRecipe(currentInput);
            this.progress = 0;
            this.pendingOutputs = null;
            if (this.cachedRecipe != null) {
                this.maxProgress = this.cachedRecipe.ticks();
            }
        }
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

        ItemStack inputStack = this.itemHandler.getStackInSlot(INPUT_SLOT);

        // 1. Manejo de subproductos pendientes si una salida estuvo llena
        if (this.pendingOutputs != null && !this.pendingOutputs.isEmpty()) {
            if (canFitAllOutputs(this.pendingOutputs)) {
                // Hay espacio ahora: insertar todo sin perder nada
                insertOutputs(this.pendingOutputs);
                int totalProduced = 0;
                for (ItemStack s : this.pendingOutputs) {
                    totalProduced += s.getCount();
                }
                this.itemHandler.extractItem(INPUT_SLOT, 1, false);
                this.activityCounter.recordItemIn(1);
                this.activityCounter.recordItemOut(totalProduced);
                this.pendingOutputs = null;
                this.progress = 0;
                this.progressAccumulator = 0.0;

                ItemStack remaining = this.itemHandler.getStackInSlot(INPUT_SLOT);
                if (remaining.isEmpty()) {
                    this.cachedRecipe = null;
                    this.lastInputItem = Items.AIR;
                    this.status = MachineStatus.NO_INPUT;
                } else {
                    this.status = MachineStatus.IDLE;
                }
                setChanged();
                return;
            } else {
                if (this.status != MachineStatus.OUTPUT_FULL) {
                    this.status = MachineStatus.OUTPUT_FULL;
                    setChanged();
                }
                this.activityCounter.tick(false);
                return;
            }
        }

        // 2. Comprobar entrada válida
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

        // 3. Comprobación previa de capacidad de salida
        // Si ninguna salida garantizada cabe y los slots están saturados
        if (!canFitGuaranteedOutputs(this.cachedRecipe, inputStack)) {
            if (this.status != MachineStatus.OUTPUT_FULL) {
                this.status = MachineStatus.OUTPUT_FULL;
                setChanged();
            }
            this.activityCounter.tick(false);
            return;
        }

        // 4. Comprobación de energía
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

        // 6. Finalización del ciclo de centrifugado
        if (this.progress >= this.maxProgress) {
            int inputPurity = inputStack.has(ModDataComponents.PURITY.get())
                    ? ModDataComponents.getPurity(inputStack)
                    : 1;

            // Determinar salidas según probabilidades usando level.random del servidor (NUNCA Math.random)
            List<ItemStack> toProduce = new ArrayList<>();
            for (ProcessingOutput out : this.cachedRecipe.outputs()) {
                float effectiveChance = ProcessingLogic.calculateEffectiveChance(out.chance(), out.scaling(), inputPurity);
                if (ProcessingLogic.shouldProduce(effectiveChance, level.random.nextDouble())) {
                    ItemStack outStack = out.createStack();
                    if (outStack.has(ModDataComponents.PURITY.get())) {
                        int outPurity = ProcessingLogic.calculateOutputPurity(inputPurity, this.cachedRecipe.purityMode());
                        ModDataComponents.setPurity(outStack, outPurity);
                    }
                    toProduce.add(outStack);
                }
            }

            // Comprobar si todas las salidas producidas caben en los slots de salida (1..3)
            if (canFitAllOutputs(toProduce)) {
                insertOutputs(toProduce);
                int totalOut = 0;
                for (ItemStack s : toProduce) {
                    totalOut += s.getCount();
                }
                this.itemHandler.extractItem(INPUT_SLOT, 1, false);
                this.activityCounter.recordItemIn(1);
                this.activityCounter.recordItemOut(totalOut);
                this.progress = 0;
                this.progressAccumulator = 0.0;

                ItemStack remaining = this.itemHandler.getStackInSlot(INPUT_SLOT);
                if (remaining.isEmpty()) {
                    this.cachedRecipe = null;
                    this.lastInputItem = Items.AIR;
                    this.status = MachineStatus.NO_INPUT;
                }
            } else {
                // Si una salida no cabe, pasa a OUTPUT_FULL y guarda pendingOutputs para NUNCA perder subproductos
                this.pendingOutputs = toProduce;
                this.status = MachineStatus.OUTPUT_FULL;
            }
        }

        setChanged();
    }

    /**
     * Comprueba si todas las salidas indicadas pueden depositarse en las ranuras de salida (1, 2, 3).
     */
    public boolean canFitAllOutputs(List<ItemStack> outputs) {
        if (outputs.isEmpty()) {
            return true;
        }

        // Crear copia virtual de las ranuras de salida
        ItemStack[] virtualSlots = new ItemStack[3];
        for (int i = 0; i < 3; i++) {
            ItemStack original = this.itemHandler.getStackInSlot(OUTPUT_SLOT_1 + i);
            virtualSlots[i] = original.isEmpty() ? ItemStack.EMPTY : original.copy();
        }

        for (ItemStack toInsert : outputs) {
            int remaining = toInsert.getCount();

            // 1. Intentar apilar en ranuras existentes con mismo ítem y componentes
            for (int i = 0; i < 3; i++) {
                ItemStack slotStack = virtualSlots[i];
                if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, toInsert)) {
                    int space = slotStack.getMaxStackSize() - slotStack.getCount();
                    int inserted = Math.min(space, remaining);
                    slotStack.grow(inserted);
                    remaining -= inserted;
                    if (remaining <= 0) {
                        break;
                    }
                }
            }

            // 2. Intentar colocar en ranuras vacías
            if (remaining > 0) {
                for (int i = 0; i < 3; i++) {
                    if (virtualSlots[i].isEmpty()) {
                        int inserted = Math.min(toInsert.getMaxStackSize(), remaining);
                        ItemStack newStack = toInsert.copy();
                        newStack.setCount(inserted);
                        virtualSlots[i] = newStack;
                        remaining -= inserted;
                        if (remaining <= 0) {
                            break;
                        }
                    }
                }
            }

            if (remaining > 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Comprobación preventiva de salidas garantizadas (chance >= 1.0f).
     */
    private boolean canFitGuaranteedOutputs(CentrifugingRecipe recipe, ItemStack inputStack) {
        int inputPurity = inputStack.has(ModDataComponents.PURITY.get()) ? ModDataComponents.getPurity(inputStack) : 1;
        List<ItemStack> guaranteed = new ArrayList<>();
        for (ProcessingOutput out : recipe.outputs()) {
            if (out.chance() >= 1.0f) {
                ItemStack st = out.createStack();
                if (st.has(ModDataComponents.PURITY.get())) {
                    ModDataComponents.setPurity(st, ProcessingLogic.calculateOutputPurity(inputPurity, recipe.purityMode()));
                }
                guaranteed.add(st);
            }
        }
        return canFitAllOutputs(guaranteed);
    }

    /**
     * Inserta efectivamente la lista de salidas en las ranuras de salida (1..3).
     */
    private void insertOutputs(List<ItemStack> outputs) {
        for (ItemStack toInsert : outputs) {
            int remaining = toInsert.getCount();

            // 1. Apilar en ranuras existentes
            for (int i = OUTPUT_SLOT_1; i <= OUTPUT_SLOT_3; i++) {
                ItemStack slotStack = this.itemHandler.getStackInSlot(i);
                if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, toInsert)) {
                    int space = slotStack.getMaxStackSize() - slotStack.getCount();
                    int inserted = Math.min(space, remaining);
                    slotStack.grow(inserted);
                    remaining -= inserted;
                    if (remaining <= 0) {
                        break;
                    }
                }
            }

            // 2. Colocar en ranuras vacías
            if (remaining > 0) {
                for (int i = OUTPUT_SLOT_1; i <= OUTPUT_SLOT_3; i++) {
                    ItemStack slotStack = this.itemHandler.getStackInSlot(i);
                    if (slotStack.isEmpty()) {
                        int inserted = Math.min(toInsert.getMaxStackSize(), remaining);
                        ItemStack newStack = toInsert.copy();
                        newStack.setCount(inserted);
                        this.itemHandler.setStackInSlot(i, newStack);
                        remaining -= inserted;
                        if (remaining <= 0) {
                            break;
                        }
                    }
                }
            }
        }
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
        if (this.pendingOutputs != null) {
            for (ItemStack pending : this.pendingOutputs) {
                if (!pending.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), pending);
                }
            }
            this.pendingOutputs = null;
        }
    }

    @Override
    public Component getName() {
        return Component.translatable("block.logtech.centrifuge");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.logtech.centrifuge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CentrifugeMenu(containerId, playerInventory, this.worldPosition, this.itemHandler, this.firmwareHandler, this.dataAccess);
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

        if (this.pendingOutputs != null && !this.pendingOutputs.isEmpty()) {
            ListTag pendingList = new ListTag();
            for (ItemStack stack : this.pendingOutputs) {
                if (!stack.isEmpty()) {
                    pendingList.add(stack.save(registries, new CompoundTag()));
                }
            }
            tag.put("PendingOutputs", pendingList);
        }
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
        this.maxProgress = tag.contains("MaxProgress") ? tag.getInt("MaxProgress") : 120;
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

        if (tag.contains("PendingOutputs", Tag.TAG_LIST)) {
            ListTag pendingList = tag.getList("PendingOutputs", Tag.TAG_COMPOUND);
            this.pendingOutputs = new ArrayList<>();
            for (int i = 0; i < pendingList.size(); i++) {
                ItemStack.parse(registries, pendingList.getCompound(i)).ifPresent(this.pendingOutputs::add);
            }
        } else {
            this.pendingOutputs = null;
        }

        ItemStack input = this.itemHandler.getStackInSlot(INPUT_SLOT);
        this.lastInputItem = input.isEmpty() ? Items.AIR : input.getItem();
        this.cachedRecipe = null;
    }
}
