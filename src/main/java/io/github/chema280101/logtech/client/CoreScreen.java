package io.github.chema280101.logtech.client;

import io.github.chema280101.logtech.block.CoreMenu;
import io.github.chema280101.logtech.network.CoreAccessUpdatePayload;
import io.github.chema280101.logtech.network.CoreClaimPayload;
import io.github.chema280101.logtech.network.CoreRuleUpdatePayload;
import io.github.chema280101.logtech.network.CoreSyncPayload;
import io.github.chema280101.logtech.security.SecurityMode;
import io.github.chema280101.logtech.security.SecurityPermission;
import io.github.chema280101.logtech.storage.RuleAction;
import io.github.chema280101.logtech.storage.RuleConditionType;
import io.github.chema280101.logtech.storage.RuleEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class CoreScreen extends AbstractContainerScreen<CoreMenu> {

    public enum Tab {
        STATUS,
        RULES,
        ACCESS
    }

    private Tab currentTab = Tab.STATUS;
    private Button statusTabButton;
    private Button rulesTabButton;
    private Button accessTabButton;
    private Button claimButton;
    private int machineScroll = 0;

    // Widgets para la pestaña de reglas
    private static class RowWidgets {
        Button condButton;
        EditBox threshBox;
        Button actButton;
        Button toggleButton;
    }
    private final List<RowWidgets> ruleWidgets = new ArrayList<>(RuleEngine.MAX_RULES);

    // Widgets para la pestaña de acceso
    private static class AccessRowWidgets {
        Button viewBtn;
        Button extractBtn;
        Button insertBtn;
        Button adminBtn;
        Button removeBtn;
    }
    private Button modeButton;
    private Button accessPrevBtn;
    private Button accessNextBtn;
    private Button cycleOnlinePlayerBtn;
    private Button addPlayerBtn;
    private int accessPage = 0;
    private int selectedOnlinePlayerIdx = 0;
    private final List<AccessRowWidgets> accessRowWidgets = new ArrayList<>(4);

    public CoreScreen(CoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 220;
        this.imageHeight = 226;
        this.inventoryLabelY = 132;
        this.inventoryLabelX = 29;
        this.titleLabelX = 29;
    }

    @Override
    protected void init() {
        super.init();
        this.menu.setOnSyncCallback(this::onSyncReceived);

        // Botones de pestañas superiores
        int tabY = this.topPos - 18;
        int statusTabX = this.leftPos + 6;
        int tabW = 48;

        this.statusTabButton = Button.builder(
                Component.translatable("gui.logtech.core.tab.status"),
                btn -> selectTab(Tab.STATUS)
        ).bounds(statusTabX, tabY, tabW, 18).build();

        this.rulesTabButton = Button.builder(
                Component.translatable("gui.logtech.core.tab.rules"),
                btn -> selectTab(Tab.RULES)
        ).bounds(statusTabX + tabW + 2, tabY, tabW, 18).build();

        this.accessTabButton = Button.builder(
                Component.translatable("gui.logtech.core.tab.access"),
                btn -> selectTab(Tab.ACCESS)
        ).bounds(statusTabX + (tabW + 2) * 2, tabY, tabW, 18).build();

        this.claimButton = Button.builder(
                Component.translatable("gui.logtech.core.claim"),
                btn -> onClaimClicked()
        ).bounds(this.leftPos + this.imageWidth - 58, tabY, 52, 18)
        .tooltip(Tooltip.create(Component.translatable("gui.logtech.core.claim.tooltip")))
        .build();

        this.addRenderableWidget(this.statusTabButton);
        this.addRenderableWidget(this.rulesTabButton);
        this.addRenderableWidget(this.accessTabButton);
        this.addRenderableWidget(this.claimButton);

        // Widgets de las 8 filas de reglas
        this.ruleWidgets.clear();
        int startY = this.topPos + 18;

        for (int i = 0; i < RuleEngine.MAX_RULES; i++) {
            final int rowIndex = i;
            int rowY = startY + i * 15;

            RowWidgets rw = new RowWidgets();

            rw.condButton = Button.builder(
                    getConditionButtonText(rowIndex),
                    btn -> cycleCondition(rowIndex)
            ).bounds(this.leftPos + 24, rowY, 48, 13).build();

            rw.threshBox = new EditBox(this.font, this.leftPos + 74, rowY, 42, 13, Component.literal("Threshold"));
            rw.threshBox.setMaxLength(10);
            rw.threshBox.setFilter(s -> s.matches("\\d*"));
            rw.threshBox.setValue(getThresholdText(rowIndex));
            rw.threshBox.setResponder(text -> onThresholdChanged(rowIndex, text));

            rw.actButton = Button.builder(
                    getActionButtonText(rowIndex),
                    btn -> cycleAction(rowIndex)
            ).bounds(this.leftPos + 118, rowY, 50, 13).build();

            rw.toggleButton = Button.builder(
                    getToggleButtonText(rowIndex),
                    btn -> toggleEnabled(rowIndex)
            ).bounds(this.leftPos + 170, rowY, 28, 13).build();

            this.ruleWidgets.add(rw);

            this.addRenderableWidget(rw.condButton);
            this.addRenderableWidget(rw.threshBox);
            this.addRenderableWidget(rw.actButton);
            this.addRenderableWidget(rw.toggleButton);
        }

        // Widgets de la pestaña de acceso
        this.modeButton = Button.builder(
                getModeButtonText(),
                btn -> toggleSecurityMode()
        ).bounds(this.leftPos + 8, this.topPos + 18, 92, 14).build();
        this.addRenderableWidget(this.modeButton);

        this.accessPrevBtn = Button.builder(
                Component.literal("<"),
                btn -> { if (this.accessPage > 0) this.accessPage--; updateAccessButtonTexts(); }
        ).bounds(this.leftPos + 172, this.topPos + 18, 14, 14).build();
        this.addRenderableWidget(this.accessPrevBtn);

        this.accessNextBtn = Button.builder(
                Component.literal(">"),
                btn -> {
                    if ((this.accessPage + 1) * 4 < this.menu.getClientAccessEntries().size()) {
                        this.accessPage++;
                        updateAccessButtonTexts();
                    }
                }
        ).bounds(this.leftPos + 188, this.topPos + 18, 14, 14).build();
        this.addRenderableWidget(this.accessNextBtn);

        this.accessRowWidgets.clear();
        for (int r = 0; r < 4; r++) {
            final int rowIdx = r;
            int rY = this.topPos + 34 + r * 17;
            AccessRowWidgets arw = new AccessRowWidgets();

            arw.viewBtn = Button.builder(Component.literal("V"), btn -> onTogglePerm(rowIdx, SecurityPermission.VIEW))
                    .bounds(this.leftPos + 92, rY, 16, 14)
                    .tooltip(Tooltip.create(Component.translatable("gui.logtech.security.perm.view")))
                    .build();

            arw.extractBtn = Button.builder(Component.literal("E"), btn -> onTogglePerm(rowIdx, SecurityPermission.EXTRACT))
                    .bounds(this.leftPos + 110, rY, 16, 14)
                    .tooltip(Tooltip.create(Component.translatable("gui.logtech.security.perm.extract")))
                    .build();

            arw.insertBtn = Button.builder(Component.literal("I"), btn -> onTogglePerm(rowIdx, SecurityPermission.INSERT))
                    .bounds(this.leftPos + 128, rY, 16, 14)
                    .tooltip(Tooltip.create(Component.translatable("gui.logtech.security.perm.insert")))
                    .build();

            arw.adminBtn = Button.builder(Component.literal("A"), btn -> onTogglePerm(rowIdx, SecurityPermission.ADMIN))
                    .bounds(this.leftPos + 146, rY, 16, 14)
                    .tooltip(Tooltip.create(Component.translatable("gui.logtech.security.perm.admin")))
                    .build();

            arw.removeBtn = Button.builder(Component.literal("×").withStyle(ChatFormatting.RED), btn -> onRemovePlayer(rowIdx))
                    .bounds(this.leftPos + 192, rY, 16, 14)
                    .tooltip(Tooltip.create(Component.translatable("gui.logtech.security.perm.remove")))
                    .build();

            this.accessRowWidgets.add(arw);
            this.addRenderableWidget(arw.viewBtn);
            this.addRenderableWidget(arw.extractBtn);
            this.addRenderableWidget(arw.insertBtn);
            this.addRenderableWidget(arw.adminBtn);
            this.addRenderableWidget(arw.removeBtn);
        }

        this.cycleOnlinePlayerBtn = Button.builder(
                getOnlinePlayerButtonText(),
                btn -> cycleOnlinePlayer()
        ).bounds(this.leftPos + 8, this.topPos + 106, 140, 15).build();
        this.addRenderableWidget(this.cycleOnlinePlayerBtn);

        this.addPlayerBtn = Button.builder(
                Component.translatable("gui.logtech.security.add"),
                btn -> onAddPlayer()
        ).bounds(this.leftPos + 152, this.topPos + 106, 56, 15).build();
        this.addRenderableWidget(this.addPlayerBtn);

        updateWidgetVisibility();
        updateAccessButtonTexts();
    }

    private void selectTab(Tab tab) {
        if (this.currentTab == tab) {
            return;
        }
        this.currentTab = tab;
        updateWidgetVisibility();
    }

    private void updateWidgetVisibility() {
        boolean canAdmin = this.menu.canClientAdmin();
        if (!canAdmin && this.currentTab == Tab.ACCESS) {
            this.currentTab = Tab.STATUS;
        }

        boolean isStatus = (this.currentTab == Tab.STATUS);
        boolean isRules = (this.currentTab == Tab.RULES);
        boolean isAccess = (this.currentTab == Tab.ACCESS);

        if (this.statusTabButton != null) this.statusTabButton.active = !isStatus;
        if (this.rulesTabButton != null) this.rulesTabButton.active = !isRules;
        if (this.accessTabButton != null) {
            this.accessTabButton.visible = canAdmin;
            this.accessTabButton.active = !isAccess;
        }
        if (this.claimButton != null) {
            this.claimButton.visible = this.menu.isClientClaimable();
        }

        for (RowWidgets rw : this.ruleWidgets) {
            if (rw.condButton != null) rw.condButton.visible = isRules;
            if (rw.threshBox != null) rw.threshBox.visible = isRules;
            if (rw.actButton != null) rw.actButton.visible = isRules;
            if (rw.toggleButton != null) rw.toggleButton.visible = isRules;
        }

        if (this.modeButton != null) this.modeButton.visible = isAccess;
        if (this.accessPrevBtn != null) this.accessPrevBtn.visible = isAccess;
        if (this.accessNextBtn != null) this.accessNextBtn.visible = isAccess;
        if (this.cycleOnlinePlayerBtn != null) this.cycleOnlinePlayerBtn.visible = isAccess;
        if (this.addPlayerBtn != null) this.addPlayerBtn.visible = isAccess;

        List<CoreSyncPayload.AccessEntryDto> entries = this.menu.getClientAccessEntries();
        for (int r = 0; r < 4; r++) {
            AccessRowWidgets arw = this.accessRowWidgets.get(r);
            int entryIdx = this.accessPage * 4 + r;
            boolean hasEntry = isAccess && (entryIdx < entries.size());

            if (arw.viewBtn != null) arw.viewBtn.visible = hasEntry;
            if (arw.extractBtn != null) arw.extractBtn.visible = hasEntry;
            if (arw.insertBtn != null) arw.insertBtn.visible = hasEntry;
            if (arw.adminBtn != null) arw.adminBtn.visible = hasEntry;
            if (arw.removeBtn != null) arw.removeBtn.visible = hasEntry;
        }
    }

    private void updateAccessButtonTexts() {
        if (this.modeButton != null) {
            this.modeButton.setMessage(getModeButtonText());
        }

        List<CoreSyncPayload.AccessEntryDto> entries = this.menu.getClientAccessEntries();
        for (int r = 0; r < 4; r++) {
            AccessRowWidgets arw = this.accessRowWidgets.get(r);
            int entryIdx = this.accessPage * 4 + r;
            if (entryIdx < entries.size()) {
                CoreSyncPayload.AccessEntryDto e = entries.get(entryIdx);
                if (arw.viewBtn != null) arw.viewBtn.setMessage(Component.literal("V").withStyle(e.canView() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
                if (arw.extractBtn != null) arw.extractBtn.setMessage(Component.literal("E").withStyle(e.canExtract() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
                if (arw.insertBtn != null) arw.insertBtn.setMessage(Component.literal("I").withStyle(e.canInsert() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
                if (arw.adminBtn != null) arw.adminBtn.setMessage(Component.literal("A").withStyle(e.canAdmin() ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
            }
        }

        if (this.cycleOnlinePlayerBtn != null) {
            this.cycleOnlinePlayerBtn.setMessage(getOnlinePlayerButtonText());
        }
        if (this.accessPrevBtn != null) {
            this.accessPrevBtn.active = this.accessPage > 0;
        }
        if (this.accessNextBtn != null) {
            this.accessNextBtn.active = (this.accessPage + 1) * 4 < entries.size();
        }
    }

    private Component getModeButtonText() {
        int ord = this.menu.getClientSecurityModeOrdinal();
        SecurityMode mode = SecurityMode.values()[Math.max(0, Math.min(SecurityMode.values().length - 1, ord))];
        return mode == SecurityMode.OPEN
                ? Component.translatable("gui.logtech.security.mode.open").withStyle(ChatFormatting.AQUA)
                : Component.translatable("gui.logtech.security.mode.private").withStyle(ChatFormatting.GOLD);
    }

    private Component getOnlinePlayerButtonText() {
        List<CoreSyncPayload.OnlinePlayerDto> online = this.menu.getClientOnlinePlayers();
        if (online.isEmpty()) {
            return Component.translatable("gui.logtech.security.no_online_players").withStyle(ChatFormatting.GRAY);
        }
        if (selectedOnlinePlayerIdx >= online.size()) {
            selectedOnlinePlayerIdx = 0;
        }
        return Component.literal(online.get(selectedOnlinePlayerIdx).name());
    }

    private void toggleSecurityMode() {
        int next = (this.menu.getClientSecurityModeOrdinal() == 0) ? 1 : 0;
        PacketDistributor.sendToServer(new CoreAccessUpdatePayload(
                this.menu.containerId,
                CoreAccessUpdatePayload.ActionType.SET_MODE,
                next,
                null,
                "",
                0
        ));
    }

    private void onTogglePerm(int rowIdx, SecurityPermission permission) {
        List<CoreSyncPayload.AccessEntryDto> entries = this.menu.getClientAccessEntries();
        int entryIdx = this.accessPage * 4 + rowIdx;
        if (entryIdx < entries.size()) {
            UUID target = entries.get(entryIdx).uuid();
            PacketDistributor.sendToServer(new CoreAccessUpdatePayload(
                    this.menu.containerId,
                    CoreAccessUpdatePayload.ActionType.TOGGLE_PERMISSION,
                    0,
                    target,
                    "",
                    permission.ordinal()
            ));
        }
    }

    private void onRemovePlayer(int rowIdx) {
        List<CoreSyncPayload.AccessEntryDto> entries = this.menu.getClientAccessEntries();
        int entryIdx = this.accessPage * 4 + rowIdx;
        if (entryIdx < entries.size()) {
            UUID target = entries.get(entryIdx).uuid();
            PacketDistributor.sendToServer(new CoreAccessUpdatePayload(
                    this.menu.containerId,
                    CoreAccessUpdatePayload.ActionType.REMOVE_PLAYER,
                    0,
                    target,
                    "",
                    0
            ));
        }
    }

    private void cycleOnlinePlayer() {
        List<CoreSyncPayload.OnlinePlayerDto> online = this.menu.getClientOnlinePlayers();
        if (!online.isEmpty()) {
            this.selectedOnlinePlayerIdx = (this.selectedOnlinePlayerIdx + 1) % online.size();
            updateAccessButtonTexts();
        }
    }

    private void onAddPlayer() {
        List<CoreSyncPayload.OnlinePlayerDto> online = this.menu.getClientOnlinePlayers();
        if (!online.isEmpty()) {
            if (selectedOnlinePlayerIdx >= online.size()) {
                selectedOnlinePlayerIdx = 0;
            }
            CoreSyncPayload.OnlinePlayerDto p = online.get(selectedOnlinePlayerIdx);
            PacketDistributor.sendToServer(new CoreAccessUpdatePayload(
                    this.menu.containerId,
                    CoreAccessUpdatePayload.ActionType.ADD_PLAYER,
                    0,
                    p.uuid(),
                    p.name(),
                    0
            ));
        }
    }

    private void onClaimClicked() {
        PacketDistributor.sendToServer(new CoreClaimPayload(this.menu.containerId));
    }

    private CoreSyncPayload.RuleDto getRuleDto(int index) {
        List<CoreSyncPayload.RuleDto> rules = this.menu.getClientRules();
        if (rules != null && index >= 0 && index < rules.size()) {
            return rules.get(index);
        }
        return null;
    }

    private Component getConditionButtonText(int index) {
        CoreSyncPayload.RuleDto r = getRuleDto(index);
        int ord = r != null ? r.conditionOrdinal() : 0;
        RuleConditionType type = RuleConditionType.values()[Math.max(0, Math.min(RuleConditionType.values().length - 1, ord))];
        return switch (type) {
            case ITEM_COUNT_GREATER -> Component.translatable("gui.logtech.core.rule.cond.item_greater");
            case ITEM_COUNT_LESS -> Component.translatable("gui.logtech.core.rule.cond.item_less");
            case TOTAL_SPACE_GREATER -> Component.translatable("gui.logtech.core.rule.cond.space_greater");
            case TOTAL_SPACE_LESS -> Component.translatable("gui.logtech.core.rule.cond.space_less");
            case QUARANTINE_GREATER -> Component.translatable("gui.logtech.core.rule.cond.quarantine_greater");
        };
    }

    private String getThresholdText(int index) {
        CoreSyncPayload.RuleDto r = getRuleDto(index);
        return r != null ? String.valueOf(r.threshold()) : "0";
    }

    private Component getActionButtonText(int index) {
        CoreSyncPayload.RuleDto r = getRuleDto(index);
        int ord = r != null ? r.actionOrdinal() : 0;
        RuleAction act = RuleAction.values()[Math.max(0, Math.min(RuleAction.values().length - 1, ord))];
        return switch (act) {
            case MOVE_TO_COLD -> Component.translatable("gui.logtech.core.rule.act.move_to_cold");
            case NOTIFY -> Component.translatable("gui.logtech.core.rule.act.notify");
            case REDSTONE_SIGNAL -> Component.translatable("gui.logtech.core.rule.act.redstone_1");
            case REDSTONE_2 -> Component.translatable("gui.logtech.core.rule.act.redstone_2");
            case REDSTONE_3 -> Component.translatable("gui.logtech.core.rule.act.redstone_3");
            case REDSTONE_4 -> Component.translatable("gui.logtech.core.rule.act.redstone_4");
            case DISCARD_EXCESS -> Component.translatable("gui.logtech.core.rule.act.discard_excess");
            case COMPACT -> Component.translatable("gui.logtech.core.rule.act.compact");
        };
    }

    private Component getToggleButtonText(int index) {
        CoreSyncPayload.RuleDto r = getRuleDto(index);
        boolean enabled = r != null && r.enabled();
        return enabled
                ? Component.literal("ON").withStyle(ChatFormatting.GREEN)
                : Component.literal("OFF").withStyle(ChatFormatting.DARK_GRAY);
    }

    private void onSyncReceived() {
        for (int i = 0; i < RuleEngine.MAX_RULES && i < this.ruleWidgets.size(); i++) {
            RowWidgets rw = this.ruleWidgets.get(i);
            rw.condButton.setMessage(getConditionButtonText(i));
            if (!rw.threshBox.isFocused()) {
                rw.threshBox.setValue(getThresholdText(i));
            }
            rw.actButton.setMessage(getActionButtonText(i));
            rw.toggleButton.setMessage(getToggleButtonText(i));
        }
        updateWidgetVisibility();
        updateAccessButtonTexts();
    }

    private void cycleCondition(int index) {
        CoreSyncPayload.RuleDto r = getRuleDto(index);
        int ord = r != null ? r.conditionOrdinal() : 0;
        int next = (ord + 1) % RuleConditionType.values().length;
        sendRuleUpdate(index, next, r != null ? r.threshold() : 0L, r != null ? r.actionOrdinal() : 0, r == null || r.enabled(), r != null ? r.item() : ItemStack.EMPTY);
    }

    private void onThresholdChanged(int index, String text) {
        try {
            long val = text.isEmpty() ? 0L : Long.parseLong(text);
            CoreSyncPayload.RuleDto r = getRuleDto(index);
            sendRuleUpdate(index, r != null ? r.conditionOrdinal() : 0, val, r != null ? r.actionOrdinal() : 0, r == null || r.enabled(), r != null ? r.item() : ItemStack.EMPTY);
        } catch (NumberFormatException ignored) {}
    }

    private void cycleAction(int index) {
        CoreSyncPayload.RuleDto r = getRuleDto(index);
        int ord = r != null ? r.actionOrdinal() : 0;
        int next = (ord + 1) % RuleAction.values().length;
        sendRuleUpdate(index, r != null ? r.conditionOrdinal() : 0, r != null ? r.threshold() : 0L, next, r == null || r.enabled(), r != null ? r.item() : ItemStack.EMPTY);
    }

    private void toggleEnabled(int index) {
        CoreSyncPayload.RuleDto r = getRuleDto(index);
        boolean current = r != null && r.enabled();
        sendRuleUpdate(index, r != null ? r.conditionOrdinal() : 0, r != null ? r.threshold() : 0L, r != null ? r.actionOrdinal() : 0, !current, r != null ? r.item() : ItemStack.EMPTY);
    }

    private void sendRuleUpdate(int index, int conditionOrdinal, long threshold, int actionOrdinal, boolean enabled, ItemStack item) {
        PacketDistributor.sendToServer(new CoreRuleUpdatePayload(
                this.menu.containerId,
                index,
                item,
                conditionOrdinal,
                threshold,
                actionOrdinal,
                enabled
        ));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.currentTab == Tab.STATUS && this.menu.isClientNetworkValid()) {
            int x = this.leftPos + 10;
            int y = this.topPos + 80;
            if (mouseX >= x && mouseX <= x + 200 && mouseY >= y && mouseY <= y + 36) {
                List<CoreSyncPayload.MachineInfo> machines = this.menu.getClientMachines();
                if (machines.size() > 3) {
                    if (button == 0) {
                        this.machineScroll = (this.machineScroll + 1) % (machines.size() - 2);
                    } else if (button == 1) {
                        this.machineScroll = (this.machineScroll - 1 + (machines.size() - 2)) % (machines.size() - 2);
                    }
                    return true;
                }
            }
        }

        if (this.currentTab == Tab.RULES) {
            int startY = this.topPos + 18;
            for (int i = 0; i < RuleEngine.MAX_RULES; i++) {
                int rowY = startY + i * 15;
                int ghostX = this.leftPos + 6;

                if (mouseX >= ghostX && mouseX <= ghostX + 16 && mouseY >= rowY && mouseY <= rowY + 16) {
                    ItemStack carried = this.menu.getCarried();
                    CoreSyncPayload.RuleDto r = getRuleDto(i);
                    int cond = r != null ? r.conditionOrdinal() : 0;
                    long thresh = r != null ? r.threshold() : 0L;
                    int act = r != null ? r.actionOrdinal() : 0;
                    boolean en = r == null || r.enabled();

                    if (!carried.isEmpty()) {
                        sendRuleUpdate(i, cond, thresh, act, en, carried.copyWithCount(1));
                    } else {
                        sendRuleUpdate(i, cond, thresh, act, en, ItemStack.EMPTY);
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component currentTitle = switch (this.currentTab) {
            case STATUS -> Component.translatable("gui.logtech.core.tab.status");
            case RULES -> Component.translatable("gui.logtech.core.tab.rules");
            case ACCESS -> Component.translatable("gui.logtech.core.tab.access");
        };
        guiGraphics.drawString(this.font, currentTitle, this.titleLabelX, 6, 0xE0E0E0, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x909090, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Marco de fondo principal
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF18181E);
        guiGraphics.renderOutline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 0xFF353540);

        // Barra de encabezado
        guiGraphics.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + this.imageWidth - 4, this.topPos + 16, 0xFF22222A);

        // Marco de ranuras del inventario del jugador
        int invX = this.leftPos + 29;
        int invY = this.topPos + 144;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int sx = invX + col * 18;
                int sy = invY + row * 18;
                guiGraphics.fill(sx, sy, sx + 16, sy + 16, 0xFF121216);
                guiGraphics.renderOutline(sx - 1, sy - 1, 18, 18, 0xFF2A2A34);
            }
        }
        for (int col = 0; col < 9; ++col) {
            int sx = invX + col * 18;
            int sy = invY + 58;
            guiGraphics.fill(sx, sy, sx + 16, sy + 16, 0xFF121216);
            guiGraphics.renderOutline(sx - 1, sy - 1, 18, 18, 0xFF2A2A34);
        }

        if (this.currentTab == Tab.STATUS) {
            renderStatusTab(guiGraphics);
        } else if (this.currentTab == Tab.RULES) {
            renderRulesTab(guiGraphics, mouseX, mouseY);
        } else {
            renderAccessTab(guiGraphics);
        }
    }

    private void renderStatusTab(GuiGraphics guiGraphics) {
        int x = this.leftPos + 10;
        int y = this.topPos + 20;

        // Estado de red
        boolean netValid = this.menu.isClientNetworkValid();
        int blockCount = this.menu.getClientBlockCount();
        Component netText = netValid
                ? Component.translatable("gui.logtech.core.status.valid", blockCount).withStyle(ChatFormatting.GREEN)
                : Component.translatable("gui.logtech.core.status.invalid").withStyle(ChatFormatting.RED);
        guiGraphics.drawString(this.font, netText, x, y, 0xFFFFFF, false);
        y += 12;

        // Energía
        int energy = this.menu.getClientEnergyStored();
        int maxEnergy = this.menu.getClientMaxEnergyStored();
        int consumption = this.menu.getClientEnergyConsumption();
        guiGraphics.drawString(this.font, Component.translatable("gui.logtech.core.status.energy", energy, maxEnergy, consumption), x, y, 0x00E5FF, false);
        y += 10;

        // Barra de energía
        int barW = 200;
        int barH = 5;
        guiGraphics.fill(x, y, x + barW, y + barH, 0xFF101014);
        int filledEnergy = maxEnergy > 0 ? (int) Math.min(barW, ((long) energy * barW / maxEnergy)) : 0;
        if (filledEnergy > 0) {
            guiGraphics.fill(x, y, x + filledEnergy, y + barH, 0xFF00C853);
        }
        guiGraphics.renderOutline(x, y, barW, barH, 0xFF2C2C36);
        y += 9;

        // Almacenamiento total
        long totalStored = this.menu.getClientTotalStored();
        long totalCap = this.menu.getClientTotalCapacity();
        double usedPct = totalCap > 0 ? (totalStored * 100.0 / totalCap) : 0.0;
        guiGraphics.drawString(this.font, Component.translatable("gui.logtech.core.status.storage_total", totalStored, totalCap, String.format("%.1f", usedPct)), x, y, 0xFFFFFF, false);
        y += 10;

        // Barra de almacenamiento
        guiGraphics.fill(x, y, x + barW, y + barH, 0xFF101014);
        int filledStorage = totalCap > 0 ? (int) Math.min(barW, (totalStored * barW / totalCap)) : 0;
        if (filledStorage > 0) {
            guiGraphics.fill(x, y, x + filledStorage, y + barH, 0xFF29B6F6);
        }
        guiGraphics.renderOutline(x, y, barW, barH, 0xFF2C2C36);
        y += 9;

        // Caliente y frío
        long hotStored = this.menu.getClientHotStored();
        long hotCap = this.menu.getClientHotCapacity();
        long coldStored = this.menu.getClientColdStored();
        long coldCap = this.menu.getClientColdCapacity();
        guiGraphics.drawString(this.font, Component.translatable("gui.logtech.core.status.storage_tiers", hotStored, hotCap, coldStored, coldCap), x, y, 0xAAAAAA, false);
        y += 13;

        // Lista de máquinas
        List<CoreSyncPayload.MachineInfo> machines = this.menu.getClientMachines();
        guiGraphics.drawString(this.font, Component.translatable("gui.logtech.core.status.machines", machines.size()), x, y, 0xFFB300, false);
        y += 11;

        if (machines.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("gui.logtech.core.status.no_machines"), x, y, 0x777777, false);
        } else {
            int maxVisible = 3;
            for (int i = 0; i < maxVisible; i++) {
                int mIndex = this.machineScroll + i;
                if (mIndex < machines.size()) {
                    CoreSyncPayload.MachineInfo m = machines.get(mIndex);
                    Component machName = m.name().copy().withStyle(ChatFormatting.WHITE);
                    Component status = Component.translatable(m.statusKey()).withStyle(ChatFormatting.GRAY);
                    guiGraphics.drawString(this.font, Component.literal("• ").append(machName).append(": ").append(status), x + 4, y, 0xCCCCCC, false);
                    y += 10;
                }
            }
        }
    }

    private void renderRulesTab(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int startY = this.topPos + 18;
        for (int i = 0; i < RuleEngine.MAX_RULES; i++) {
            int rowY = startY + i * 15;
            int ghostX = this.leftPos + 6;

            guiGraphics.fill(ghostX, rowY, ghostX + 16, rowY + 16, 0xFF121216);
            guiGraphics.renderOutline(ghostX - 1, rowY - 1, 18, 18, 0xFF2A2A34);

            CoreSyncPayload.RuleDto r = getRuleDto(i);
            if (r != null && r.item() != null && !r.item().isEmpty()) {
                guiGraphics.renderFakeItem(r.item(), ghostX, rowY);
            }

            if (mouseX >= ghostX && mouseX <= ghostX + 16 && mouseY >= rowY && mouseY <= rowY + 16) {
                guiGraphics.fill(ghostX, rowY, ghostX + 16, rowY + 16, 0x40FFFFFF);
            }

            int ledX = this.leftPos + 203;
            int ledY = rowY + 4;
            int ledColor = 0xFF2A2A2A;

            if (r != null && r.enabled()) {
                RuleConditionType cond = RuleConditionType.values()[Math.max(0, Math.min(RuleConditionType.values().length - 1, r.conditionOrdinal()))];
                RuleAction act = RuleAction.values()[Math.max(0, Math.min(RuleAction.values().length - 1, r.actionOrdinal()))];
                boolean missingItem = (cond.requiresItem() || act.requiresItem()) && (r.item() == null || r.item().isEmpty());

                if (missingItem) {
                    ledColor = 0xFFAA3333;
                } else if (r.active()) {
                    ledColor = 0xFF00E676;
                } else {
                    ledColor = 0xFF555555;
                }
            }

            guiGraphics.fill(ledX, ledY, ledX + 5, ledY + 5, ledColor);
            guiGraphics.renderOutline(ledX, ledY, 5, 5, 0xFF111111);
        }
    }

    private void renderAccessTab(GuiGraphics guiGraphics) {
        // Dueño
        String owner = this.menu.getClientOwnerName();
        String ownerText = owner.isBlank() ? "—" : owner;
        guiGraphics.drawString(this.font, Component.translatable("gui.logtech.security.owner", ownerText), this.leftPos + 104, this.topPos + 21, 0xBBBBBB, false);

        // Lista de accesos
        List<CoreSyncPayload.AccessEntryDto> entries = this.menu.getClientAccessEntries();
        int total = entries.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / 4.0));
        String pageInfo = (this.accessPage + 1) + "/" + totalPages;
        guiGraphics.drawString(this.font, pageInfo, this.leftPos + 144, this.topPos + 21, 0x888888, false);

        if (entries.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("gui.logtech.security.empty_list"), this.leftPos + 12, this.topPos + 55, 0x777777, false);
        } else {
            for (int r = 0; r < 4; r++) {
                int entryIdx = this.accessPage * 4 + r;
                if (entryIdx < entries.size()) {
                    int rY = this.topPos + 34 + r * 17;
                    CoreSyncPayload.AccessEntryDto e = entries.get(entryIdx);
                    String name = e.name();
                    if (name.length() > 11) {
                        name = name.substring(0, 10) + "…";
                    }
                    guiGraphics.drawString(this.font, name, this.leftPos + 12, rY + 3, 0xFFFFFF, false);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateWidgetVisibility();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltips de la pestaña de reglas
        if (this.currentTab == Tab.RULES) {
            int startY = this.topPos + 18;
            for (int i = 0; i < RuleEngine.MAX_RULES; i++) {
                int rowY = startY + i * 15;
                int ghostX = this.leftPos + 6;

                if (mouseX >= ghostX && mouseX <= ghostX + 16 && mouseY >= rowY && mouseY <= rowY + 16) {
                    CoreSyncPayload.RuleDto r = getRuleDto(i);
                    if (r != null && r.item() != null && !r.item().isEmpty()) {
                        guiGraphics.renderTooltip(this.font, r.item(), mouseX, mouseY);
                    } else {
                        guiGraphics.renderTooltip(this.font, Component.translatable("gui.logtech.core.rule.ghost_slot_tooltip"), mouseX, mouseY);
                    }
                    break;
                }

                int ledX = this.leftPos + 203;
                int ledY = rowY + 4;
                if (mouseX >= ledX && mouseX <= ledX + 5 && mouseY >= ledY && mouseY <= ledY + 5) {
                    CoreSyncPayload.RuleDto r = getRuleDto(i);
                    Component ledTooltip;
                    if (r == null || !r.enabled()) {
                        ledTooltip = Component.translatable("gui.logtech.core.rule.status.disabled");
                    } else {
                        RuleConditionType cond = RuleConditionType.values()[Math.max(0, Math.min(RuleConditionType.values().length - 1, r.conditionOrdinal()))];
                        RuleAction act = RuleAction.values()[Math.max(0, Math.min(RuleAction.values().length - 1, r.actionOrdinal()))];
                        if ((cond.requiresItem() || act.requiresItem()) && (r.item() == null || r.item().isEmpty())) {
                            ledTooltip = Component.translatable("gui.logtech.core.rule.status.ignored");
                        } else if (r.active()) {
                            ledTooltip = Component.translatable("gui.logtech.core.rule.status.active");
                        } else {
                            ledTooltip = Component.translatable("gui.logtech.core.rule.status.waiting");
                        }
                    }
                    guiGraphics.renderTooltip(this.font, ledTooltip, mouseX, mouseY);
                    break;
                }
            }
        }
    }
}
