package io.github.chema280101.logtech.block;

import io.github.chema280101.logtech.machine.CentrifugeBlockEntity;
import io.github.chema280101.logtech.machine.CrusherBlockEntity;
import io.github.chema280101.logtech.machine.DrillBlockEntity;
import io.github.chema280101.logtech.machine.WasherBlockEntity;
import io.github.chema280101.logtech.network.ColdRetrievalPayload;
import io.github.chema280101.logtech.network.EnergyPriority;
import io.github.chema280101.logtech.network.Network;
import io.github.chema280101.logtech.network.NetworkManager;
import io.github.chema280101.logtech.network.NetworkMember;
import io.github.chema280101.logtech.network.TerminalActionPayload;
import io.github.chema280101.logtech.network.TerminalEnergyPayload;
import io.github.chema280101.logtech.network.TerminalStatsPayload;
import io.github.chema280101.logtech.network.TerminalSyncPayload;
import io.github.chema280101.logtech.network.TerminalTabPayload;
import io.github.chema280101.logtech.registry.ModBlocks;
import io.github.chema280101.logtech.registry.ModMenuTypes;
import io.github.chema280101.logtech.storage.ColdOrder;
import io.github.chema280101.logtech.storage.ColdRetrievalLogic;
import io.github.chema280101.logtech.storage.ItemKey;
import io.github.chema280101.logtech.storage.TerminalItemEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
    public static final int MAX_COLD_QUEUE_SIZE = 5;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final ServerPlayer serverPlayer;

    // Estado del servidor
    private int tickCounter = 0;
    private boolean initialSyncNeeded = true;
    private boolean lastSentValid = false;
    private boolean lastSentPowered = false;
    private Map<ItemKey, Long> lastSentHotStorage = null;
    private Map<ItemKey, Long> lastSentColdStorage = null;
    private Map<ItemKey, io.github.chema280101.logtech.storage.ItemPinMode> lastSentPolicies = null;
    private int activeTab = TerminalTabPayload.TAB_ITEMS;
    private int statsPacketsSent = 0;
    private int energyPacketsSent = 0;
    private final Queue<ColdOrder<ItemKey>> coldOrders = new ArrayDeque<>();
    private ColdOrder<ItemKey> currentColdOrder = null;

    // Estado del cliente
    private List<TerminalItemEntry> clientItems = Collections.emptyList();
    private final List<TerminalItemEntry> incomingBatchAccumulator = new ArrayList<>();
    private boolean clientNetworkValid = true;
    private boolean clientPowered = true;
    private Runnable onItemsUpdatedCallback;
    private TerminalStatsPayload clientStats = null;
    private Runnable onStatsUpdatedCallback;
    private TerminalEnergyPayload clientEnergy = null;
    private Runnable onEnergyUpdatedCallback;
    private ItemStack clientColdItem = ItemStack.EMPTY;
    private int clientColdAmount = 0;
    private int clientColdRemainingTicks = 0;
    private int clientColdTotalTicks = 0;
    private int clientColdQueueSize = 0;

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
        int reach;
        try {
            reach = io.github.chema280101.logtech.LogTechConfig.SCREEN_REACH_DISTANCE.get();
        } catch (Exception e) {
            reach = 8;
        }
        double maxDistSq = (double) reach * reach;
        return this.access.evaluate((level, pos) ->
                level.getBlockState(pos).is(ModBlocks.TERMINAL.get())
                        && player.distanceToSqr((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D) <= maxDistSq,
                true);
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
            case CYCLE_PIN_POLICY -> handleCyclePinPolicy(player, payload.item());
        }
    }

    private void handleCyclePinPolicy(ServerPlayer player, ItemStack targetItem) {
        if (targetItem == null || targetItem.isEmpty()) {
            return;
        }
        CoreBlockEntity coreBe = getCoreBlockEntity(player);
        if (coreBe == null) {
            return;
        }
        ItemKey key = ItemKey.of(targetItem);
        coreBe.cyclePinMode(key);
        triggerImmediateSync();
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
        int desired = (int) Math.min(stored, maxStack);

        long hotStored = coreBe.getAmount(key, io.github.chema280101.logtech.storage.StorageTier.HOT);
        int hotToExtract = (int) Math.min((long) desired, hotStored);
        long actuallyExtracted = 0;
        if (hotToExtract > 0) {
            actuallyExtracted = coreBe.extractHot(key, hotToExtract, false);
            if (actuallyExtracted > 0) {
                this.setCarried(key.toStack((int) actuallyExtracted));
                this.broadcastChanges();
                triggerImmediateSync();
            }
        }

        int remaining = desired - (int) actuallyExtracted;
        if (remaining > 0) {
            long coldStored = coreBe.getAmount(key, io.github.chema280101.logtech.storage.StorageTier.COLD);
            if (coldStored > 0) {
                enqueueColdOrder(key, remaining);
            }
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

        long hotStored = coreBe.getAmount(key, io.github.chema280101.logtech.storage.StorageTier.HOT);
        int hotToExtract = (int) Math.min((long) half, hotStored);
        long actuallyExtracted = 0;
        if (hotToExtract > 0) {
            actuallyExtracted = coreBe.extractHot(key, hotToExtract, false);
            if (actuallyExtracted > 0) {
                this.setCarried(key.toStack((int) actuallyExtracted));
                this.broadcastChanges();
                triggerImmediateSync();
            }
        }

        int remaining = half - (int) actuallyExtracted;
        if (remaining > 0) {
            long coldStored = coreBe.getAmount(key, io.github.chema280101.logtech.storage.StorageTier.COLD);
            if (coldStored > 0) {
                enqueueColdOrder(key, remaining);
            }
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

        long hotStored = coreBe.getAmount(key, io.github.chema280101.logtech.storage.StorageTier.HOT);
        int hotToExtract = (int) Math.min((long) toExtract, hotStored);
        long actuallyExtracted = 0;
        if (hotToExtract > 0) {
            actuallyExtracted = coreBe.extractHot(key, hotToExtract, false);
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

        int remaining = toExtract - (int) actuallyExtracted;
        if (remaining > 0) {
            long coldStored = coreBe.getAmount(key, io.github.chema280101.logtech.storage.StorageTier.COLD);
            if (coldStored > 0) {
                enqueueColdOrder(key, remaining);
            }
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
            this.lastSentHotStorage = Collections.emptyMap();
            this.lastSentColdStorage = Collections.emptyMap();
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
            this.lastSentHotStorage = Collections.emptyMap();
            this.lastSentColdStorage = Collections.emptyMap();
            PacketDistributor.sendToPlayer(
                    this.serverPlayer,
                    new TerminalSyncPayload(this.containerId, true, false, 0, 1, Collections.emptyList())
            );
            return;
        }

        this.lastSentValid = true;
        this.lastSentPowered = true;
        this.lastSentHotStorage = new HashMap<>(coreBe.getTierStorage(io.github.chema280101.logtech.storage.StorageTier.HOT).getAll());
        this.lastSentColdStorage = new HashMap<>(coreBe.getTierStorage(io.github.chema280101.logtech.storage.StorageTier.COLD).getAll());
        sendFullSync(coreBe);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (this.serverPlayer != null && !this.serverPlayer.level().isClientSide()) {
            serverTick();
        }
    }

    public void handleTabChange(int tab) {
        this.activeTab = tab;
        if (this.serverPlayer != null) {
            String tabName = switch (tab) {
                case TerminalTabPayload.TAB_STATS -> "ESTADÍSTICAS";
                case TerminalTabPayload.TAB_ENERGY -> "ENERGÍA";
                default -> "ÍTEMS";
            };
            LOGGER.info("[LogTech TabSync] Jugador {} cambió pestaña a: {}",
                    this.serverPlayer.getScoreboardName(), tabName);
        }
        if (tab == TerminalTabPayload.TAB_STATS) {
            sendStatsSync();
        } else if (tab == TerminalTabPayload.TAB_ENERGY) {
            sendEnergySync();
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.activeTab = TerminalTabPayload.TAB_ITEMS;
        this.coldOrders.clear();
        this.currentColdOrder = null;
    }

    private void serverTick() {
        if (!stillValid(this.serverPlayer)) {
            if (this.currentColdOrder != null || !this.coldOrders.isEmpty()) {
                this.coldOrders.clear();
                this.currentColdOrder = null;
            }
            return;
        }

        // Procesar pedidos pendientes del nivel frío
        if (this.currentColdOrder == null && !this.coldOrders.isEmpty()) {
            this.currentColdOrder = this.coldOrders.poll();
            sendColdRetrievalSync();
        }
        if (this.currentColdOrder != null) {
            this.currentColdOrder.tick();
            if (this.currentColdOrder.isComplete()) {
                fulfillColdOrder(this.currentColdOrder);
                this.currentColdOrder = !this.coldOrders.isEmpty() ? this.coldOrders.poll() : null;
                sendColdRetrievalSync();
            } else if (this.currentColdOrder.getRemainingTicks() % 5 == 0) {
                sendColdRetrievalSync();
            }
        }

        this.tickCounter++;

        // Sincronización de telemetría según el intervalo configurado SOLO mientras la pestaña correspondiente está abierta
        int statsInterval = io.github.chema280101.logtech.LogTechConfig.STATS_SYNC_INTERVAL.get();
        if (this.activeTab == TerminalTabPayload.TAB_STATS && (statsInterval <= 1 || this.tickCounter % statsInterval == 0)) {
            sendStatsSync();
        } else if (this.activeTab == TerminalTabPayload.TAB_ENERGY && (statsInterval <= 1 || this.tickCounter % statsInterval == 0)) {
            sendEnergySync();
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
            if (this.lastSentValid || this.lastSentPowered || this.lastSentHotStorage == null) {
                this.lastSentValid = false;
                this.lastSentPowered = false;
                this.lastSentHotStorage = Collections.emptyMap();
                this.lastSentColdStorage = Collections.emptyMap();
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
            if (!this.lastSentValid || this.lastSentPowered || this.lastSentHotStorage == null) {
                this.lastSentValid = true;
                this.lastSentPowered = false;
                this.lastSentHotStorage = Collections.emptyMap();
                this.lastSentColdStorage = Collections.emptyMap();
                PacketDistributor.sendToPlayer(
                        this.serverPlayer,
                        new TerminalSyncPayload(this.containerId, true, false, 0, 1, Collections.emptyList())
                );
            }
            return;
        }

        Map<ItemKey, Long> hotStorage = coreBe.getTierStorage(io.github.chema280101.logtech.storage.StorageTier.HOT).getAll();
        Map<ItemKey, Long> coldStorage = coreBe.getTierStorage(io.github.chema280101.logtech.storage.StorageTier.COLD).getAll();
        Map<ItemKey, io.github.chema280101.logtech.storage.ItemPinMode> policies = coreBe.getPolicyManager().getPinnedPolicies();
        if (!this.lastSentValid || !this.lastSentPowered
                || !hotStorage.equals(this.lastSentHotStorage)
                || !coldStorage.equals(this.lastSentColdStorage)
                || !policies.equals(this.lastSentPolicies)) {
            this.lastSentValid = true;
            this.lastSentPowered = true;
            this.lastSentHotStorage = new HashMap<>(hotStorage);
            this.lastSentColdStorage = new HashMap<>(coldStorage);
            this.lastSentPolicies = new HashMap<>(policies);
            sendFullSync(coreBe);
        }
    }

    private void sendFullSync(CoreBlockEntity coreBe) {
        Map<ItemKey, Long> storage = coreBe.getAll();
        List<TerminalItemEntry> entries = new ArrayList<>(storage.size());
        for (Map.Entry<ItemKey, Long> entry : storage.entrySet()) {
            ItemStack stack = entry.getKey().toStack(1);
            long hotAmount = coreBe.getAmount(entry.getKey(), io.github.chema280101.logtech.storage.StorageTier.HOT);
            long coldAmount = coreBe.getAmount(entry.getKey(), io.github.chema280101.logtech.storage.StorageTier.COLD);
            io.github.chema280101.logtech.storage.ItemPinMode pinMode = coreBe.getPinMode(entry.getKey());
            entries.add(new TerminalItemEntry(stack, entry.getValue(), hotAmount, coldAmount, pinMode));
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

    public void sendEnergySync() {
        if (this.serverPlayer == null || this.serverPlayer.level().isClientSide()) {
            return;
        }
        Level level = this.serverPlayer.serverLevel();
        NetworkManager manager = NetworkManager.get(level);
        Network network = manager != null ? manager.scan(this.pos, null) : Network.EMPTY;

        if (!network.isValid()) {
            PacketDistributor.sendToPlayer(
                    this.serverPlayer,
                    new TerminalEnergyPayload(
                            this.containerId,
                            0, 0, 0, 0,
                            new int[60],
                            new int[60],
                            0L, 0L,
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

        int energyStored = coreBe.getEnergyStored();
        int energyCapacity = coreBe.getMaxEnergyStored();
        int energyReceivedPerMinute = coreBe.getEnergyReceivedCounter().getEnergyReceived(1);

        int coreConsumedPerMinute = coreBe.getActivityCounter().getEnergyConsumed(1);
        long storageBranchConsumption = coreConsumedPerMinute;
        long mineralsBranchConsumption = 0L;

        List<NetworkMember> members = coreBe.getMembers();
        List<TerminalEnergyPayload.MachineEnergyEntry> machineEntries = new ArrayList<>();

        // Incluimos el Núcleo en la lista de consumidores (como rama de almacenamiento)
        machineEntries.add(new TerminalEnergyPayload.MachineEnergyEntry(
                coreBe.getBlockPos(),
                Component.translatable("block.logtech.core"),
                coreConsumedPerMinute,
                EnergyPriority.HIGH,
                true
        ));

        int memberConsumedTotal = 0;
        for (NetworkMember member : members) {
            int mConsumed = member.getActivityCounter().getEnergyConsumed(1);
            memberConsumedTotal += mConsumed;

            if (member instanceof CrusherBlockEntity || member instanceof WasherBlockEntity
                    || member instanceof CentrifugeBlockEntity || member instanceof DrillBlockEntity) {
                mineralsBranchConsumption += mConsumed;
            }

            machineEntries.add(new TerminalEnergyPayload.MachineEnergyEntry(
                    member.getBlockPos(),
                    member.getName(),
                    mConsumed,
                    member.getEnergyPriority(),
                    false
            ));
        }

        int energyConsumedPerMinute = coreConsumedPerMinute + memberConsumedTotal;

        // Historial de los últimos 60 minutos
        int[] receivedHistory = new int[60];
        int[] consumedHistory = new int[60];
        for (int i = 0; i < 60; i++) {
            int minsAgo = 59 - i;
            receivedHistory[i] = coreBe.getEnergyReceivedCounter().getBucket(minsAgo).getEnergyReceived();

            int totalMinConsumed = coreBe.getActivityCounter().getBucket(minsAgo).getEnergyConsumed();
            for (NetworkMember member : members) {
                totalMinConsumed += member.getActivityCounter().getBucket(minsAgo).getEnergyConsumed();
            }
            consumedHistory[i] = totalMinConsumed;
        }

        // Ordenar de mayor a menor consumo, luego alfabéticamente
        machineEntries.sort((a, b) -> {
            int cmp = Integer.compare(b.energyConsumed(), a.energyConsumed());
            if (cmp != 0) return cmp;
            return a.name().getString().compareToIgnoreCase(b.name().getString());
        });

        this.energyPacketsSent++;
        PacketDistributor.sendToPlayer(
                this.serverPlayer,
                new TerminalEnergyPayload(
                        this.containerId,
                        energyStored,
                        energyCapacity,
                        energyReceivedPerMinute,
                        energyConsumedPerMinute,
                        receivedHistory,
                        consumedHistory,
                        storageBranchConsumption,
                        mineralsBranchConsumption,
                        machineEntries
                )
        );
    }

    public void handleChangeMachinePriority(ServerPlayer player, BlockPos machinePos, EnergyPriority newPriority) {
        if (!stillValid(player) || machinePos == null || newPriority == null) {
            return;
        }
        Level level = player.serverLevel();
        NetworkManager manager = NetworkManager.get(level);
        Network network = manager != null ? manager.scan(this.pos, null) : Network.EMPTY;
        if (!network.isValid() || !network.getMachines().contains(machinePos)) {
            return;
        }
        if (level.hasChunkAt(machinePos)) {
            BlockEntity be = level.getBlockEntity(machinePos);
            if (be instanceof NetworkMember member && !be.isRemoved()) {
                member.setEnergyPriority(newPriority);
                be.setChanged();
                sendEnergySync();
            }
        }
    }

    // --- Métodos de cliente ---

    public void setOnItemsUpdatedCallback(Runnable callback) {
        this.onItemsUpdatedCallback = callback;
    }

    public void setOnStatsUpdatedCallback(Runnable callback) {
        this.onStatsUpdatedCallback = callback;
    }

    public void setOnEnergyUpdatedCallback(Runnable callback) {
        this.onEnergyUpdatedCallback = callback;
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

    public void receiveEnergyStats(TerminalEnergyPayload energy) {
        this.clientEnergy = energy;
        if (this.onEnergyUpdatedCallback != null) {
            this.onEnergyUpdatedCallback.run();
        }
    }

    public List<TerminalItemEntry> getClientItems() {
        return this.clientItems;
    }

    public TerminalStatsPayload getClientStats() {
        return this.clientStats;
    }

    public TerminalEnergyPayload getClientEnergy() {
        return this.clientEnergy;
    }

    public boolean isNetworkValid() {
        return this.clientNetworkValid;
    }

    public boolean isPowered() {
        return this.clientPowered;
    }

    // --- Gestión de pedidos desde el nivel frío ---

    private void enqueueColdOrder(ItemKey key, int amount) {
        if (this.serverPlayer == null || amount <= 0) {
            return;
        }
        int maxItems = io.github.chema280101.logtech.LogTechConfig.COLD_RETRIEVAL_MAX_ITEMS.get();
        int boundedAmount = Math.min(amount, maxItems);
        int delay = io.github.chema280101.logtech.LogTechConfig.COLD_RETRIEVAL_DELAY_TICKS.get();

        if (delay <= 0) {
            // Sin retraso configurado: entrega inmediata directa
            CoreBlockEntity coreBe = getCoreBlockEntity(this.serverPlayer);
            if (coreBe != null) {
                ItemStack sample = key.toStack(boundedAmount);
                int space = calculateInventorySpace(sample, 0, 36);
                if (space < boundedAmount) {
                    this.serverPlayer.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("gui.logtech.terminal.cold_inventory_full"), true
                    );
                    return;
                }
                long extracted = coreBe.extractCold(key, boundedAmount, false);
                if (extracted > 0) {
                    ItemStack delivered = key.toStack((int) extracted);
                    this.moveItemStackTo(delivered, 0, 36, false);
                    if (!delivered.isEmpty()) {
                        coreBe.insert(key, delivered.getCount(), false);
                    }
                    this.broadcastChanges();
                    triggerImmediateSync();
                }
            }
            return;
        }

        if (this.coldOrders.size() >= MAX_COLD_QUEUE_SIZE) {
            this.serverPlayer.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("gui.logtech.terminal.cold_queue_full"), true
            );
            return;
        }

        ColdOrder<ItemKey> order = new ColdOrder<>(key, boundedAmount, delay);
        if (this.currentColdOrder == null) {
            this.currentColdOrder = order;
            sendColdRetrievalSync();
        } else {
            this.coldOrders.add(order);
            sendColdRetrievalSync();
        }
    }

    private void fulfillColdOrder(ColdOrder<ItemKey> order) {
        if (this.serverPlayer == null || !stillValid(this.serverPlayer)) {
            return;
        }
        CoreBlockEntity coreBe = getCoreBlockEntity(this.serverPlayer);
        if (coreBe == null) {
            this.serverPlayer.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("gui.logtech.terminal.no_energy"), true
            );
            return;
        }

        ItemKey key = order.getKey();
        long availableInCold = coreBe.getAmount(key, io.github.chema280101.logtech.storage.StorageTier.COLD);

        ItemStack sample = key.toStack(order.getRequestedAmount());
        int availableSpace = calculateInventorySpace(sample, 0, 36);

        ColdRetrievalLogic.DeliveryResult result =
                ColdRetrievalLogic.evaluateDelivery(order, availableInCold, availableSpace);

        switch (result.status()) {
            case OUT_OF_STOCK -> {
                this.serverPlayer.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("gui.logtech.terminal.cold_out_of_stock"), true
                );
            }
            case INVENTORY_FULL -> {
                this.serverPlayer.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("gui.logtech.terminal.cold_inventory_full"), true
                );
            }
            case SUCCESS -> {
                int toDeliver = result.deliverableAmount();
                long extracted = coreBe.extractCold(key, toDeliver, false);
                if (extracted > 0) {
                    ItemStack deliveredStack = key.toStack((int) extracted);
                    this.moveItemStackTo(deliveredStack, 0, 36, false);
                    if (!deliveredStack.isEmpty()) {
                        coreBe.insert(key, deliveredStack.getCount(), false);
                    }
                    this.broadcastChanges();
                    triggerImmediateSync();
                }
            }
        }
    }

    private void sendColdRetrievalSync() {
        if (this.serverPlayer == null || this.serverPlayer.level().isClientSide()) {
            return;
        }
        if (this.currentColdOrder == null) {
            PacketDistributor.sendToPlayer(
                    this.serverPlayer,
                    new ColdRetrievalPayload(this.containerId, ItemStack.EMPTY, 0, 0, 0, 0)
            );
        } else {
            ItemStack displayStack = this.currentColdOrder.getKey().toStack(this.currentColdOrder.getRequestedAmount());
            PacketDistributor.sendToPlayer(
                    this.serverPlayer,
                    new ColdRetrievalPayload(
                            this.containerId,
                            displayStack,
                            this.currentColdOrder.getRequestedAmount(),
                            this.currentColdOrder.getRemainingTicks(),
                            this.currentColdOrder.getTotalTicks(),
                            this.coldOrders.size() + 1
                    )
            );
        }
    }

    public void handleColdRetrievalUpdate(ColdRetrievalPayload payload) {
        this.clientColdItem = payload.item();
        this.clientColdAmount = payload.amount();
        this.clientColdRemainingTicks = payload.remainingTicks();
        this.clientColdTotalTicks = payload.totalTicks();
        this.clientColdQueueSize = payload.queueSize();
    }

    public boolean hasActiveColdRetrieval() {
        return this.clientColdAmount > 0 && !this.clientColdItem.isEmpty();
    }

    public ItemStack getClientColdItem() {
        return this.clientColdItem;
    }

    public int getClientColdAmount() {
        return this.clientColdAmount;
    }

    public int getClientColdRemainingTicks() {
        return this.clientColdRemainingTicks;
    }

    public int getClientColdTotalTicks() {
        return this.clientColdTotalTicks;
    }

    public int getClientColdQueueSize() {
        return this.clientColdQueueSize;
    }

    public void clientTick() {
        if (this.clientColdRemainingTicks > 0) {
            this.clientColdRemainingTicks--;
        }
    }
}
