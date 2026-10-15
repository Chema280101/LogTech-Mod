package io.github.chema280101.logtech.machine;

import io.github.chema280101.logtech.ore.DepositSavedData;
import io.github.chema280101.logtech.ore.DepositType;
import io.github.chema280101.logtech.ore.DepositVein;
import io.github.chema280101.logtech.ore.VeinGenerator;
import io.github.chema280101.logtech.registry.ModBlockEntities;
import io.github.chema280101.logtech.registry.ModDataComponents;
import io.github.chema280101.logtech.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import io.github.chema280101.logtech.network.NetworkMember;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FluidState;
import io.github.chema280101.logtech.registry.ModBlocks;
import io.github.chema280101.logtech.LogTechConfig;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;

public class DrillBlockEntity extends BlockEntity implements NetworkMember, HeatDissipatingMachine, net.minecraft.world.MenuProvider, MachineConfigurable {
    public static final int ENERGY_CAPACITY = 10_000;
    public static final int ENERGY_MAX_RECEIVE = 200;
    public static final int PUSH_INTERVAL = 20;
    public static final int BUFFER_CAPACITY = 64;

    private final ItemStackHandler bufferHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return BUFFER_CAPACITY;
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
    private int pushTimer = 0;
    private MachineStatus status = MachineStatus.NO_RESOURCE;
    private io.github.chema280101.logtech.network.EnergyPriority energyPriority =
            io.github.chema280101.logtech.network.EnergyPriority.NORMAL;
    private boolean energyShed = false;
    private final HeatModel heatModel = new HeatModel();
    private double cachedCooling = -1.0;
    private int cachedHeatsinks = 0;
    private int cachedWaterSides = 0;

    private final net.minecraft.world.inventory.ContainerData dataAccess = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> DrillBlockEntity.this.progress;
                case 1 -> LogTechConfig.DRILL_PROCESS_TIME.get();
                case 2 -> DrillBlockEntity.this.energyStorage.getEnergyStored() & 0xFFFF;
                case 3 -> (DrillBlockEntity.this.energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 4 -> DrillBlockEntity.this.energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 5 -> (DrillBlockEntity.this.energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 6 -> DrillBlockEntity.this.status.ordinal();
                case 7 -> DrillBlockEntity.this.energyPriority.ordinal();
                case 8 -> (int) Math.round(DrillBlockEntity.this.heatModel.getHeat());
                case 9 -> (int) Math.round(DrillBlockEntity.this.getCachedCooling() * 10.0);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> DrillBlockEntity.this.progress = value;
                case 6 -> {
                    if (value >= 0 && value < MachineStatus.values().length) {
                        DrillBlockEntity.this.status = MachineStatus.values()[value];
                    }
                }
                case 7 -> {
                    DrillBlockEntity.this.energyPriority = io.github.chema280101.logtech.network.EnergyPriority.fromOrdinal(value);
                    DrillBlockEntity.this.setChanged();
                }
                case 8 -> DrillBlockEntity.this.heatModel.setHeat(value);
            }
        }

        @Override
        public int getCount() {
            return 10;
        }
    };

    // Sided capability para extracción de automatización externa
    private final IItemHandler outputHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return bufferHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return bufferHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return BUFFER_CAPACITY;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    public DrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRILL.get(), pos, state);
    }

    public ItemStackHandler getBufferHandler() {
        return this.bufferHandler;
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

    public net.minecraft.world.inventory.ContainerData getDataAccess() {
        return this.dataAccess;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.logtech.drill");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DrillMenu(containerId, playerInventory, this.worldPosition, this.bufferHandler, this.firmwareHandler, this.dataAccess);
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        return this.outputHandler;
    }

    public MachineEnergyStorage getEnergyStorage() {
        return this.energyStorage;
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
    public io.github.chema280101.logtech.network.EnergyPriority getEnergyPriority() {
        return this.energyPriority;
    }

    @Override
    public void setEnergyPriority(io.github.chema280101.logtech.network.EnergyPriority priority) {
        this.energyPriority = priority != null ? priority : io.github.chema280101.logtech.network.EnergyPriority.NORMAL;
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

    public int getBufferCount() {
        ItemStack stack = this.bufferHandler.getStackInSlot(0);
        return stack.isEmpty() ? 0 : stack.getCount();
    }

    public static Item getRawItemForDeposit(DepositType type) {
        return switch (type) {
            case NEOFERRITE -> ModItems.RAW_NEOFERRITE.get();
            case CRYSTALITE -> ModItems.RAW_CRYSTALITE.get();
        };
    }

    @Override
    public Component getName() {
        return Component.translatable("block.logtech.drill");
    }

    private final ActivityCounter activityCounter = new ActivityCounter();

    public ActivityCounter getActivityCounter() {
        return this.activityCounter;
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

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
            updateMachineState(level, pos, state, MachineStatus.OVERHEATED, false);
            this.activityCounter.tick(false);
            return;
        } else if (this.status == MachineStatus.OVERHEATED) {
            this.status = MachineStatus.NO_RESOURCE;
            setChanged();
        }

        // 1. Empujar lo producido a los item handlers vecinos cada 20 ticks
        pushTimer++;
        if (pushTimer >= PUSH_INTERVAL) {
            pushTimer = 0;
            pushToNeighbors(serverLevel, pos);
        }

        // 2. Verificar si cae dentro del radio de una veta descubierta
        DepositSavedData savedData = DepositSavedData.get(serverLevel);
        List<DepositVein> veins = VeinGenerator.getVeinsAt(serverLevel.getSeed(), pos.getX(), pos.getZ());
        DepositVein targetVein = null;
        for (DepositVein vein : veins) {
            if (savedData.isDiscovered(vein)) {
                targetVein = vein;
                break;
            }
        }

        if (targetVein == null) {
            updateMachineState(level, pos, state, MachineStatus.NO_RESOURCE, false);
            this.activityCounter.tick(false);
            this.progress = 0;
            this.progressAccumulator = 0.0;
            return;
        }

        // 3. Verificar si la reserva de la veta no está agotada
        boolean infinite = io.github.chema280101.logtech.LogTechConfig.INFINITE_RESERVES.get();
        int remaining = savedData.getRemainingReserve(targetVein);
        if (!infinite && remaining <= 0) {
            updateMachineState(level, pos, state, MachineStatus.NO_RESOURCE, false);
            this.activityCounter.tick(false);
            this.progress = 0;
            this.progressAccumulator = 0.0;
            return;
        }

        // 4. Verificar espacio en el búfer de 64 ítems
        int produceAmount = io.github.chema280101.logtech.LogTechConfig.DRILL_PRODUCTION_AMOUNT.get();
        Item rawItem = getRawItemForDeposit(targetVein.getType());
        ItemStack producedStack = new ItemStack(rawItem, produceAmount);
        ModDataComponents.setPurity(producedStack, targetVein.getPurity());

        ItemStack currentBuffer = this.bufferHandler.getStackInSlot(0);
        boolean canBufferFit = currentBuffer.isEmpty()
                || (ItemStack.isSameItemSameComponents(currentBuffer, producedStack)
                && currentBuffer.getCount() + produceAmount <= BUFFER_CAPACITY);

        if (!canBufferFit) {
            updateMachineState(level, pos, state, MachineStatus.OUTPUT_FULL, false);
            this.activityCounter.tick(false);
            return;
        }

        // 5. Verificar energía
        int baseEnergyCost = io.github.chema280101.logtech.LogTechConfig.DRILL_ENERGY_CONSUMPTION.get();
        int energyCost = io.github.chema280101.logtech.machine.firmware.FirmwareEffects.calculateEnergyCost(
                baseEnergyCost,
                fw,
                io.github.chema280101.logtech.LogTechConfig.FIRMWARE_ECO_ENERGY.get(),
                io.github.chema280101.logtech.LogTechConfig.FIRMWARE_TURBO_ENERGY.get()
        );

        if (this.energyStorage.getEnergyStored() < energyCost) {
            MachineStatus target = this.energyShed ? MachineStatus.SHED : MachineStatus.NO_ENERGY;
            updateMachineState(level, pos, state, target, false);
            this.activityCounter.tick(false);
            return;
        }

        // 6. Todas las condiciones cumplidas: trabajando y consumiendo energía
        updateMachineState(level, pos, state, MachineStatus.WORKING, true);

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

        // 7. Cada N ticks produce N mineral crudo y resta a su reserva en SavedData si no es infinita
        int processTime = io.github.chema280101.logtech.LogTechConfig.DRILL_PROCESS_TIME.get();
        if (this.progress >= processTime) {
            this.progress = 0;
            this.progressAccumulator = 0.0;

            if (!infinite) {
                int currentReserve = savedData.getRemainingReserve(targetVein);
                if (currentReserve > 0) {
                    int toDeduct = Math.min(currentReserve, produceAmount);
                    savedData.setRemainingReserve(targetVein, currentReserve - toDeduct);
                } else {
                    // Reserva agotada concurrentemente
                    updateMachineState(level, pos, state, MachineStatus.NO_RESOURCE, false);
                    setChanged();
                    return;
                }
            }

            if (currentBuffer.isEmpty()) {
                this.bufferHandler.setStackInSlot(0, producedStack);
            } else {
                currentBuffer.grow(produceAmount);
            }
            this.activityCounter.recordItemOut(produceAmount);
            setChanged();
        }
    }

    private void updateMachineState(Level level, BlockPos pos, BlockState state, MachineStatus newStatus, boolean active) {
        boolean statusChanged = (this.status != newStatus);
        if (statusChanged) {
            this.status = newStatus;
            setChanged();
        }
        if (state.hasProperty(DrillBlock.ACTIVE) && state.getValue(DrillBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(DrillBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    private void pushToNeighbors(Level level, BlockPos pos) {
        ItemStack bufferStack = bufferHandler.getStackInSlot(0);
        if (bufferStack.isEmpty()) {
            return;
        }

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            IItemHandler neighborHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, direction.getOpposite());
            if (neighborHandler != null) {
                ItemStack toInsert = bufferStack.copy();
                ItemStack remainder = insertIntoHandler(neighborHandler, toInsert, false);
                int inserted = toInsert.getCount() - remainder.getCount();
                if (inserted > 0) {
                    bufferHandler.extractItem(0, inserted, false);
                    setChanged();
                    bufferStack = bufferHandler.getStackInSlot(0);
                    if (bufferStack.isEmpty()) {
                        break;
                    }
                }
            }
        }
    }

    private ItemStack insertIntoHandler(IItemHandler target, ItemStack stack, boolean simulate) {
        if (target == null || stack.isEmpty()) return stack;
        ItemStack remainder = stack.copy();
        for (int i = 0; i < target.getSlots(); i++) {
            remainder = target.insertItem(i, remainder, simulate);
            if (remainder.isEmpty()) {
                break;
            }
        }
        return remainder;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < this.bufferHandler.getSlots(); i++) {
            ItemStack stack = this.bufferHandler.getStackInSlot(i);
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("data_version", 4);
        tag.put("Inventory", this.bufferHandler.serializeNBT(registries));
        tag.put("Firmware", this.firmwareHandler.serializeNBT(registries));
        tag.putInt("Energy", this.energyStorage.getEnergyStored());
        tag.putInt("Progress", this.progress);
        tag.putDouble("ProgressAccumulator", this.progressAccumulator);
        tag.putInt("PushTimer", this.pushTimer);
        tag.putInt("Status", this.status.ordinal());
        tag.putString("energy_priority", this.energyPriority.name());
        tag.putDouble("Heat", this.heatModel.getHeat());
        tag.putBoolean("Overheated", this.heatModel.isOverheated());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            this.bufferHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
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
        this.pushTimer = tag.getInt("PushTimer");
        if (tag.contains("Status")) {
            int ord = tag.getInt("Status");
            if (ord >= 0 && ord < MachineStatus.values().length) {
                this.status = MachineStatus.values()[ord];
            }
        }
        if (tag.contains("energy_priority")) {
            this.energyPriority = io.github.chema280101.logtech.network.EnergyPriority.fromName(tag.getString("energy_priority"));
        } else {
            this.energyPriority = io.github.chema280101.logtech.network.EnergyPriority.NORMAL;
        }
        if (tag.contains("Heat")) {
            this.heatModel.setHeat(tag.getDouble("Heat"));
        }
        if (tag.contains("Overheated")) {
            this.heatModel.setOverheated(tag.getBoolean("Overheated"));
        }
    }
}
