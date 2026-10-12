package io.github.chema280101.logtech.block;

import io.github.chema280101.logtech.registry.ModBlockEntities;
import io.github.chema280101.logtech.registry.ModDataComponents;
import io.github.chema280101.logtech.storage.ItemKey;
import io.github.chema280101.logtech.storage.StorageContent;
import io.github.chema280101.logtech.storage.StorageEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import io.github.chema280101.logtech.machine.ActivityCounter;
import io.github.chema280101.logtech.network.Network;
import io.github.chema280101.logtech.network.NetworkManager;
import io.github.chema280101.logtech.network.NetworkMember;
import net.minecraft.network.chat.Component;
import io.github.chema280101.logtech.storage.CoreEnergyStorage;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CoreBlockEntity extends BlockEntity {
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int ENERGY_MAX_RECEIVE = 1_000;
    public static final int ENERGY_TICK_INTERVAL = 20;

    private final Map<ItemKey, Long> storage = new HashMap<>();
    private final ActivityCounter activityCounter = new ActivityCounter();
    private final CoreEnergyStorage energyStorage = new CoreEnergyStorage(
            ENERGY_CAPACITY,
            ENERGY_MAX_RECEIVE,
            0,
            this::setChanged
    );
    private int energyTickCounter = 0;
    private Network network = null;

    public CoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE.get(), pos, state);
    }

    public int getEnergyConsumptionPerTick() {
        return io.github.chema280101.logtech.LogTechConfig.CORE_ENERGY_CONSUMPTION.get();
    }

    public ActivityCounter getActivityCounter() {
        return this.activityCounter;
    }

    public CoreEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public int getEnergyStored() {
        return this.energyStorage.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return this.energyStorage.getMaxEnergyStored();
    }

    public boolean isPowered() {
        return this.energyStorage.getEnergyStored() >= getEnergyConsumptionPerTick();
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        this.activityCounter.tick(isPowered());
        this.energyTickCounter++;
        if (this.energyTickCounter >= ENERGY_TICK_INTERVAL) {
            this.energyTickCounter = 0;
            Network net = getOrRecalculateNetwork();
            if (net.isValid()) {
                int consumption = getEnergyConsumptionPerTick();
                int accumulated = consumption * ENERGY_TICK_INTERVAL;
                if (this.energyStorage.getEnergyStored() >= accumulated) {
                    this.energyStorage.consume(accumulated);
                } else if (this.energyStorage.getEnergyStored() > 0) {
                    this.energyStorage.consume(this.energyStorage.getEnergyStored());
                }
            }
        }
    }

    public void setNetwork(Network network) {
        this.network = network;
        setChanged();
    }

    public Network getNetwork() {
        return this.network;
    }

    public Network getOrRecalculateNetwork() {
        if (this.network == null && level != null && !level.isClientSide()) {
            recalculateNetwork();
        }
        return this.network != null ? this.network : Network.EMPTY;
    }

    public void recalculateNetwork() {
        if (level != null && !level.isClientSide()) {
            NetworkManager manager = NetworkManager.get(level);
            if (manager != null) {
                Network scanned = manager.scan(worldPosition, null);
                manager.updateCoresInNetwork(scanned);
            }
        }
    }

    public long getCapacity() {
        Network net = getOrRecalculateNetwork();
        if (!net.isValid() || level == null) {
            return 0L;
        }
        long totalCapacity = 0L;
        for (BlockPos rackPos : net.getRacks()) {
            if (level.hasChunkAt(rackPos)) {
                BlockEntity be = level.getBlockEntity(rackPos);
                if (be instanceof RackBlockEntity rackBe) {
                    totalCapacity += rackBe.getCapacity();
                }
            }
        }
        return totalCapacity;
    }

    public long getTotalStored() {
        return storage.values().stream().mapToLong(Long::longValue).sum();
    }

    public boolean isOverloaded() {
        return getTotalStored() > getCapacity();
    }

    public List<NetworkMember> getMembers() {
        Network net = getOrRecalculateNetwork();
        if (!net.isValid() || level == null) {
            return Collections.emptyList();
        }
        List<NetworkMember> members = new ArrayList<>();
        for (BlockPos pos : net.getMachines()) {
            if (level.hasChunkAt(pos)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof NetworkMember member && !be.isRemoved()) {
                    members.add(member);
                }
            }
        }
        return members;
    }

    public List<BlockPos> getMemberPositions() {
        Network net = getOrRecalculateNetwork();
        if (!net.isValid()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(net.getMachines());
    }

    public int getMemberCount() {
        Network net = getOrRecalculateNetwork();
        return net.isValid() ? net.getMachineCount() : 0;
    }

    public void showNetworkStatus(Player player) {
        Network net = getOrRecalculateNetwork();
        int count = net.getBlockCount();
        String key;
        if (!net.isValid()) {
            key = count == 1 ? "action.logtech.core.network_invalid_singular" : "action.logtech.core.network_invalid";
            player.displayClientMessage(Component.translatable(key, count), true);
        } else if (!isPowered()) {
            player.displayClientMessage(
                    Component.translatable("action.logtech.core.network_no_energy", count, getEnergyStored(), getMaxEnergyStored()),
                    true
            );
        } else if (isOverloaded()) {
            player.displayClientMessage(
                    Component.translatable("action.logtech.core.network_overloaded", count, getTotalStored(), getCapacity()),
                    true
            );
        } else {
            key = count == 1 ? "action.logtech.core.network_valid_singular" : "action.logtech.core.network_valid";
            player.displayClientMessage(Component.translatable(key, count), true);
        }
    }

    /**
     * Inserta una cantidad de items en el Núcleo respetando la capacidad de la red.
     * @param key clave del item y sus componentes
     * @param amount cantidad a insertar (debe ser > 0)
     * @param simulate si es true, no modifica el almacenamiento
     * @return la cantidad insertada exitosamente
     */
    public long insert(ItemKey key, long amount, boolean simulate) {
        if (key == null || amount <= 0 || !isPowered()) {
            return 0;
        }

        Network net = getOrRecalculateNetwork();
        if (!net.isValid()) {
            return 0;
        }

        long capacity = getCapacity();
        long totalStored = getTotalStored();
        long remainingSpace = capacity - totalStored;
        if (remainingSpace <= 0) {
            return 0;
        }

        long toInsert = Math.min(amount, remainingSpace);
        if (!simulate) {
            storage.merge(key, toInsert, Long::sum);
            this.activityCounter.recordItemIn((int) Math.min(toInsert, Integer.MAX_VALUE));
            setChanged();
        }
        return toInsert;
    }

    /**
     * Extrae una cantidad de items del Núcleo.
     * @param key clave del item y sus componentes
     * @param amount cantidad solicitada a extraer (debe ser > 0)
     * @param simulate si es true, no modifica el almacenamiento
     * @return la cantidad realmente extraída
     */
    public long extract(ItemKey key, long amount, boolean simulate) {
        if (key == null || amount <= 0 || !isPowered()) {
            return 0;
        }

        long current = storage.getOrDefault(key, 0L);
        long toExtract = Math.min(current, amount);
        if (toExtract <= 0) {
            return 0;
        }

        if (!simulate) {
            if (current == toExtract) {
                storage.remove(key);
            } else {
                storage.put(key, current - toExtract);
            }
            setChanged();
        }
        return toExtract;
    }

    /**
     * Devuelve una vista de solo lectura del mapa de almacenamiento.
     */
    public Map<ItemKey, Long> getAll() {
        return Collections.unmodifiableMap(this.storage);
    }

    /**
     * Devuelve la cantidad almacenada de un tipo de item específico.
     */
    public long getAmount(ItemKey key) {
        return storage.getOrDefault(key, 0L);
    }

    public boolean isEmpty() {
        return storage.isEmpty();
    }

    public StorageContent getStorageContent() {
        List<StorageEntry> entries = storage.entrySet().stream()
                .map(e -> new StorageEntry(e.getKey(), e.getValue()))
                .toList();
        return new StorageContent(entries);
    }

    public void setStorageContent(StorageContent content) {
        this.storage.clear();
        if (content != null) {
            for (StorageEntry entry : content.entries()) {
                if (entry.amount() > 0) {
                    this.storage.put(entry.key(), entry.amount());
                }
            }
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", this.energyStorage.getEnergyStored());
        StorageContent content = getStorageContent();
        StorageContent.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), content)
                .resultOrPartial()
                .ifPresent(storageTag -> tag.put("storage", storageTag));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("energy")) {
            this.energyStorage.setEnergy(tag.getInt("energy"));
        }
        if (tag.contains("storage")) {
            StorageContent.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("storage"))
                    .resultOrPartial()
                    .ifPresent(this::setStorageContent);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModDataComponents.STORAGE_CONTENT.get(), getStorageContent());
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        StorageContent content = componentInput.get(ModDataComponents.STORAGE_CONTENT.get());
        if (content != null) {
            setStorageContent(content);
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("storage");
    }
}
