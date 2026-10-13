package io.github.chema280101.logtech.client;

import io.github.chema280101.logtech.block.CoreMenu;
import io.github.chema280101.logtech.network.CoreRuleUpdatePayload;
import io.github.chema280101.logtech.network.CoreSyncPayload;
import io.github.chema280101.logtech.storage.RuleAction;
import io.github.chema280101.logtech.storage.RuleConditionType;
import io.github.chema280101.logtech.storage.RuleEngine;
import java.util.ArrayList;
import java.util.List;
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
        RULES
    }

    private Tab currentTab = Tab.STATUS;
    private Button statusTabButton;
    private Button rulesTabButton;
    private int machineScroll = 0;

    private static class RowWidgets {
        Button condButton;
        EditBox threshBox;
        Button actButton;
        Button toggleButton;
    }

    private final List<RowWidgets> ruleWidgets = new ArrayList<>(RuleEngine.MAX_RULES);

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
        int statusTabW = 54;
        int rulesTabX = statusTabX + statusTabW + 2;
        int rulesTabW = 54;

        this.statusTabButton = Button.builder(
                Component.translatable("gui.logtech.core.tab.status"),
                btn -> selectTab(Tab.STATUS)
        ).bounds(statusTabX, tabY, statusTabW, 18).build();

        this.rulesTabButton = Button.builder(
                Component.translatable("gui.logtech.core.tab.rules"),
                btn -> selectTab(Tab.RULES)
        ).bounds(rulesTabX, tabY, rulesTabW, 18).build();

        this.addRenderableWidget(this.statusTabButton);
        this.addRenderableWidget(this.rulesTabButton);

        // Widgets de las 8 filas de reglas
        this.ruleWidgets.clear();
        int startY = this.topPos + 18;

        for (int i = 0; i < RuleEngine.MAX_RULES; i++) {
            final int rowIndex = i;
            int rowY = startY + i * 15;

            RowWidgets rw = new RowWidgets();

            // Botón de condición
            rw.condButton = Button.builder(
                    getConditionButtonText(rowIndex),
                    btn -> cycleCondition(rowIndex)
            ).bounds(this.leftPos + 24, rowY, 48, 13).build();

            // Campo de umbral
            rw.threshBox = new EditBox(this.font, this.leftPos + 74, rowY, 42, 13, Component.literal("Threshold"));
            rw.threshBox.setMaxLength(10);
            rw.threshBox.setFilter(s -> s.matches("\\d*"));
            rw.threshBox.setValue(getThresholdText(rowIndex));
            rw.threshBox.setResponder(text -> onThresholdChanged(rowIndex, text));

            // Botón de acción
            rw.actButton = Button.builder(
                    getActionButtonText(rowIndex),
                    btn -> cycleAction(rowIndex)
            ).bounds(this.leftPos + 118, rowY, 50, 13).build();

            // Botón de activar/desactivar
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

        updateWidgetVisibility();
    }

    private void selectTab(Tab tab) {
        if (this.currentTab == tab) {
            return;
        }
        this.currentTab = tab;
        updateWidgetVisibility();
    }

    private void updateWidgetVisibility() {
        boolean isStatus = (this.currentTab == Tab.STATUS);
        boolean isRules = (this.currentTab == Tab.RULES);

        if (this.statusTabButton != null) {
            this.statusTabButton.active = !isStatus;
        }
        if (this.rulesTabButton != null) {
            this.rulesTabButton.active = !isRules;
        }

        for (RowWidgets rw : this.ruleWidgets) {
            if (rw.condButton != null) rw.condButton.visible = isRules;
            if (rw.threshBox != null) rw.threshBox.visible = isRules;
            if (rw.actButton != null) rw.actButton.visible = isRules;
            if (rw.toggleButton != null) rw.toggleButton.visible = isRules;
        }
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
        for (int i = 0; i < this.ruleWidgets.size(); i++) {
            RowWidgets rw = this.ruleWidgets.get(i);
            rw.condButton.setMessage(getConditionButtonText(i));
            rw.actButton.setMessage(getActionButtonText(i));
            rw.toggleButton.setMessage(getToggleButtonText(i));
            if (!rw.threshBox.isFocused()) {
                rw.threshBox.setValue(getThresholdText(i));
            }
        }
    }

    private void cycleCondition(int index) {
        CoreSyncPayload.RuleDto r = getRuleDto(index);
        int nextOrd = (r != null ? r.conditionOrdinal() + 1 : 1) % RuleConditionType.values().length;
        sendUpdate(index, r != null ? r.item() : ItemStack.EMPTY, nextOrd, r != null ? r.threshold() : 0L, r != null ? r.actionOrdinal() : 0, r == null || r.enabled());
    }

    private void cycleAction(int index) {
        CoreSyncPayload.RuleDto r = getRuleDto(index);
        int nextOrd = (r != null ? r.actionOrdinal() + 1 : 1) % RuleAction.values().length;
        sendUpdate(index, r != null ? r.item() : ItemStack.EMPTY, r != null ? r.conditionOrdinal() : 0, r != null ? r.threshold() : 0L, nextOrd, r == null || r.enabled());
    }

    private void toggleEnabled(int index) {
        CoreSyncPayload.RuleDto r = getRuleDto(index);
        boolean nextEnabled = r == null || !r.enabled();
        sendUpdate(index, r != null ? r.item() : ItemStack.EMPTY, r != null ? r.conditionOrdinal() : 0, r != null ? r.threshold() : 0L, r != null ? r.actionOrdinal() : 0, nextEnabled);
    }

    private void onThresholdChanged(int index, String text) {
        long val = 0L;
        try {
            if (!text.isEmpty()) {
                val = Long.parseLong(text);
            }
        } catch (NumberFormatException ignored) {
        }
        CoreSyncPayload.RuleDto r = getRuleDto(index);
        sendUpdate(index, r != null ? r.item() : ItemStack.EMPTY, r != null ? r.conditionOrdinal() : 0, val, r != null ? r.actionOrdinal() : 0, r == null || r.enabled());
    }

    private void sendUpdate(int index, ItemStack item, int condOrd, long threshold, int actOrd, boolean enabled) {
        PacketDistributor.sendToServer(new CoreRuleUpdatePayload(
                this.menu.containerId,
                index,
                item != null ? item : ItemStack.EMPTY,
                condOrd,
                threshold,
                actOrd,
                enabled
        ));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.currentTab == Tab.RULES) {
            int startY = this.topPos + 18;
            for (int i = 0; i < RuleEngine.MAX_RULES; i++) {
                int ghostX = this.leftPos + 6;
                int ghostY = startY + i * 15;
                if (mouseX >= ghostX && mouseX <= ghostX + 16 && mouseY >= ghostY && mouseY <= ghostY + 16) {
                    ItemStack carried = this.menu.getCarried();
                    CoreSyncPayload.RuleDto r = getRuleDto(i);
                    if (!carried.isEmpty()) {
                        // Asignar copia del ítem
                        sendUpdate(i, carried.copyWithCount(1), r != null ? r.conditionOrdinal() : 0, r != null ? r.threshold() : 0L, r != null ? r.actionOrdinal() : 0, r == null || r.enabled());
                    } else {
                        // Limpiar ranura
                        sendUpdate(i, ItemStack.EMPTY, r != null ? r.conditionOrdinal() : 0, r != null ? r.threshold() : 0L, r != null ? r.actionOrdinal() : 0, r == null || r.enabled());
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.currentTab == Tab.RULES) {
            int startY = this.topPos + 18;
            for (int i = 0; i < RuleEngine.MAX_RULES; i++) {
                int ghostX = this.leftPos + 6;
                int ghostY = startY + i * 15;
                if (mouseX >= ghostX && mouseX <= ghostX + 16 && mouseY >= ghostY && mouseY <= ghostY + 16) {
                    ItemStack carried = this.menu.getCarried();
                    if (!carried.isEmpty()) {
                        CoreSyncPayload.RuleDto r = getRuleDto(i);
                        sendUpdate(i, carried.copyWithCount(1), r != null ? r.conditionOrdinal() : 0, r != null ? r.threshold() : 0L, r != null ? r.actionOrdinal() : 0, r == null || r.enabled());
                        return true;
                    }
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.currentTab == Tab.STATUS) {
            List<CoreSyncPayload.MachineInfo> machines = this.menu.getClientMachines();
            if (machines != null && machines.size() > 3) {
                int maxScroll = machines.size() - 3;
                if (scrollY > 0) {
                    this.machineScroll = Math.max(0, this.machineScroll - 1);
                    return true;
                } else if (scrollY < 0) {
                    this.machineScroll = Math.min(maxScroll, this.machineScroll + 1);
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component currentTitle = (this.currentTab == Tab.STATUS)
                ? Component.translatable("gui.logtech.core.tab.status")
                : Component.translatable("gui.logtech.core.tab.rules");
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
        } else {
            renderRulesTab(guiGraphics, mouseX, mouseY);
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

            // Ranura fantasma
            guiGraphics.fill(ghostX, rowY, ghostX + 16, rowY + 16, 0xFF121216);
            guiGraphics.renderOutline(ghostX - 1, rowY - 1, 18, 18, 0xFF2A2A34);

            CoreSyncPayload.RuleDto r = getRuleDto(i);
            if (r != null && r.item() != null && !r.item().isEmpty()) {
                guiGraphics.renderFakeItem(r.item(), ghostX, rowY);
            }

            if (mouseX >= ghostX && mouseX <= ghostX + 16 && mouseY >= rowY && mouseY <= rowY + 16) {
                guiGraphics.fill(ghostX, rowY, ghostX + 16, rowY + 16, 0x40FFFFFF);
            }

            // Indicador LED de estado de regla
            int ledX = this.leftPos + 203;
            int ledY = rowY + 4;
            int ledColor = 0xFF2A2A2A; // Apagado por defecto

            if (r != null && r.enabled()) {
                RuleConditionType cond = RuleConditionType.values()[Math.max(0, Math.min(RuleConditionType.values().length - 1, r.conditionOrdinal()))];
                RuleAction act = RuleAction.values()[Math.max(0, Math.min(RuleAction.values().length - 1, r.actionOrdinal()))];
                boolean missingItem = (cond.requiresItem() || act.requiresItem()) && (r.item() == null || r.item().isEmpty());

                if (missingItem) {
                    ledColor = 0xFFAA3333; // Rojo oscuro: ignorada por falta de ítem
                } else if (r.active()) {
                    ledColor = 0xFF00E676; // Verde brillante: activa
                } else {
                    ledColor = 0xFF555555; // Gris: en espera
                }
            }

            guiGraphics.fill(ledX, ledY, ledX + 5, ledY + 5, ledColor);
            guiGraphics.renderOutline(ledX, ledY, 5, 5, 0xFF111111);
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

                // Tooltip de ranura fantasma
                if (mouseX >= ghostX && mouseX <= ghostX + 16 && mouseY >= rowY && mouseY <= rowY + 16) {
                    CoreSyncPayload.RuleDto r = getRuleDto(i);
                    if (r != null && r.item() != null && !r.item().isEmpty()) {
                        guiGraphics.renderTooltip(this.font, r.item(), mouseX, mouseY);
                    } else {
                        guiGraphics.renderTooltip(this.font, Component.translatable("gui.logtech.core.rule.ghost_slot_tooltip"), mouseX, mouseY);
                    }
                    break;
                }

                // Tooltip del LED indicador
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
