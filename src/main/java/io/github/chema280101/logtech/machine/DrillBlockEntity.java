package io.github.chema280101.logtech.machine;

import io.github.chema280101.logtech.ore.DepositSavedData;
import io.github.chema280101.logtech.ore.DepositType;
import io.github.chema280101.logtech.ore.DepositVein;
import io.github.chema280101.logtech.ore.VeinGenerator;
import io.github.chema280101.logtech.registry.ModBlockEntities;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DrillBlockEntity extends BlockEntity implements NetworkMember {
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

    private final MachineEnergyStorage energyStorage = new MachineEnergyStorage(
            ENERGY_CAPACITY,
            ENERGY_MAX_RECEIVE,
            0,
            this::setChanged
    );

    private int progress = 0;
    private int pushTimer = 0;
    private MachineStatus status = MachineStatus.NO_RESOURCE;

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

    public IItemHandler getItemHandler(@Nullable Direction side) {
        return this.outputHandler;
    }

    public MachineEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public MachineStatus getStatus() {
        return this.status;
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
            return;
        }

        // 3. Verificar si la reserva de la veta no está agotada
        boolean infinite = io.github.chema280101.logtech.LogTechConfig.INFINITE_RESERVES.get();
        int remaining = savedData.getRemainingReserve(targetVein);
        if (!infinite && remaining <= 0) {
            updateMachineState(level, pos, state, MachineStatus.NO_RESOURCE, false);
            this.activityCounter.tick(false);
            this.progress = 0;
            return;
        }

        // 4. Verificar espacio en el búfer de 64 ítems
        int produceAmount = io.github.chema280101.logtech.LogTechConfig.DRILL_PRODUCTION_AMOUNT.get();
        Item rawItem = getRawItemForDeposit(targetVein.getType());
        ItemStack currentBuffer = this.bufferHandler.getStackInSlot(0);
        boolean canBufferFit = currentBuffer.isEmpty()
                || (currentBuffer.is(rawItem) && currentBuffer.getCount() + produceAmount <= BUFFER_CAPACITY);

        if (!canBufferFit) {
            updateMachineState(level, pos, state, MachineStatus.OUTPUT_FULL, false);
            this.activityCounter.tick(false);
            return;
        }

        // 5. Verificar energía
        int energyCost = io.github.chema280101.logtech.LogTechConfig.DRILL_ENERGY_CONSUMPTION.get();
        if (this.energyStorage.getEnergyStored() < energyCost) {
            updateMachineState(level, pos, state, MachineStatus.NO_ENERGY, false);
            this.activityCounter.tick(false);
            return;
        }

        // 6. Todas las condiciones cumplidas: trabajando y consumiendo energía
        updateMachineState(level, pos, state, MachineStatus.WORKING, true);
        this.energyStorage.consume(energyCost);
        this.progress++;
        this.activityCounter.tick(true);

        // 7. Cada N ticks produce N mineral crudo y resta a su reserva en SavedData si no es infinita
        int processTime = io.github.chema280101.logtech.LogTechConfig.DRILL_PROCESS_TIME.get();
        if (this.progress >= processTime) {
            this.progress = 0;

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
                this.bufferHandler.setStackInSlot(0, new ItemStack(rawItem, produceAmount));
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
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", this.bufferHandler.serializeNBT(registries));
        tag.putInt("Energy", this.energyStorage.getEnergyStored());
        tag.putInt("Progress", this.progress);
        tag.putInt("PushTimer", this.pushTimer);
        tag.putInt("Status", this.status.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            this.bufferHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("Energy")) {
            this.energyStorage.setEnergy(tag.getInt("Energy"));
        }
        this.progress = tag.getInt("Progress");
        this.pushTimer = tag.getInt("PushTimer");
        if (tag.contains("Status")) {
            int ord = tag.getInt("Status");
            if (ord >= 0 && ord < MachineStatus.values().length) {
                this.status = MachineStatus.values()[ord];
            }
        }
    }
}
