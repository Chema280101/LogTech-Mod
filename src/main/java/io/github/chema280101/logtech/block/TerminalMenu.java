package io.github.chema280101.logtech.block;

import io.github.chema280101.logtech.network.Network;
import io.github.chema280101.logtech.network.NetworkManager;
import io.github.chema280101.logtech.network.NetworkMember;
import io.github.chema280101.logtech.network.TerminalActionPayload;
import io.github.chema280101.logtech.network.TerminalStatsPayload;
import io.github.chema280101.logtech.network.TerminalSyncPayload;
import io.github.chema280101.logtech.registry.ModBlocks;
import io.github.chema280101.logtech.registry.ModMenuTypes;
import io.github.chema280101.logtech.storage.ItemKey;
import io.github.chema280101.logtech.storage.TerminalItemEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class TerminalMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int BATCH_SIZE = 50;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final ServerPlayer serverPlayer;

    // Estado del servidor
    private int tickCounter = 0;
    private boolean initialSyncNeeded = true;
    private boolean lastSentValid = false;
    private boolean lastSentPowered = false;
    private Map<ItemKey, Long> lastSentStorage = null;
    private boolean statsTabOpen = false;
    private int statsPacketsSent = 0;

    // Estado del cliente
    private List<TerminalItemEntry> clientItems = Collections.emptyList();
    private final List<TerminalItemEntry> incomingBatchAccumulator = new ArrayList<>();
    private boolean clientNetworkValid = true;
    private boolean clientPowered = true;
    private Runnable onItemsUpdatedCallback;
    private TerminalStatsPayload clientStats = null;
    private Runnable onStatsUpdatedCallback;

    /**
     * Constructor para el cliente (creado vía IMenuTypeExtension).
     */
    public TerminalMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos());
    }

    /**
     * Constructor común/servidor con la posición del bloque Terminal.
     */
    public TerminalMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.TERMINAL.get(), containerId);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.serverPlayer = playerInventory.player instanceof ServerPlayer sp ? sp : null;

        // Ranuras del inventario del jugador (3 filas x 9 columnas) en la parte inferior
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        // Barra de acceso rápido (hotbar: 1 fila x 9 columnas)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.TERMINAL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return ItemStack.EMPTY;
        }
        CoreBlockEntity coreBe = getCoreBlockEntity(serverPlayer);
        if (coreBe == null || !coreBe.isPowered()) {
            return ItemStack.EMPTY;
        }
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack slotStack = slot.getItem();
        ItemStack original = slotStack.copy();

        handleInsertSlot(serverPlayer, index);

        if (slot.getItem().getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        return original;
    }

    public void handleAction(ServerPlayer player, TerminalActionPayload payload) {
        if (!stillValid(player)) {
            return;
        }
        switch (payload.actionType()) {
            case EXTRACT_STACK -> handleExtractStack(player, payload.item());
            case EXTRACT_HALF -> handleExtractHalf(player, payload.item());
            case EXTRACT_SHIFT -> handleExtractShift(player, payload.item());
            case INSERT_CURSOR_ALL -> handleInsertCursor(player, false);
            case INSERT_CURSOR_ONE -> handleInsertCursor(player, true);
            case INSERT_SLOT -> handleInsertSlot(player, payload.slotIndex());
        }
    }

    private CoreBlockEntity getCoreBlockEntity(ServerPlayer player) {
        if (!stillValid(player)) {
            return null;
        }
        Level level = player.serverLevel();
        NetworkManager manager = NetworkManager.get(level);
        if (manager == null) {
            return null;
        }
        Network network = manager.scan(this.pos, null);
        if (!network.isValid()) {
            return null;
        }
        BlockPos corePos = network.getCores().iterator().next();
        if (!level.hasChunkAt(corePos)) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(corePos);
        return be instanceof CoreBlockEntity coreBe && coreBe.isPowered() ? coreBe : null;
    }

    private void handleExtractStack(ServerPlayer player, ItemStack targetItem) {
        if (targetItem.isEmpty()) {
            return;
        }
        if (!this.getCarried().isEmpty()) {
            handleInsertCursor(player, false);
            return;
        }
        CoreBlockEntity coreBe = getCoreBlockEntity(player);
        if (coreBe == null) {
            return;
        }
        ItemKey key = ItemKey.of(targetItem);
        long stored = coreBe.getAmount(key);
        if (stored <= 0) {
            return;
        }
        int maxStack = targetItem.getMaxStackSize();
        int toExtract = (int) Math.min(stored, maxStack);
        long actuallyExtracted = coreBe.extract(key, toExtract, false);
        if (actuallyExtracted > 0) {
            this.setCarried(key.toStack((int) actuallyExtracted));
            this.broadcastChanges();
            triggerImmediateSync();
        }
    }

    private void handleExtractHalf(ServerPlayer player, ItemStack targetItem) {
        if (targetItem.isEmpty()) {
            return;
        }
        if (!this.getCarried().isEmpty()) {
            handleInsertCursor(player, true);
            return;
        }
        CoreBlockEntity coreBe = getCoreBlockEntity(player);
        if (coreBe == null) {
            return;
        }
        ItemKey key = ItemKey.of(targetItem);
        long stored = coreBe.getAmount(key);
        if (stored <= 0) {
            return;
        }
        int maxStack = targetItem.getMaxStackSize();
        int availableInStack = (int) Math.min(stored, maxStack);
        int half = (availableInStack + 1) / 2;
        long actuallyExtracted = coreBe.extract(key, half, false);
        if (actuallyExtracted > 0) {
            this.setCarried(key.toStack((int) actuallyExtracted));
            this.broadcastChanges();
            triggerImmediateSync();
        }
    }

    private void handleExtractShift(ServerPlayer player, ItemStack targetItem) {
        if (targetItem.isEmpty()) {
            return;
        }
        CoreBlockEntity coreBe = getCoreBlockEntity(player);
        if (coreBe == null) {
            return;
        }
        ItemKey key = ItemKey.of(targetItem);
        long stored = coreBe.getAmount(key);
        if (stored <= 0) {
            return;
        }
        int maxStack = targetItem.getMaxStackSize();
        int desired = (int) Math.min(stored, maxStack);
        ItemStack sample = key.toStack(desired);

        int space = calculateInventorySpace(sample, 0, 36);
        int toExtract = Math.min(desired, space);
        if (toExtract <= 0) {
            return;
        }

        long actuallyExtracted = coreBe.extract(key, toExtract, false);
        if (actuallyExtracted > 0) {
            ItemStack toMove = key.toStack((int) actuallyExtracted);
            this.moveItemStackTo(toMove, 0, 36, false);
            if (!toMove.isEmpty()) {
                coreBe.insert(key, toMove.getCount(), false);
            }
            this.broadcastChanges();
            triggerImmediateSync();
        }
    }

    private int calculateInventorySpace(ItemStack stack, int startIndex, int endIndex) {
        int space = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Slot slot = this.slots.get(i);
            ItemStack slotStack = slot.getItem();
            if (slotStack.isEmpty()) {
                space += Math.min(slot.getMaxStackSize(), stack.getMaxStackSize());
            } else if (ItemStack.isSameItemSameComponents(slotStack, stack)) {
                int slotMax = Math.min(slot.getMaxStackSize(slotStack), slotStack.getMaxStackSize());
                space += Math.max(0, slotMax - slotStack.getCount());
            }
        }
        return space;
    }

    private void handleInsertCursor(ServerPlayer player, boolean oneOnly) {
        ItemStack carried = this.getCarried();
        if (carried.isEmpty()) {
            return;
        }
        CoreBlockEntity coreBe = getCoreBlockEntity(player);
        if (coreBe == null) {
            return;
        }
        ItemKey key = ItemKey.of(carried);
        long toInsert = oneOnly ? 1 : carried.getCount();
        long inserted = coreBe.insert(key, toInsert, false);
        if (inserted > 0) {
            carried.shrink((int) inserted);
            this.setCarried(carried);
            this.broadcastChanges();
            triggerImmediateSync();
        }
    }

    public void handleInsertSlot(ServerPlayer player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.slots.size()) {
            return;
        }
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return;
        }
        CoreBlockEntity coreBe = getCoreBlockEntity(player);
        if (coreBe == null) {
            return;
        }
        ItemStack slotStack = slot.getItem();
        ItemKey key = ItemKey.of(slotStack);
        long inserted = coreBe.insert(key, slotStack.getCount(), false);
        if (inserted > 0) {
            slotStack.shrink((int) inserted);
            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            this.broadcastChanges();
            triggerImmediateSync();
        }
    }

    private void triggerImmediateSync() {
        if (this.serverPlayer == null || this.serverPlayer.level().isClientSide()) {
            return;
        }
        Level level = this.serverPlayer.serverLevel();
        NetworkManager manager = NetworkManager.get(level);
        Network network = manager != null ? manager.scan(this.pos, null) : Network.EMPTY;
        if (!network.isValid()) {
            this.lastSentValid = false;
            this.lastSentPowered = false;
            this.lastSentStorage = Collections.emptyMap();
            PacketDistributor.sendToPlayer(
                    this.serverPlayer,
                    new TerminalSyncPayload(this.containerId, false, false, 0, 1, Collections.emptyList())
            );
            return;
        }

        BlockPos corePos = network.getCores().iterator().next();
        if (!level.hasChunkAt(corePos)) {
            return;
        }
        BlockEntity be = level.getBlockEntity(corePos);
        if (!(be instanceof CoreBlockEntity coreBe)) {
            return;
        }

        if (!coreBe.isPowered()) {
            this.lastSentValid = true;
            this.lastSentPowered = false;
            this.lastSentStorage = Collections.emptyMap();
            PacketDistributor.sendToPlayer(
                    this.serverPlayer,
                    new TerminalSyncPayload(this.containerId, true, false, 0, 1, Collections.emptyList())
            );
            return;
        }

        Map<ItemKey, Long> currentStorage = coreBe.getAll();
        this.lastSentValid = true;
        this.lastSentPowered = true;
        this.lastSentStorage = new HashMap<>(currentStorage);
        sendFullSync(currentStorage);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (this.serverPlayer != null && !this.serverPlayer.level().isClientSide()) {
            serverTick();
        }
    }

    public void handleTabChange(boolean statsTabOpen) {
        this.statsTabOpen = statsTabOpen;
        if (this.serverPlayer != null) {
            LOGGER.info("[LogTech StatsSync] Jugador {} cambió pestaña de estadísticas: {}",
                    this.serverPlayer.getScoreboardName(), statsTabOpen ? "ABIERTA" : "CERRADA");
        }
        if (statsTabOpen) {
            sendStatsSync();
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.statsTabOpen = false;
    }

    private void serverTick() {
        if (!stillValid(this.serverPlayer)) {
            return;
        }

        this.tickCounter++;

        // Sincronización de estadísticas según el intervalo configurado SOLO mientras la pestaña de estadísticas está abierta
        int statsInterval = io.github.chema280101.logtech.LogTechConfig.STATS_SYNC_INTERVAL.get();
        if (this.statsTabOpen && (statsInterval <= 1 || this.tickCounter % statsInterval == 0)) {
            sendStatsSync();
        }

        // Sincronizar en el primer tick y cada 10 ticks (0.5 segundos) solo si cambió algo
        if (this.tickCounter % 10 != 0 && !this.initialSyncNeeded) {
            return;
        }
        this.initialSyncNeeded = false;

        Level level = this.serverPlayer.serverLevel();
        NetworkManager manager = NetworkManager.get(level);
        Network network = manager != null ? manager.scan(this.pos, null) : Network.EMPTY;

        if (!network.isValid()) {
            if (this.lastSentValid || this.lastSentPowered || this.lastSentStorage == null) {
                this.lastSentValid = false;
                this.lastSentPowered = false;
                this.lastSentStorage = Collections.emptyMap();
                PacketDistributor.sendToPlayer(
                        this.serverPlayer,
                        new TerminalSyncPayload(this.containerId, false, false, 0, 1, Collections.emptyList())
                );
            }
            return;
        }

        BlockPos corePos = network.getCores().iterator().next();
        if (!level.hasChunkAt(corePos)) {
            return;
        }

        BlockEntity be = level.getBlockEntity(corePos);
        if (!(be instanceof CoreBlockEntity coreBe)) {
            return;
        }

        boolean powered = coreBe.isPowered();
        if (!powered) {
            if (!this.lastSentValid || this.lastSentPowered || this.lastSentStorage == null) {
                this.lastSentValid = true;
                this.lastSentPowered = false;
                this.lastSentStorage = Collections.emptyMap();
                PacketDistributor.sendToPlayer(
                        this.serverPlayer,
                        new TerminalSyncPayload(this.containerId, true, false, 0, 1, Collections.emptyList())
                );
            }
            return;
        }

        Map<ItemKey, Long> currentStorage = coreBe.getAll();
        if (!this.lastSentValid || !this.lastSentPowered || !currentStorage.equals(this.lastSentStorage)) {
            this.lastSentValid = true;
            this.lastSentPowered = true;
            this.lastSentStorage = new HashMap<>(currentStorage);
            sendFullSync(currentStorage);
        }
    }

    private void sendFullSync(Map<ItemKey, Long> storage) {
        List<TerminalItemEntry> entries = new ArrayList<>(storage.size());
        for (Map.Entry<ItemKey, Long> entry : storage.entrySet()) {
            ItemStack stack = entry.getKey().toStack(1);
            entries.add(new TerminalItemEntry(stack, entry.getValue()));
        }

        // Ordenar por cantidad descendente, luego nombre alfabético
        entries.sort((a, b) -> {
            int cmp = Long.compare(b.amount(), a.amount());
            if (cmp != 0) {
                return cmp;
            }
            return a.stack().getHoverName().getString().compareToIgnoreCase(b.stack().getHoverName().getString());
        });

        if (entries.isEmpty()) {
            PacketDistributor.sendToPlayer(
                    this.serverPlayer,
                    new TerminalSyncPayload(this.containerId, true, true, 0, 1, Collections.emptyList())
            );
            return;
        }

        int totalBatches = (entries.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        for (int i = 0; i < totalBatches; i++) {
            int from = i * BATCH_SIZE;
            int to = Math.min(from + BATCH_SIZE, entries.size());
            List<TerminalItemEntry> batch = entries.subList(from, to);
            PacketDistributor.sendToPlayer(
                    this.serverPlayer,
                    new TerminalSyncPayload(this.containerId, true, true, i, totalBatches, batch)
            );
        }
    }

    public void sendStatsSync() {
        if (this.serverPlayer == null || this.serverPlayer.level().isClientSide()) {
            return;
        }
        Level level = this.serverPlayer.serverLevel();
        NetworkManager manager = NetworkManager.get(level);
        Network network = manager != null ? manager.scan(this.pos, null) : Network.EMPTY;

        if (!network.isValid()) {
            PacketDistributor.sendToPlayer(
                    this.serverPlayer,
                    new TerminalStatsPayload(
                            this.containerId,
                            0L, 0L, 0, 0.0f, 0, 0, 0,
                            new int[60],
                            Collections.emptyList()
                    )
            );
            return;
        }

        BlockPos corePos = network.getCores().iterator().next();
        if (!level.hasChunkAt(corePos)) {
            return;
        }

        BlockEntity be = level.getBlockEntity(corePos);
        if (!(be instanceof CoreBlockEntity coreBe)) {
            return;
        }

        long usedCapacity = coreBe.getTotalStored();
        long totalCapacity = coreBe.getCapacity();
        int itemsPerMinLast = coreBe.getActivityCounter().getItemsIn(1);
        float itemsPerMin5MinAvg = coreBe.getActivityCounter().getItemsIn(5) / 5.0f;
        int energyStored = coreBe.getEnergyStored();
        int energyCapacity = coreBe.getMaxEnergyStored();
        int energyConsumption = coreBe.isPowered() ? coreBe.getEnergyConsumptionPerTick() : 0;

        int[] hourlyHistory = new int[60];
        for (int i = 0; i < 60; i++) {
            hourlyHistory[i] = coreBe.getActivityCounter().getBucket(59 - i).getItemsIn();
        }

        List<NetworkMember> members = coreBe.getMembers();
        List<TerminalStatsPayload.MachineStatEntry> machineEntries = new ArrayList<>();
        for (NetworkMember member : members) {
            int mItemsPerMin = Math.max(member.getActivityCounter().getItemsOut(1), member.getActivityCounter().getItemsIn(1));
            machineEntries.add(new TerminalStatsPayload.MachineStatEntry(
                    member.getName(),
                    member.getStatus(),
                    mItemsPerMin
            ));
        }

        machineEntries.sort((a, b) -> {
            int cmp = Integer.compare(b.itemsPerMinute(), a.itemsPerMinute());
            if (cmp != 0) return cmp;
            return a.name().getString().compareToIgnoreCase(b.name().getString());
        });

        if (machineEntries.size() > 10) {
            machineEntries = new ArrayList<>(machineEntries.subList(0, 10));
        }

        this.statsPacketsSent++;
        LOGGER.info("[LogTech StatsSync] Enviando estadísticas a {} (paquetes enviados: {})",
                this.serverPlayer.getScoreboardName(), this.statsPacketsSent);

        PacketDistributor.sendToPlayer(
                this.serverPlayer,
                new TerminalStatsPayload(
                        this.containerId,
                        usedCapacity,
                        totalCapacity,
                        itemsPerMinLast,
                        itemsPerMin5MinAvg,
                        energyStored,
                        energyCapacity,
                        energyConsumption,
                        hourlyHistory,
                        machineEntries
                )
        );
    }

    // --- Métodos de cliente ---

    public void setOnItemsUpdatedCallback(Runnable callback) {
        this.onItemsUpdatedCallback = callback;
    }

    public void setOnStatsUpdatedCallback(Runnable callback) {
        this.onStatsUpdatedCallback = callback;
    }

    public void receiveSync(boolean networkValid, boolean powered, int batchIndex, int totalBatches, List<TerminalItemEntry> items) {
        if (batchIndex == 0) {
            this.incomingBatchAccumulator.clear();
        }
        this.incomingBatchAccumulator.addAll(items);
        if (batchIndex >= totalBatches - 1) {
            this.clientItems = new ArrayList<>(this.incomingBatchAccumulator);
            this.clientNetworkValid = networkValid;
            this.clientPowered = powered;
            if (this.onItemsUpdatedCallback != null) {
                this.onItemsUpdatedCallback.run();
            }
        }
    }

    public void receiveStats(TerminalStatsPayload stats) {
        this.clientStats = stats;
        if (this.onStatsUpdatedCallback != null) {
            this.onStatsUpdatedCallback.run();
        }
    }

    public List<TerminalItemEntry> getClientItems() {
        return this.clientItems;
    }

    public TerminalStatsPayload getClientStats() {
        return this.clientStats;
    }

    public boolean isNetworkValid() {
        return this.clientNetworkValid;
    }

    public boolean isPowered() {
        return this.clientPowered;
    }
}
