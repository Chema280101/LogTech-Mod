package io.github.chema280101.logtech.block;

import io.github.chema280101.logtech.network.CoreRuleUpdatePayload;
import io.github.chema280101.logtech.network.CoreSyncPayload;
import io.github.chema280101.logtech.network.Network;
import io.github.chema280101.logtech.network.NetworkMember;
import io.github.chema280101.logtech.registry.ModBlocks;
import io.github.chema280101.logtech.registry.ModMenuTypes;
import io.github.chema280101.logtech.storage.AutomationRule;
import io.github.chema280101.logtech.storage.ItemKey;
import io.github.chema280101.logtech.storage.RuleAction;
import io.github.chema280101.logtech.storage.RuleConditionType;
import io.github.chema280101.logtech.storage.RuleEngine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
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

public class CoreMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final ContainerLevelAccess access;
    private final ServerPlayer serverPlayer;
    private int syncTickCounter = 0;

    // Estado del cliente recibido en sincronización
    private boolean clientNetworkValid = false;
    private int clientBlockCount = 0;
    private int clientEnergyStored = 0;
    private int clientMaxEnergyStored = 0;
    private int clientEnergyConsumption = 0;
    private long clientTotalStored = 0L;
    private long clientTotalCapacity = 0L;
    private long clientHotStored = 0L;
    private long clientHotCapacity = 0L;
    private long clientColdStored = 0L;
    private long clientColdCapacity = 0L;
    private List<CoreSyncPayload.MachineInfo> clientMachines = Collections.emptyList();
    private List<CoreSyncPayload.RuleDto> clientRules = Collections.emptyList();
    private Runnable onSyncCallback = null;

    public CoreMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos());
    }

    public CoreMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.CORE.get(), containerId);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.serverPlayer = playerInventory.player instanceof ServerPlayer sp ? sp : null;

        // Ranuras del inventario del jugador (3 filas x 9 columnas) en la parte inferior centradas para width=220
        int invX = 29;
        int invY = 144;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }

        // Barra de acceso rápido (hotbar: 1 fila x 9 columnas)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, invX + col * 18, invY + 58));
        }

        if (this.serverPlayer != null) {
            CoreBlockEntity coreBe = getCoreBlockEntity(this.serverPlayer);
            if (coreBe != null) {
                sendFullSync(coreBe);
            }
        }
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    public void setOnSyncCallback(Runnable callback) {
        this.onSyncCallback = callback;
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
                level.getBlockState(pos).is(ModBlocks.CORE.get())
                        && player.distanceToSqr((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D) <= maxDistSq,
                true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 27) {
                if (!this.moveItemStackTo(itemstack1, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 36) {
                if (!this.moveItemStackTo(itemstack1, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (this.serverPlayer != null) {
            this.syncTickCounter++;
            if (this.syncTickCounter >= 10) {
                this.syncTickCounter = 0;
                CoreBlockEntity coreBe = getCoreBlockEntity(this.serverPlayer);
                if (coreBe != null) {
                    sendFullSync(coreBe);
                }
            }
        }
    }

    private CoreBlockEntity getCoreBlockEntity(ServerPlayer player) {
        Level level = player.level();
        if (level == null || !level.hasChunkAt(pos)) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof CoreBlockEntity coreBe ? coreBe : null;
    }

    private void sendFullSync(CoreBlockEntity coreBe) {
        if (this.serverPlayer == null) return;

        Network net = coreBe.getOrRecalculateNetwork();
        boolean valid = net.isValid();
        int blockCount = net.getBlockCount();
        int energy = coreBe.getEnergyStored();
        int maxEnergy = coreBe.getMaxEnergyStored();
        int consumption = coreBe.getEnergyConsumptionPerTick();
        long totalStored = coreBe.getTotalStored();
        long totalCapacity = coreBe.getCapacity();
        long hotStored = coreBe.getTotalStored(io.github.chema280101.logtech.storage.StorageTier.HOT);
        long hotCapacity = coreBe.getCapacity(io.github.chema280101.logtech.storage.StorageTier.HOT);
        long coldStored = coreBe.getTotalStored(io.github.chema280101.logtech.storage.StorageTier.COLD);
        long coldCapacity = coreBe.getCapacity(io.github.chema280101.logtech.storage.StorageTier.COLD);

        List<NetworkMember> members = coreBe.getMembers();
        List<CoreSyncPayload.MachineInfo> machineInfos = new ArrayList<>(members.size());
        for (NetworkMember m : members) {
            machineInfos.add(new CoreSyncPayload.MachineInfo(
                    m.getName(),
                    m.getStatus().getStatusKey(),
                    m.getStatus().getCauseKey()
            ));
        }

        List<CoreSyncPayload.RuleDto> ruleDtos = new ArrayList<>(RuleEngine.MAX_RULES);
        for (int i = 0; i < RuleEngine.MAX_RULES; i++) {
            AutomationRule<ItemKey> rule = coreBe.getRuleManager().getRule(i);
            if (rule != null) {
                ItemStack stack = rule.getItem() != null ? rule.getItem().toStack(1) : ItemStack.EMPTY;
                ruleDtos.add(new CoreSyncPayload.RuleDto(
                        rule.getId(),
                        stack,
                        rule.getConditionType().ordinal(),
                        rule.getThreshold(),
                        rule.getAction().ordinal(),
                        rule.isEnabled(),
                        coreBe.getRuleManager().isRuleActive(i)
                ));
            } else {
                ruleDtos.add(new CoreSyncPayload.RuleDto(
                        i,
                        ItemStack.EMPTY,
                        RuleConditionType.ITEM_COUNT_GREATER.ordinal(),
                        0L,
                        RuleAction.MOVE_TO_COLD.ordinal(),
                        false,
                        false
                ));
            }
        }

        PacketDistributor.sendToPlayer(
                this.serverPlayer,
                new CoreSyncPayload(
                        this.containerId,
                        valid,
                        blockCount,
                        energy,
                        maxEnergy,
                        consumption,
                        totalStored,
                        totalCapacity,
                        hotStored,
                        hotCapacity,
                        coldStored,
                        coldCapacity,
                        machineInfos,
                        ruleDtos
                )
        );
    }

    public void handleRuleUpdate(CoreRuleUpdatePayload payload) {
        if (this.serverPlayer == null || !stillValid(this.serverPlayer)) {
            return;
        }
        CoreBlockEntity coreBe = getCoreBlockEntity(this.serverPlayer);
        if (coreBe == null) {
            return;
        }

        int ruleIndex = payload.ruleIndex();
        if (ruleIndex < 0 || ruleIndex >= RuleEngine.MAX_RULES) {
            return;
        }

        RuleConditionType condition;
        try {
            condition = RuleConditionType.values()[payload.conditionOrdinal()];
        } catch (Exception e) {
            condition = RuleConditionType.ITEM_COUNT_GREATER;
        }

        RuleAction action;
        try {
            action = RuleAction.values()[payload.actionOrdinal()];
        } catch (Exception e) {
            action = RuleAction.MOVE_TO_COLD;
        }

        long threshold = payload.threshold();
        if (condition == RuleConditionType.TOTAL_SPACE_GREATER || condition == RuleConditionType.TOTAL_SPACE_LESS) {
            threshold = Math.max(0L, Math.min(100L, threshold));
        } else {
            threshold = Math.max(0L, Math.min(1_000_000_000L, threshold));
        }

        ItemStack itemStack = payload.item();
        ItemKey itemKey = null;
        if (itemStack != null && !itemStack.isEmpty()) {
            itemKey = ItemKey.of(itemStack);
        }

        boolean enabled = payload.enabled();

        AutomationRule<ItemKey> rule = new AutomationRule<>(ruleIndex, itemKey, condition, threshold, action, enabled);
        coreBe.getRuleManager().setRule(ruleIndex, rule);
        coreBe.setChanged();

        sendFullSync(coreBe);
    }

    public void receiveSync(CoreSyncPayload payload) {
        this.clientNetworkValid = payload.networkValid();
        this.clientBlockCount = payload.blockCount();
        this.clientEnergyStored = payload.energyStored();
        this.clientMaxEnergyStored = payload.maxEnergyStored();
        this.clientEnergyConsumption = payload.energyConsumption();
        this.clientTotalStored = payload.totalStored();
        this.clientTotalCapacity = payload.totalCapacity();
        this.clientHotStored = payload.hotStored();
        this.clientHotCapacity = payload.hotCapacity();
        this.clientColdStored = payload.coldStored();
        this.clientColdCapacity = payload.coldCapacity();
        this.clientMachines = payload.machines();
        this.clientRules = payload.rules();

        if (this.onSyncCallback != null) {
            this.onSyncCallback.run();
        }
    }

    // Getters para el cliente
    public boolean isClientNetworkValid() { return clientNetworkValid; }
    public int getClientBlockCount() { return clientBlockCount; }
    public int getClientEnergyStored() { return clientEnergyStored; }
    public int getClientMaxEnergyStored() { return clientMaxEnergyStored; }
    public int getClientEnergyConsumption() { return clientEnergyConsumption; }
    public long getClientTotalStored() { return clientTotalStored; }
    public long getClientTotalCapacity() { return clientTotalCapacity; }
    public long getClientHotStored() { return clientHotStored; }
    public long getClientHotCapacity() { return clientHotCapacity; }
    public long getClientColdStored() { return clientColdStored; }
    public long getClientColdCapacity() { return clientColdCapacity; }
    public List<CoreSyncPayload.MachineInfo> getClientMachines() { return clientMachines; }
    public List<CoreSyncPayload.RuleDto> getClientRules() { return clientRules; }
}
