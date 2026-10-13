package io.github.chema280101.logtech.machine;

import io.github.chema280101.logtech.LogTechConfig;
import io.github.chema280101.logtech.block.CoreBlockEntity;
import io.github.chema280101.logtech.network.Network;
import io.github.chema280101.logtech.network.NetworkManager;
import io.github.chema280101.logtech.network.NetworkMember;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class CoalGeneratorBlockEntity extends BlockEntity implements MenuProvider, NetworkMember {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && getBurnDuration(stack) > 0;
        }
    };

    private final MachineEnergyStorage energyStorage = new MachineEnergyStorage(
            LogTechConfig.COAL_GENERATOR_CAPACITY.get(),
            0,
            LogTechConfig.COAL_GENERATOR_MAX_TRANSFER.get(),
            this::setChanged
    );

    private int burnTime = 0;
    private int maxBurnTime = 0;
    private MachineStatus status = MachineStatus.NO_INPUT;
    private final ActivityCounter activityCounter = new ActivityCounter();
    private Network cachedNetwork = null;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> CoalGeneratorBlockEntity.this.burnTime & 0xFFFF;
                case 1 -> (CoalGeneratorBlockEntity.this.burnTime >> 16) & 0xFFFF;
                case 2 -> CoalGeneratorBlockEntity.this.maxBurnTime & 0xFFFF;
                case 3 -> (CoalGeneratorBlockEntity.this.maxBurnTime >> 16) & 0xFFFF;
                case 4 -> CoalGeneratorBlockEntity.this.energyStorage.getEnergyStored() & 0xFFFF;
                case 5 -> (CoalGeneratorBlockEntity.this.energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 6 -> CoalGeneratorBlockEntity.this.energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 7 -> (CoalGeneratorBlockEntity.this.energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 8 -> CoalGeneratorBlockEntity.this.status.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> CoalGeneratorBlockEntity.this.burnTime = (CoalGeneratorBlockEntity.this.burnTime & 0xFFFF0000) | (value & 0xFFFF);
                case 1 -> CoalGeneratorBlockEntity.this.burnTime = (CoalGeneratorBlockEntity.this.burnTime & 0x0000FFFF) | ((value & 0xFFFF) << 16);
                case 2 -> CoalGeneratorBlockEntity.this.maxBurnTime = (CoalGeneratorBlockEntity.this.maxBurnTime & 0xFFFF0000) | (value & 0xFFFF);
                case 3 -> CoalGeneratorBlockEntity.this.maxBurnTime = (CoalGeneratorBlockEntity.this.maxBurnTime & 0x0000FFFF) | ((value & 0xFFFF) << 16);
                case 4 -> {
                    int currentMax = CoalGeneratorBlockEntity.this.energyStorage.getMaxEnergyStored();
                    int newLow = value & 0xFFFF;
                    int newHigh = (CoalGeneratorBlockEntity.this.energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    CoalGeneratorBlockEntity.this.energyStorage.setEnergy(Math.min(currentMax, (newHigh << 16) | newLow));
                }
                case 5 -> {
                    int currentMax = CoalGeneratorBlockEntity.this.energyStorage.getMaxEnergyStored();
                    int newLow = CoalGeneratorBlockEntity.this.energyStorage.getEnergyStored() & 0xFFFF;
                    int newHigh = value & 0xFFFF;
                    CoalGeneratorBlockEntity.this.energyStorage.setEnergy(Math.min(currentMax, (newHigh << 16) | newLow));
                }
                case 8 -> {
                    if (value >= 0 && value < MachineStatus.values().length) {
                        CoalGeneratorBlockEntity.this.status = MachineStatus.values()[value];
                    }
                }
            }
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COAL_GENERATOR.get(), pos, state);
    }

    public static int getBurnDuration(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int burn = stack.getBurnTime(RecipeType.SMELTING);
        if (burn > 0) {
            return burn;
        }
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
            return 1600;
        }
        if (stack.is(Items.COAL_BLOCK)) {
            return 16000;
        }
        return 0;
    }

    public ItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    public MachineEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    @Override
    public Component getName() {
        return Component.translatable("block.logtech.coal_generator");
    }

    @Override
    public MachineStatus getStatus() {
        return this.status;
    }

    @Override
    public ActivityCounter getActivityCounter() {
        return this.activityCounter;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CoalGeneratorMenu(containerId, playerInventory, this.worldPosition, this.itemHandler, this.dataAccess);
    }

    public void dropContents(Level level, BlockPos pos) {
        ItemStack stack = this.itemHandler.getStackInSlot(0);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        int genPerTick = LogTechConfig.COAL_GENERATOR_GENERATION.get();
        int maxTransfer = LogTechConfig.COAL_GENERATOR_MAX_TRANSFER.get();
        int currentEnergy = this.energyStorage.getEnergyStored();
        int maxEnergy = this.energyStorage.getMaxEnergyStored();

        // 1. Proceso de combustión y generación de energía
        if (this.burnTime > 0) {
            if (currentEnergy < maxEnergy) {
                int toGen = Math.min(maxEnergy - currentEnergy, genPerTick);
                this.energyStorage.setEnergy(currentEnergy + toGen);
                currentEnergy = this.energyStorage.getEnergyStored();
                this.burnTime--;
                this.status = MachineStatus.WORKING;
                this.activityCounter.tick(true);
                setChanged();
            } else {
                // Buffer lleno: pausar consumo de combustible temporalmente
                this.status = MachineStatus.IDLE;
                this.activityCounter.tick(false);
            }
        } else {
            // Sin combustible activo: intentar quemar si hay espacio de energía
            if (currentEnergy < maxEnergy) {
                ItemStack fuel = this.itemHandler.getStackInSlot(0);
                int duration = getBurnDuration(fuel);
                if (duration > 0) {
                    this.itemHandler.extractItem(0, 1, false);
                    this.burnTime = duration;
                    this.maxBurnTime = duration;
                    this.activityCounter.recordItemIn(1);
                    this.status = MachineStatus.WORKING;
                    setChanged();

                    // Generar en el primer tick de inicio
                    int toGen = Math.min(maxEnergy - currentEnergy, genPerTick);
                    this.energyStorage.setEnergy(currentEnergy + toGen);
                    currentEnergy = this.energyStorage.getEnergyStored();
                    this.burnTime--;
                    this.activityCounter.tick(true);
                } else {
                    this.burnTime = 0;
                    this.maxBurnTime = 0;
                    this.status = fuel.isEmpty() ? MachineStatus.NO_INPUT : MachineStatus.NO_RESOURCE;
                    this.activityCounter.tick(false);
                }
            } else {
                this.status = MachineStatus.IDLE;
                this.activityCounter.tick(false);
            }
        }

        // 2. Actualizar estado visual (LIT)
        boolean shouldBeLit = this.burnTime > 0 && this.status == MachineStatus.WORKING;
        if (state.getValue(CoalGeneratorBlock.LIT) != shouldBeLit) {
            level.setBlock(pos, state.setValue(CoalGeneratorBlock.LIT, shouldBeLit), 3);
        }

        // 3. Distribución / Push de energía
        int energyToSend = Math.min(this.energyStorage.getEnergyStored(), maxTransfer);
        if (energyToSend > 0) {
            // Push a bloques adyacentes en las 6 caras
            for (Direction dir : Direction.values()) {
                if (energyToSend <= 0) break;
                BlockPos neighborPos = pos.relative(dir);
                IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, dir.getOpposite());
                if (neighbor != null && neighbor.canReceive()) {
                    int accepted = neighbor.receiveEnergy(energyToSend, false);
                    if (accepted > 0) {
                        this.energyStorage.consume(accepted);
                        energyToSend -= accepted;
                    }
                }
            }

            // Push al Núcleo de la red si está conectado por Backbone
            if (energyToSend > 0) {
                Network net = getOrRecalculateNetwork();
                if (net != null && net.isValid() && !net.getCores().isEmpty()) {
                    for (BlockPos corePos : net.getCores()) {
                        if (energyToSend <= 0) break;
                        if (!corePos.equals(this.worldPosition) && level.hasChunkAt(corePos)) {
                            BlockEntity coreBe = level.getBlockEntity(corePos);
                            if (coreBe instanceof CoreBlockEntity core) {
                                int accepted = core.getEnergyStorage().receiveEnergy(energyToSend, false);
                                if (accepted > 0) {
                                    this.energyStorage.consume(accepted);
                                    energyToSend -= accepted;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Network getOrRecalculateNetwork() {
        if (level == null || level.isClientSide()) {
            return Network.EMPTY;
        }
        if (this.cachedNetwork == null || !this.cachedNetwork.isValid()) {
            NetworkManager manager = NetworkManager.get(level);
            if (manager != null) {
                this.cachedNetwork = manager.scan(worldPosition, null);
            }
        }
        return this.cachedNetwork != null ? this.cachedNetwork : Network.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("data_version", 2);
        tag.put("inventory", this.itemHandler.serializeNBT(registries));
        tag.putInt("energy", this.energyStorage.getEnergyStored());
        tag.putInt("burn_time", this.burnTime);
        tag.putInt("max_burn_time", this.maxBurnTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            this.itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("energy")) {
            this.energyStorage.setEnergy(tag.getInt("energy"));
        }
        this.burnTime = tag.getInt("burn_time");
        this.maxBurnTime = tag.getInt("max_burn_time");
    }
}
