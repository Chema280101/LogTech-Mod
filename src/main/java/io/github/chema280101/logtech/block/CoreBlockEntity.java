package io.github.chema280101.logtech.block;

import io.github.chema280101.logtech.LogTechConfig;
import io.github.chema280101.logtech.registry.ModBlockEntities;
import io.github.chema280101.logtech.registry.ModDataComponents;
import io.github.chema280101.logtech.storage.ItemKey;
import io.github.chema280101.logtech.storage.StorageContent;
import io.github.chema280101.logtech.storage.StorageEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
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
import org.jetbrains.annotations.Nullable;

public class CoreBlockEntity extends BlockEntity implements Configurable {
    public static final int ENERGY_CAPACITY = 200_000;
    public static final int ENERGY_MAX_RECEIVE = Integer.MAX_VALUE;
    public static final int ENERGY_TICK_INTERVAL = 20;

    private long energyReceivedTotal = 0L;
    private long energyDistributedTotal = 0L;
    private long energyConsumedSelfTotal = 0L;

    private final io.github.chema280101.logtech.storage.TierStorage hotTier =
            new io.github.chema280101.logtech.storage.TierStorage(io.github.chema280101.logtech.storage.StorageTier.HOT);
    private final io.github.chema280101.logtech.storage.TierStorage coldTier =
            new io.github.chema280101.logtech.storage.TierStorage(io.github.chema280101.logtech.storage.StorageTier.COLD);
    private final io.github.chema280101.logtech.storage.TierPolicyManager policyManager =
            new io.github.chema280101.logtech.storage.TierPolicyManager();
    private final io.github.chema280101.logtech.storage.RuleManager ruleManager =
            new io.github.chema280101.logtech.storage.RuleManager();
    private final io.github.chema280101.logtech.storage.TrashManager trashManager =
            new io.github.chema280101.logtech.storage.TrashManager();
    private final io.github.chema280101.logtech.storage.QuarantineManager quarantineManager =
            new io.github.chema280101.logtech.storage.QuarantineManager();
    private final io.github.chema280101.logtech.storage.ViewManager viewManager =
            new io.github.chema280101.logtech.storage.ViewManager();
    private final io.github.chema280101.logtech.security.NetworkSecurityManager securityManager =
            new io.github.chema280101.logtech.security.NetworkSecurityManager();
    private final io.github.chema280101.logtech.security.ActivityLogBuffer<String> activityLog =
            new io.github.chema280101.logtech.security.ActivityLogBuffer<>();
    private final io.github.chema280101.logtech.storage.RateTracker<ItemKey> rateTracker =
            new io.github.chema280101.logtech.storage.RateTracker<>();
    private final ActivityCounter activityCounter = new ActivityCounter();
    private final ActivityCounter energyReceivedCounter = new ActivityCounter();
    private final CoreEnergyStorage energyStorage = new CoreEnergyStorage(
            ENERGY_CAPACITY,
            ENERGY_MAX_RECEIVE,
            0,
            this::setChanged,
            this::onEnergyReceived
    );
    private int energyTickCounter = 0;
    private int policyTickCounter = 0;
    private int ruleTickCounter = 0;
    private int quarantineTickCounter = 0;
    private static int getHoldCyclesConfig() {
        try {
            return io.github.chema280101.logtech.LogTechConfig.ENERGY_PRIORITY_HOLD_CYCLES.get();
        } catch (Exception e) {
            return 3;
        }
    }

    private final io.github.chema280101.logtech.network.PriorityEnergyDistributor<NetworkMember> energyDistributor =
            new io.github.chema280101.logtech.network.PriorityEnergyDistributor<>(getHoldCyclesConfig());
    private Network network = null;

    public CoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE.get(), pos, state);
    }

    public int getEnergyTickInterval() {
        try {
            return io.github.chema280101.logtech.LogTechConfig.ENERGY_DISTRIBUTION_INTERVAL.get();
        } catch (Exception e) {
            return ENERGY_TICK_INTERVAL;
        }
    }

    public int getRuleNotificationRange() {
        try {
            return io.github.chema280101.logtech.LogTechConfig.RULE_NOTIFICATION_RANGE.get();
        } catch (Exception e) {
            return 64;
        }
    }

    public io.github.chema280101.logtech.storage.TrashManager getTrashManager() {
        return this.trashManager;
    }

    public io.github.chema280101.logtech.storage.TrashBin<ItemKey> getTrashBin() {
        return this.trashManager;
    }

    public int getTrashCapacity() {
        try {
            return io.github.chema280101.logtech.LogTechConfig.TRASH_CAPACITY.get();
        } catch (Exception e) {
            return 4096;
        }
    }

    public int getTrashLifetimeTicks() {
        try {
            return io.github.chema280101.logtech.LogTechConfig.TRASH_LIFETIME_TICKS.get();
        } catch (Exception e) {
            return 36000;
        }
    }

    public io.github.chema280101.logtech.storage.QuarantineManager getQuarantineManager() {
        return this.quarantineManager;
    }

    public io.github.chema280101.logtech.storage.ViewManager getViewManager() {
        return this.viewManager;
    }

    public int getMaxSavedViews() {
        try {
            return io.github.chema280101.logtech.LogTechConfig.MAX_SAVED_VIEWS.get();
        } catch (Exception e) {
            return io.github.chema280101.logtech.storage.ViewManager.MAX_VIEWS;
        }
    }

    public io.github.chema280101.logtech.security.ActivityLogBuffer<String> getActivityLog() {
        return this.activityLog;
    }

    public int getActivityLogCapacity() {
        try {
            return io.github.chema280101.logtech.LogTechConfig.ACTIVITY_LOG_CAPACITY.get();
        } catch (Exception e) {
            return 500;
        }
    }

    public long getActivityLogGroupingWindowTicks() {
        try {
            return io.github.chema280101.logtech.LogTechConfig.ACTIVITY_LOG_GROUPING_WINDOW_TICKS.get();
        } catch (Exception e) {
            return io.github.chema280101.logtech.security.ActivityLogBuffer.DEFAULT_GROUPING_WINDOW_TICKS;
        }
    }

    public boolean isActivityLogEnabled() {
        try {
            return io.github.chema280101.logtech.LogTechConfig.ACTIVITY_LOG_ENABLED.get();
        } catch (Exception e) {
            return true;
        }
    }

    public void logActivity(
            @Nullable Player player,
            io.github.chema280101.logtech.security.ActivityAction action,
            @Nullable String itemId,
            long amount
    ) {
        if (player == null || action == null || amount <= 0 || !isActivityLogEnabled()) {
            return;
        }
        long gameTime = (level != null) ? level.getGameTime() : 0L;
        this.activityLog.setGroupingWindowTicks(getActivityLogGroupingWindowTicks());
        this.activityLog.record(
                gameTime,
                player.getUUID(),
                player.getScoreboardName(),
                action,
                (itemId != null) ? itemId : "",
                amount,
                getActivityLogCapacity()
        );
        setChanged();
    }

    public io.github.chema280101.logtech.storage.RateTracker<ItemKey> getRateTracker() {
        try {
            if (LogTechConfig.RATE_TRACKER_MAX_ITEMS != null) {
                this.rateTracker.setMaxTrackedItems(LogTechConfig.RATE_TRACKER_MAX_ITEMS.get());
            }
        } catch (Exception ignored) {
        }
        return this.rateTracker;
    }

    public io.github.chema280101.logtech.storage.QuarantineBuffer<ItemKey> getQuarantineBuffer() {
        return this.quarantineManager;
    }

    public long getQuarantineCount() {
        return this.quarantineManager.getTotalCount();
    }

    public boolean isQuarantineEnabled() {
        try {
            return io.github.chema280101.logtech.LogTechConfig.QUARANTINE_ENABLED.get();
        } catch (Exception e) {
            return true;
        }
    }

    public int getQuarantineCapacity() {
        try {
            return io.github.chema280101.logtech.LogTechConfig.QUARANTINE_CAPACITY.get();
        } catch (Exception e) {
            return 1024;
        }
    }

    public int getQuarantineDrainPerCycle() {
        try {
            return io.github.chema280101.logtech.LogTechConfig.QUARANTINE_DRAIN_PER_CYCLE.get();
        } catch (Exception e) {
            return 64;
        }
    }

    public int getEnergyConsumptionPerTick() {
        return io.github.chema280101.logtech.LogTechConfig.CORE_ENERGY_CONSUMPTION.get();
    }

    public ActivityCounter getActivityCounter() {
        return this.activityCounter;
    }

    public ActivityCounter getEnergyReceivedCounter() {
        return this.energyReceivedCounter;
    }

    public CoreEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public io.github.chema280101.logtech.security.NetworkSecurityManager getSecurityManager() {
        return this.securityManager;
    }

    public void closeRevokedPlayerMenus() {
        if (this.level == null || this.level.isClientSide()) return;
        if (this.level.getServer() == null) return;
        for (net.minecraft.server.level.ServerPlayer player : this.level.getServer().getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof CoreMenu cm && cm.getBlockPos().equals(this.worldPosition)) {
                if (!io.github.chema280101.logtech.security.NetworkSecurity.canPlayerDo(player, io.github.chema280101.logtech.security.PlayerAction.OPEN_CORE, this)) {
                    player.closeContainer();
                }
            } else if (player.containerMenu instanceof TerminalMenu tm) {
                CoreBlockEntity termCore = tm.getCoreBlockEntity(player);
                if (termCore == this && !io.github.chema280101.logtech.security.NetworkSecurity.canPlayerDo(player, io.github.chema280101.logtech.security.PlayerAction.OPEN_TERMINAL, this)) {
                    player.closeContainer();
                }
            }
        }
    }

    public int getEnergyStored() {
        return this.energyStorage.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return this.energyStorage.getMaxEnergyStored();
    }

    private void onEnergyReceived(int amount) {
        this.energyReceivedTotal += amount;
        this.energyReceivedCounter.recordEnergyReceived(amount);
        setChanged();
    }

    public long getEnergyReceivedTotal() {
        return this.energyReceivedTotal;
    }

    public long getEnergyDistributedTotal() {
        return this.energyDistributedTotal;
    }

    public long getEnergyConsumedSelfTotal() {
        return this.energyConsumedSelfTotal;
    }

    public boolean isPowered() {
        return this.energyStorage.getEnergyStored() >= getEnergyConsumptionPerTick();
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        this.activityCounter.tick(isPowered());
        this.energyReceivedCounter.tick(true);
        this.rateTracker.tick();
        this.energyTickCounter++;
        int energyInterval = getEnergyTickInterval();
        if (this.energyTickCounter >= energyInterval) {
            this.energyTickCounter = 0;
            Network net = getOrRecalculateNetwork();
            if (net.isValid()) {
                int consumption = getEnergyConsumptionPerTick();
                int accumulated = consumption * energyInterval;
                boolean selfPowered = false;
                if (this.energyStorage.getEnergyStored() >= accumulated) {
                    this.energyStorage.consume(accumulated);
                    this.energyConsumedSelfTotal += accumulated;
                    this.activityCounter.recordEnergyConsumed(accumulated);
                    selfPowered = true;
                } else if (this.energyStorage.getEnergyStored() > 0) {
                    int remaining = this.energyStorage.getEnergyStored();
                    this.energyStorage.consume(remaining);
                    this.energyConsumedSelfTotal += remaining;
                    this.activityCounter.recordEnergyConsumed(remaining);
                }

                // Repartir proporcionalmente la energía disponible a las máquinas de la red
                if (selfPowered && this.energyStorage.getEnergyStored() > 0) {
                    distributeNetworkEnergy(net);
                }
            }
        }

        if (isPowered()) {
            Network net = getOrRecalculateNetwork();
            if (net.isValid()) {
                this.quarantineTickCounter++;
                if (this.quarantineTickCounter >= 20) {
                    this.quarantineTickCounter = 0;
                    if (isQuarantineEnabled() && !this.quarantineManager.isEmpty()) {
                        updateCapacities();
                        if (getTotalStored() < getCapacity()) {
                            long drained = this.quarantineManager.drainTo(
                                    (k, amt) -> this.insert(k, amt, false),
                                    getQuarantineDrainPerCycle()
                            );
                            if (drained > 0) {
                                setChanged();
                            }
                        }
                    }
                }

                this.ruleTickCounter++;
                if (this.ruleTickCounter >= 20) {
                    this.ruleTickCounter = 0;
                    updateCapacities();
                    io.github.chema280101.logtech.storage.RuleEngine.NetworkState<ItemKey> ruleState =
                            new io.github.chema280101.logtech.storage.RuleEngine.NetworkState<>(
                                    getAll(),
                                    getTotalStored(),
                                    getCapacity(),
                                    this.quarantineManager.getTotalCount()
                            );
                    io.github.chema280101.logtech.storage.RuleEvaluationResult<ItemKey> evalResult =
                            this.ruleManager.evaluate(ruleState);
                    for (ItemKey coldKey : evalResult.activeColdItems()) {
                        long hotAmt = this.hotTier.getAmount(coldKey);
                        if (hotAmt > 0 && this.coldTier.getRemainingSpace() > 0) {
                            long moved = this.hotTier.moveTo(this.coldTier, coldKey, hotAmt);
                            if (moved > 0) {
                                setChanged();
                            }
                        }
                    }

                    // Descartar excedente a la papelera (mandar a la papelera lo que pase de N)
                    if (!evalResult.discardExcessItems().isEmpty()) {
                        for (Map.Entry<ItemKey, Long> excessEntry : evalResult.discardExcessItems().entrySet()) {
                            ItemKey excessKey = excessEntry.getKey();
                            long excess = excessEntry.getValue();
                            if (excess > 0) {
                                long extracted = 0;
                                long hotAmt = this.hotTier.getAmount(excessKey);
                                long toExtractHot = Math.min(excess, hotAmt);
                                if (toExtractHot > 0) {
                                    extracted += this.hotTier.extract(excessKey, toExtractHot, false);
                                }
                                long remaining = excess - extracted;
                                if (remaining > 0) {
                                    long coldAmt = this.coldTier.getAmount(excessKey);
                                    long toExtractCold = Math.min(remaining, coldAmt);
                                    if (toExtractCold > 0) {
                                        extracted += this.coldTier.extract(excessKey, toExtractCold, false);
                                    }
                                }
                                if (extracted > 0 && level != null) {
                                    this.rateTracker.recordExtract(excessKey, extracted, toExtractHot, Math.max(0L, extracted - toExtractHot));
                                    this.trashManager.add(excessKey, extracted, level.getGameTime(), getTrashCapacity());
                                    setChanged();
                                }
                            }
                        }
                    }

                    // Compactar excedente (convertir lo que pase de N a su forma compacta de forma atómica)
                    if (!evalResult.compactExcessItems().isEmpty() && level != null && !level.isClientSide()) {
                        io.github.chema280101.logtech.storage.CompactionRegistry.ensureInitialized(
                                level.getRecipeManager(),
                                level.registryAccess()
                        );
                        int maxConversions = LogTechConfig.COMPACT_MAX_CONVERSIONS_PER_CYCLE.get();
                        for (Map.Entry<ItemKey, Long> compactEntry : evalResult.compactExcessItems().entrySet()) {
                            ItemKey baseKey = compactEntry.getKey();
                            long excess = compactEntry.getValue();
                            if (baseKey == null || excess <= 0) {
                                continue;
                            }
                            // Solo items sin componentes de datos especiales y sin pureza
                            if (!baseKey.patch().isEmpty() || baseKey.toStack(1).has(ModDataComponents.PURITY.get())) {
                                continue;
                            }
                            io.github.chema280101.logtech.storage.CompactionPair<net.minecraft.world.item.Item> pair =
                                    io.github.chema280101.logtech.storage.CompactionRegistry.getPair(baseKey.item());
                            if (pair == null) {
                                continue;
                            }
                            int ratio = pair.ratio();
                            io.github.chema280101.logtech.storage.CompactionLogic.ConversionPlan plan =
                                    io.github.chema280101.logtech.storage.CompactionLogic.planCompaction(excess, ratio, maxConversions);
                            if (plan.conversions() <= 0) {
                                continue;
                            }

                            long toExtract = plan.baseItemsToExtract();
                            long extracted = 0;
                            long hotAmt = this.hotTier.getAmount(baseKey);
                            long toExtractHot = Math.min(toExtract, hotAmt);
                            if (toExtractHot > 0) {
                                extracted += this.hotTier.extract(baseKey, toExtractHot, false);
                            }
                            long remaining = toExtract - extracted;
                            if (remaining > 0) {
                                long coldAmt = this.coldTier.getAmount(baseKey);
                                long toExtractCold = Math.min(remaining, coldAmt);
                                if (toExtractCold > 0) {
                                    extracted += this.coldTier.extract(baseKey, toExtractCold, false);
                                }
                            }

                            if (extracted > 0) {
                                long actualConversions = extracted / ratio;
                                long remainderBase = extracted % ratio;
                                if (remainderBase > 0) {
                                    insert(baseKey, remainderBase, false);
                                }

                                if (actualConversions > 0) {
                                    ItemKey compactKey = new ItemKey(pair.compactItem(), DataComponentPatch.EMPTY);
                                    long insertedCompact = insert(compactKey, actualConversions, false);
                                    long failedCompact = actualConversions - insertedCompact;
                                    if (failedCompact > 0) {
                                        insert(baseKey, failedCompact * ratio, false);
                                    }
                                    if (insertedCompact > 0) {
                                        setChanged();
                                    }
                                }
                            }
                        }
                    }

                    // Purgar ítems caducados en la papelera
                    if (level != null) {
                        int pruned = this.trashManager.pruneExpired(level.getGameTime(), getTrashLifetimeTicks());
                        if (pruned > 0) {
                            setChanged();
                        }
                    }

                    // Notificaciones por chat (jugadores a menos de 64 bloques del Núcleo)
                    if (!evalResult.notifications().isEmpty()) {
                        sendRuleNotifications(evalResult.notifications());
                    }

                    // Actualizar alarmas conectadas a la red según canales activos
                    updateNetworkAlarms(net, evalResult.activeRedstoneChannels());
                }

                this.policyTickCounter++;
                int cycleInterval = io.github.chema280101.logtech.LogTechConfig.TIER_POLICY_CYCLE_INTERVAL.get();
                if (this.policyTickCounter >= cycleInterval) {
                    this.policyTickCounter = 0;
                    io.github.chema280101.logtech.storage.TierPolicyManager.PolicyConfig config =
                            new io.github.chema280101.logtech.storage.TierPolicyManager.PolicyConfig(
                                    cycleInterval,
                                    io.github.chema280101.logtech.LogTechConfig.TIER_POLICY_DECAY_RATE.get(),
                                    io.github.chema280101.logtech.LogTechConfig.TIER_POLICY_HOT_HIGH_WATERMARK.get(),
                                    io.github.chema280101.logtech.LogTechConfig.TIER_POLICY_HOT_LOW_WATERMARK.get(),
                                    io.github.chema280101.logtech.LogTechConfig.TIER_POLICY_MAX_ITEMS_PER_CYCLE.get(),
                                    io.github.chema280101.logtech.LogTechConfig.TIER_POLICY_DEBUG_LOG.get()
                            );
                    updateCapacities();
                    io.github.chema280101.logtech.storage.TierPolicyManager.CycleResult result =
                            this.policyManager.runCycle(this.hotTier, this.coldTier, config);
                    if (result.totalItemsMoved() > 0) {
                        setChanged();
                    }
                }
            } else {
                updateNetworkAlarms(net, Collections.emptySet());
            }
        } else {
            Network net = this.network;
            if (net != null && net.isValid()) {
                updateNetworkAlarms(net, Collections.emptySet());
            }
        }
    }

    private void sendRuleNotifications(List<io.github.chema280101.logtech.storage.RuleNotification<ItemKey>> notifications) {
        if (level == null || level.isClientSide() || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        int range = getRuleNotificationRange();
        double maxDistSq = (double) range * range;
        net.minecraft.world.phys.Vec3 coreCenter = net.minecraft.world.phys.Vec3.atCenterOf(worldPosition);

        for (io.github.chema280101.logtech.storage.RuleNotification<ItemKey> notif : notifications) {
            io.github.chema280101.logtech.storage.AutomationRule<ItemKey> rule = notif.rule();
            Component message = formatNotificationMessage(rule);

            for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(coreCenter) <= maxDistSq) {
                    player.sendSystemMessage(message);
                    player.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                }
            }
        }
    }

    private Component formatNotificationMessage(io.github.chema280101.logtech.storage.AutomationRule<ItemKey> rule) {
        Component prefix = Component.literal("[LogTech] ").withStyle(net.minecraft.ChatFormatting.GOLD);
        Component desc;
        switch (rule.getConditionType()) {
            case ITEM_COUNT_GREATER -> {
                Component itemName = rule.getItem() != null ? rule.getItem().toStack(1).getHoverName() : Component.literal("?");
                desc = Component.translatable("chat.logtech.rule.notify_item_greater", itemName, rule.getThreshold())
                        .withStyle(net.minecraft.ChatFormatting.YELLOW);
            }
            case ITEM_COUNT_LESS -> {
                Component itemName = rule.getItem() != null ? rule.getItem().toStack(1).getHoverName() : Component.literal("?");
                desc = Component.translatable("chat.logtech.rule.notify_item_less", itemName, rule.getThreshold())
                        .withStyle(net.minecraft.ChatFormatting.YELLOW);
            }
            case TOTAL_SPACE_GREATER -> {
                desc = Component.translatable("chat.logtech.rule.notify_space_greater", rule.getThreshold())
                        .withStyle(net.minecraft.ChatFormatting.YELLOW);
            }
            case TOTAL_SPACE_LESS -> {
                desc = Component.translatable("chat.logtech.rule.notify_space_less", rule.getThreshold())
                        .withStyle(net.minecraft.ChatFormatting.YELLOW);
            }
            case QUARANTINE_GREATER -> {
                desc = Component.translatable("chat.logtech.rule.notify_quarantine_greater", rule.getThreshold())
                        .withStyle(net.minecraft.ChatFormatting.YELLOW);
            }
            default -> desc = Component.translatable("chat.logtech.rule.notify_generic").withStyle(net.minecraft.ChatFormatting.YELLOW);
        }
        return Component.empty().append(prefix).append(desc);
    }

    public void updateNetworkAlarms(Network net, Set<Integer> activeChannels) {
        if (net == null || level == null || level.isClientSide()) {
            return;
        }
        for (BlockPos memberPos : net.getMachines()) {
            if (level.hasChunkAt(memberPos)) {
                BlockEntity be = level.getBlockEntity(memberPos);
                if (be instanceof AlarmBlockEntity alarm && !be.isRemoved()) {
                    int channel = alarm.getChannel();
                    boolean active = activeChannels != null && activeChannels.contains(channel);
                    alarm.setPowered(active);
                }
            }
        }
    }

    private void distributeNetworkEnergy(Network net) {
        if (net == null || level == null || level.isClientSide()) {
            return;
        }

        int available = this.energyStorage.getEnergyStored();
        if (available <= 0) {
            return;
        }

        Map<NetworkMember, io.github.chema280101.logtech.network.PriorityEnergyDistributor.Request<NetworkMember>> requests =
                new LinkedHashMap<>();
        for (BlockPos memberPos : net.getMachines()) {
            if (level.hasChunkAt(memberPos)) {
                BlockEntity be = level.getBlockEntity(memberPos);
                if (be instanceof NetworkMember member && !be.isRemoved()) {
                    int req = member.requestedEnergy();
                    if (req > 0) {
                        requests.put(
                                member,
                                new io.github.chema280101.logtech.network.PriorityEnergyDistributor.Request<>(
                                        member,
                                        req,
                                        member.getEnergyPriority()
                                )
                        );
                    } else {
                        member.setEnergyShed(false);
                    }
                }
            }
        }

        if (requests.isEmpty()) {
            return;
        }

        io.github.chema280101.logtech.network.PriorityEnergyDistributor.DistributionResult<NetworkMember> result =
                this.energyDistributor.distribute(requests, available);

        int totalDistributed = 0;
        for (Map.Entry<NetworkMember, io.github.chema280101.logtech.network.PriorityEnergyDistributor.Allocation> entry :
                result.allocations().entrySet()) {
            NetworkMember member = entry.getKey();
            io.github.chema280101.logtech.network.PriorityEnergyDistributor.Allocation alloc = entry.getValue();

            member.setEnergyShed(alloc.shed());
            int allocated = alloc.allocatedEnergy();
            if (allocated > 0) {
                int accepted = member.receiveNetworkEnergy(allocated);
                totalDistributed += accepted;
            }
        }

        if (totalDistributed > 0) {
            this.energyStorage.consume(totalDistributed);
            this.energyDistributedTotal += totalDistributed;
            setChanged();
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

    public io.github.chema280101.logtech.storage.TierStorage getTierStorage(io.github.chema280101.logtech.storage.StorageTier tier) {
        return tier == io.github.chema280101.logtech.storage.StorageTier.COLD ? this.coldTier : this.hotTier;
    }

    public void updateCapacities() {
        Network net = getOrRecalculateNetwork();
        if (!net.isValid() || level == null) {
            this.hotTier.setCapacity(0L);
            this.coldTier.setCapacity(0L);
            return;
        }
        long hotCap = 0L;
        long coldCap = 0L;
        for (BlockPos rackPos : net.getRacks()) {
            if (level.hasChunkAt(rackPos)) {
                BlockEntity be = level.getBlockEntity(rackPos);
                if (be instanceof RackBlockEntity rackBe) {
                    if (rackBe.getTier() == io.github.chema280101.logtech.storage.StorageTier.COLD) {
                        coldCap += rackBe.getCapacity();
                    } else {
                        hotCap += rackBe.getCapacity();
                    }
                }
            }
        }
        this.hotTier.setCapacity(hotCap);
        this.coldTier.setCapacity(coldCap);
    }

    public long getCapacity(io.github.chema280101.logtech.storage.StorageTier tier) {
        updateCapacities();
        return getTierStorage(tier).getCapacity();
    }

    public long getCapacity() {
        updateCapacities();
        return this.hotTier.getCapacity() + this.coldTier.getCapacity();
    }

    public long getTotalStored() {
        return this.hotTier.getTotalStored() + this.coldTier.getTotalStored();
    }

    public long getTotalStored(io.github.chema280101.logtech.storage.StorageTier tier) {
        return getTierStorage(tier).getTotalStored();
    }

    public boolean isOverloaded() {
        updateCapacities();
        return this.hotTier.isOverloaded() || this.coldTier.isOverloaded();
    }

    public boolean isOverloaded(io.github.chema280101.logtech.storage.StorageTier tier) {
        updateCapacities();
        return getTierStorage(tier).isOverloaded();
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
     * Inserta primero en el nivel caliente (HOT) y, si está lleno, en el nivel frío (COLD).
     * @param key clave del item y sus componentes
     * @param amount cantidad a insertar (debe ser > 0)
     * @param simulate si es true, no modifica el almacenamiento
     * @return la cantidad insertada exitosamente
     */
    public long insert(ItemKey key, long amount, boolean simulate) {
        if (key == null || amount <= 0 || !isPowered()) {
            return 0L;
        }

        Network net = getOrRecalculateNetwork();
        if (!net.isValid()) {
            return 0L;
        }

        updateCapacities();

        long insertedHot = 0L;
        long insertedCold = 0L;
        if (getPinMode(key) == io.github.chema280101.logtech.storage.ItemPinMode.SOLO_FRIO) {
            insertedCold = this.coldTier.insert(key, amount, simulate);
            long remaining = amount - insertedCold;
            if (remaining > 0) {
                insertedHot = this.hotTier.insert(key, remaining, simulate);
            }
        } else {
            insertedHot = this.hotTier.insert(key, amount, simulate);
            long remaining = amount - insertedHot;
            if (remaining > 0) {
                insertedCold = this.coldTier.insert(key, remaining, simulate);
            }
        }

        long totalInserted = insertedHot + insertedCold;
        if (!simulate && totalInserted > 0) {
            this.activityCounter.recordItemIn((int) Math.min(totalInserted, Integer.MAX_VALUE));
            this.rateTracker.recordInsert(key, totalInserted, insertedHot, insertedCold);
            setChanged();
        }
        return totalInserted;
    }

    public long insertQuarantine(ItemKey key, long amount, boolean simulate) {
        if (!isQuarantineEnabled() || key == null || amount <= 0) {
            return 0L;
        }
        long inserted = this.quarantineManager.insert(key, amount, simulate, getQuarantineCapacity());
        if (!simulate && inserted > 0) {
            setChanged();
        }
        return inserted;
    }

    /**
     * Inserta ítems provenientes del bloque Backbone (tolvas, tuberías, Perforadoras).
     * Si la red no puede aceptar ítems (sin energía, sobrecargada o llena), los ítems
     * entran en el búfer de cuarentena si está habilitado en la configuración.
     */
    public long insertFromBackbone(ItemKey key, long amount, boolean simulate) {
        if (key == null || amount <= 0) {
            return 0L;
        }

        boolean canNormalInsert = isPowered() && !isOverloaded();
        long insertedNormal = 0L;
        if (canNormalInsert) {
            insertedNormal = insert(key, amount, simulate);
        }

        long remaining = amount - insertedNormal;
        if (remaining > 0 && isQuarantineEnabled()) {
            long insertedQuarantine = insertQuarantine(key, remaining, simulate);
            return insertedNormal + insertedQuarantine;
        }

        return insertedNormal;
    }

    /**
     * Extrae una cantidad de items del Núcleo.
     * Extrae primero del nivel caliente (HOT) y luego del nivel frío (COLD).
     * @param key clave del item y sus componentes
     * @param amount cantidad solicitada a extraer (debe ser > 0)
     * @param simulate si es true, no modifica el almacenamiento
     * @return la cantidad realmente extraída
     */
    public long extract(ItemKey key, long amount, boolean simulate) {
        if (key == null || amount <= 0 || !isPowered()) {
            return 0L;
        }

        long extractedHot = this.hotTier.extract(key, amount, simulate);
        long remaining = amount - extractedHot;
        long extractedCold = 0L;
        if (remaining > 0) {
            extractedCold = this.coldTier.extract(key, remaining, simulate);
        }

        long totalExtracted = extractedHot + extractedCold;
        if (!simulate && totalExtracted > 0) {
            this.policyManager.recordExtraction(key);
            this.rateTracker.recordExtract(key, totalExtracted, extractedHot, extractedCold);
            setChanged();
        }
        return totalExtracted;
    }

    /**
     * Extrae items exclusivamente del nivel caliente (HOT).
     */
    public long extractHot(ItemKey key, long amount, boolean simulate) {
        if (key == null || amount <= 0 || !isPowered()) {
            return 0L;
        }
        long extracted = this.hotTier.extract(key, amount, simulate);
        if (!simulate && extracted > 0) {
            this.policyManager.recordExtraction(key);
            this.rateTracker.recordExtract(key, extracted, extracted, 0L);
            setChanged();
        }
        return extracted;
    }

    /**
     * Extrae items exclusivamente del nivel frío (COLD).
     */
    public long extractCold(ItemKey key, long amount, boolean simulate) {
        if (key == null || amount <= 0 || !isPowered()) {
            return 0L;
        }
        long extracted = this.coldTier.extract(key, amount, simulate);
        if (!simulate && extracted > 0) {
            this.policyManager.recordExtraction(key);
            this.rateTracker.recordExtract(key, extracted, 0L, extracted);
            setChanged();
        }
        return extracted;
    }

    public io.github.chema280101.logtech.storage.ItemPinMode getPinMode(ItemKey key) {
        if (this.ruleManager.isItemMoveToColdActive(key)) {
            return io.github.chema280101.logtech.storage.ItemPinMode.SOLO_FRIO;
        }
        return this.policyManager.getPinMode(key);
    }

    public void setPinMode(ItemKey key, io.github.chema280101.logtech.storage.ItemPinMode mode) {
        this.policyManager.setPinMode(key, mode);
        setChanged();
    }

    public io.github.chema280101.logtech.storage.ItemPinMode cyclePinMode(ItemKey key) {
        io.github.chema280101.logtech.storage.ItemPinMode next = this.policyManager.cyclePinMode(key);
        setChanged();
        return next;
    }

    public io.github.chema280101.logtech.storage.TierPolicyManager getPolicyManager() {
        return this.policyManager;
    }

    public io.github.chema280101.logtech.storage.RuleManager getRuleManager() {
        return this.ruleManager;
    }

    /**
     * Devuelve una vista combinada de solo lectura de todo lo almacenado en ambos niveles.
     */
    public Map<ItemKey, Long> getAll() {
        Map<ItemKey, Long> combined = new HashMap<>(this.hotTier.getAll());
        for (Map.Entry<ItemKey, Long> entry : this.coldTier.getAll().entrySet()) {
            combined.merge(entry.getKey(), entry.getValue(), Long::sum);
        }
        return Collections.unmodifiableMap(combined);
    }

    /**
     * Devuelve la cantidad almacenada de un tipo de item específico sumando ambos niveles.
     */
    public long getAmount(ItemKey key) {
        return this.hotTier.getAmount(key) + this.coldTier.getAmount(key);
    }

    public long getAmount(ItemKey key, io.github.chema280101.logtech.storage.StorageTier tier) {
        return getTierStorage(tier).getAmount(key);
    }

    public boolean isEmpty() {
        return this.hotTier.isEmpty() && this.coldTier.isEmpty();
    }

    public StorageContent getStorageContent() {
        return this.hotTier.getStorageContent();
    }

    public StorageContent getColdStorageContent() {
        return this.coldTier.getStorageContent();
    }

    public void setStorageContent(StorageContent content) {
        this.hotTier.setContent(content);
        setChanged();
    }

    public void setColdStorageContent(StorageContent content) {
        this.coldTier.setContent(content);
        setChanged();
    }

    @Override
    public String getConfigType() {
        return ConfigTypes.CORE;
    }

    @Override
    public CompoundTag exportConfig(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("data_version", 6);
        tag.put("automation_rules", this.ruleManager.saveRules(registries));
        tag.put("item_policies", this.policyManager.savePinnedPolicies(registries));
        return tag;
    }

    @Override
    public boolean importConfig(CompoundTag tag, HolderLookup.Provider registries, @Nullable Player player) {
        if (tag == null) {
            return false;
        }
        int version = tag.contains("data_version") ? tag.getInt("data_version") : -1;
        if (version != 6) {
            return false;
        }

        if (tag.contains("automation_rules")) {
            net.minecraft.nbt.Tag rulesTag = tag.get("automation_rules");
            if (!(rulesTag instanceof net.minecraft.nbt.ListTag list)) {
                return false;
            }
            int maxRules = io.github.chema280101.logtech.LogTechConfig.MAX_RULES.get();
            if (list.size() > maxRules) {
                return false;
            }
            for (int i = 0; i < list.size(); i++) {
                if (!(list.get(i) instanceof CompoundTag ruleTag)) {
                    return false;
                }
                int id = ruleTag.getInt("id");
                if (id < 0 || id >= maxRules) {
                    return false;
                }
                long threshold = ruleTag.getLong("threshold");
                if (threshold < 0) {
                    return false;
                }
                if (!ruleTag.contains("condition") || !ruleTag.contains("action")) {
                    return false;
                }
                String condName = ruleTag.getString("condition");
                try {
                    io.github.chema280101.logtech.storage.RuleConditionType.fromSerialized(condName);
                } catch (Exception e) {
                    return false;
                }
                String actName = ruleTag.getString("action");
                try {
                    io.github.chema280101.logtech.storage.RuleAction.fromSerialized(actName);
                } catch (Exception e) {
                    return false;
                }
            }
        }

        if (tag.contains("item_policies")) {
            net.minecraft.nbt.Tag policiesTag = tag.get("item_policies");
            if (!(policiesTag instanceof net.minecraft.nbt.ListTag list)) {
                return false;
            }
            if (list.size() > 5000) {
                return false;
            }
            for (int i = 0; i < list.size(); i++) {
                if (!(list.get(i) instanceof CompoundTag policyTag)) {
                    return false;
                }
                if (!policyTag.contains("key") || !policyTag.contains("mode")) {
                    return false;
                }
                try {
                    io.github.chema280101.logtech.storage.ItemPinMode.valueOf(policyTag.getString("mode"));
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        }

        if (tag.contains("automation_rules")) {
            this.ruleManager.loadRules(tag.get("automation_rules"), registries);
        }
        if (tag.contains("item_policies")) {
            this.policyManager.loadPinnedPolicies(tag.get("item_policies"), registries);
        }
        if (player != null) {
            logActivity(player, io.github.chema280101.logtech.security.ActivityAction.CHANGE_RULES, "", 1L);
        }
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("data_version", 8);
        tag.putInt("energy", this.energyStorage.getEnergyStored());
        tag.putLong("energy_received_total", this.energyReceivedTotal);
        tag.putLong("energy_distributed_total", this.energyDistributedTotal);
        tag.putLong("energy_consumed_self_total", this.energyConsumedSelfTotal);
        tag.put("hot_storage", this.hotTier.save(registries));
        tag.put("cold_storage", this.coldTier.save(registries));
        tag.put("item_policies", this.policyManager.savePinnedPolicies(registries));
        tag.put("automation_rules", this.ruleManager.saveRules(registries));
        tag.put("trash_bin", this.trashManager.saveTrash(registries));
        tag.put("quarantine", this.quarantineManager.saveQuarantine(registries));
        tag.put("saved_views", io.github.chema280101.logtech.storage.ViewNbtAdapter.saveViews(this.viewManager, registries));
        tag.put("security", io.github.chema280101.logtech.security.SecurityNbtAdapter.save(this.securityManager));
        tag.put("activity_log", io.github.chema280101.logtech.security.ActivityLogNbtAdapter.save(this.activityLog));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("energy")) {
            this.energyStorage.setEnergy(tag.getInt("energy"));
        }
        if (tag.contains("energy_received_total")) {
            this.energyReceivedTotal = tag.getLong("energy_received_total");
        }
        if (tag.contains("energy_distributed_total")) {
            this.energyDistributedTotal = tag.getLong("energy_distributed_total");
        }
        if (tag.contains("energy_consumed_self_total")) {
            this.energyConsumedSelfTotal = tag.getLong("energy_consumed_self_total");
        }

        int dataVersion = tag.contains("data_version") ? tag.getInt("data_version") : 1;
        if (dataVersion < 2) {
            // Migración desde v0.1: sin data_version (o data_version 1), todo va a caliente
            if (tag.contains("storage")) {
                StorageContent.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("storage"))
                        .resultOrPartial()
                        .ifPresent(this.hotTier::setContent);
            }
        } else {
            if (tag.contains("hot_storage")) {
                this.hotTier.load(tag.get("hot_storage"), registries);
            }
            if (tag.contains("cold_storage")) {
                this.coldTier.load(tag.get("cold_storage"), registries);
            }
            if (tag.contains("item_policies")) {
                this.policyManager.loadPinnedPolicies(tag.get("item_policies"), registries);
            }
        }
        if (tag.contains("automation_rules")) {
            this.ruleManager.loadRules(tag.get("automation_rules"), registries);
        }
        if (dataVersion >= 4 && tag.contains("trash_bin")) {
            this.trashManager.loadTrash(tag.get("trash_bin"), registries, getTrashCapacity());
        } else {
            this.trashManager.clear();
        }
        if (dataVersion >= 5 && tag.contains("quarantine")) {
            this.quarantineManager.loadQuarantine(tag.get("quarantine"), registries, getQuarantineCapacity());
        } else {
            this.quarantineManager.clear();
        }
        // data_version 6: vistas guardadas (mundos anteriores inician con lista vacía)
        if (dataVersion >= 6 && tag.contains("saved_views")) {
            io.github.chema280101.logtech.storage.ViewNbtAdapter.loadViews(this.viewManager, tag.get("saved_views"), registries);
        } else {
            this.viewManager.clear();
        }
        // data_version 7: seguridad y permisos (mundos anteriores inician en Abierta sin propietario)
        if (dataVersion >= 7 && tag.contains("security")) {
            io.github.chema280101.logtech.security.SecurityNbtAdapter.load(this.securityManager, tag.getCompound("security"));
        } else {
            this.securityManager.setOwner(null, "");
            this.securityManager.setMode(io.github.chema280101.logtech.security.SecurityMode.OPEN);
            this.securityManager.clearEntries();
        }
        // data_version 8: registro de actividad (mundos anteriores inician con registro vacío)
        if (dataVersion >= 8 && tag.contains("activity_log")) {
            io.github.chema280101.logtech.security.ActivityLogNbtAdapter.load(this.activityLog, tag.getCompound("activity_log"), getActivityLogCapacity());
        } else {
            this.activityLog.clear();
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModDataComponents.STORAGE_CONTENT.get(), getStorageContent());
        components.set(ModDataComponents.COLD_STORAGE_CONTENT.get(), getColdStorageContent());
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        StorageContent hot = componentInput.get(ModDataComponents.STORAGE_CONTENT.get());
        if (hot != null) {
            setStorageContent(hot);
        }
        StorageContent cold = componentInput.get(ModDataComponents.COLD_STORAGE_CONTENT.get());
        if (cold != null) {
            setColdStorageContent(cold);
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("storage");
        tag.remove("hot_storage");
        tag.remove("cold_storage");
        tag.remove("item_policies");
        tag.remove("automation_rules");
        tag.remove("trash_bin");
        tag.remove("quarantine");
        tag.remove("saved_views");
        tag.remove("security");
        tag.remove("activity_log");
    }
}
