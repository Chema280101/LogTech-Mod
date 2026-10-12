package io.github.chema280101.logtech.client;

import io.github.chema280101.logtech.block.TerminalMenu;
import io.github.chema280101.logtech.network.TerminalActionPayload;
import io.github.chema280101.logtech.network.TerminalStatsPayload;
import io.github.chema280101.logtech.network.TerminalTabPayload;
import io.github.chema280101.logtech.storage.TerminalItemEntry;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
    private static final ResourceLocation CONTAINER_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public enum Tab {
        ITEMS,
        STATS
    }

    public enum SortMode {
        AMOUNT,
        NAME
    }

    private static SortMode currentSortMode = SortMode.AMOUNT;

    private Tab currentTab = Tab.ITEMS;
    private Button itemsTabButton;
    private Button statsTabButton;
    private int machineScroll = 0;

    private final List<TerminalItemEntry> filteredItems = new ArrayList<>();
    private EditBox searchBox;
    private Button sortButton;
    private int scrollRow = 0;

    public TerminalScreen(TerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = 129;
    }

    @Override
    protected void init() {
        super.init();
        this.menu.setOnItemsUpdatedCallback(this::onItemsUpdated);
        this.menu.setOnStatsUpdatedCallback(this::onStatsUpdated);

        String prevSearch = this.searchBox != null ? this.searchBox.getValue() : "";

        // Botones de pestañas superiores
        int tabButtonY = this.topPos - 18;
        int itemsTabX = this.leftPos + 6;
        int itemsTabW = 48;
        int statsTabX = itemsTabX + itemsTabW + 2;
        int statsTabW = 78;

        this.itemsTabButton = Button.builder(
                Component.translatable("gui.logtech.terminal.tab.items"),
                btn -> selectTab(Tab.ITEMS)
        ).bounds(itemsTabX, tabButtonY, itemsTabW, 18).build();

        this.statsTabButton = Button.builder(
                Component.translatable("gui.logtech.terminal.tab.stats"),
                btn -> selectTab(Tab.STATS)
        ).bounds(statsTabX, tabButtonY, statsTabW, 18).build();

        this.addRenderableWidget(this.itemsTabButton);
        this.addRenderableWidget(this.statsTabButton);

        // Controles de la pestaña Ítems
        int sortButtonWidth = 24;
        int sortButtonHeight = 13;
        int sortButtonX = this.leftPos + 168 - sortButtonWidth;
        int sortButtonY = this.topPos + 3;

        this.sortButton = Button.builder(
                getSortButtonText(),
                btn -> {
                    currentSortMode = (currentSortMode == SortMode.AMOUNT) ? SortMode.NAME : SortMode.AMOUNT;
                    updateSortButton();
                    refreshFilteredItems();
                }
        )
        .bounds(sortButtonX, sortButtonY, sortButtonWidth, sortButtonHeight)
        .tooltip(getSortButtonTooltip())
        .build();

        int searchX = this.leftPos + 74;
        int searchY = this.topPos + 3;
        int searchWidth = sortButtonX - searchX - 3;
        int searchHeight = 12;

        this.searchBox = new EditBox(
                this.font,
                searchX,
                searchY,
                searchWidth,
                searchHeight,
                Component.translatable("gui.logtech.terminal.search")
        );
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(true);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(0xFFFFFF);
        this.searchBox.setHint(
                Component.translatable("gui.logtech.terminal.search_hint").withStyle(ChatFormatting.DARK_GRAY)
        );
        this.searchBox.setValue(prevSearch);
        this.searchBox.setResponder(text -> {
            this.scrollRow = 0;
            refreshFilteredItems();
        });

        this.addRenderableWidget(this.searchBox);
        this.addRenderableWidget(this.sortButton);

        updateWidgetVisibility();
        refreshFilteredItems();
    }

    private void selectTab(Tab tab) {
        if (this.currentTab == tab) {
            return;
        }
        this.currentTab = tab;
        boolean isStats = (tab == Tab.STATS);
        PacketDistributor.sendToServer(new TerminalTabPayload(this.menu.containerId, isStats));
        updateWidgetVisibility();
    }

    private void updateWidgetVisibility() {
        boolean isItems = (this.currentTab == Tab.ITEMS);
        boolean active = this.menu.isNetworkValid() && this.menu.isPowered();

        if (this.itemsTabButton != null) {
            this.itemsTabButton.active = !isItems;
        }
        if (this.statsTabButton != null) {
            this.statsTabButton.active = isItems;
        }

        if (this.searchBox != null) {
            this.searchBox.visible = isItems && active;
        }
        if (this.sortButton != null) {
            this.sortButton.visible = isItems && active;
        }
    }

    private void updateSortButton() {
        if (this.sortButton != null) {
            this.sortButton.setMessage(getSortButtonText());
            this.sortButton.setTooltip(getSortButtonTooltip());
        }
    }

    private Component getSortButtonText() {
        return currentSortMode == SortMode.AMOUNT
                ? Component.translatable("gui.logtech.terminal.sort.amount_icon")
                : Component.translatable("gui.logtech.terminal.sort.name_icon");
    }

    private Tooltip getSortButtonTooltip() {
        return Tooltip.create(
                currentSortMode == SortMode.AMOUNT
                        ? Component.translatable("gui.logtech.terminal.sort.by_amount")
                        : Component.translatable("gui.logtech.terminal.sort.by_name")
        );
    }

    private void onItemsUpdated() {
        refreshFilteredItems();
    }

    private void onStatsUpdated() {
        // Callback al recibir nuevas estadísticas del servidor
    }

    private void refreshFilteredItems() {
        List<TerminalItemEntry> raw = this.menu.getClientItems();
        this.filteredItems.clear();

        String query = this.searchBox != null ? this.searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        String normalizedQuery = stripAccents(query);

        for (TerminalItemEntry entry : raw) {
            if (query.isEmpty()) {
                this.filteredItems.add(entry);
            } else {
                String name = entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT);
                if (name.contains(query) || stripAccents(name).contains(normalizedQuery)) {
                    this.filteredItems.add(entry);
                }
            }
        }

        if (currentSortMode == SortMode.AMOUNT) {
            this.filteredItems.sort((a, b) -> {
                int cmp = Long.compare(b.amount(), a.amount());
                if (cmp != 0) {
                    return cmp;
                }
                return a.stack().getHoverName().getString().compareToIgnoreCase(b.stack().getHoverName().getString());
            });
        } else {
            this.filteredItems.sort((a, b) -> {
                int cmp = a.stack().getHoverName().getString().compareToIgnoreCase(b.stack().getHoverName().getString());
                if (cmp != 0) {
                    return cmp;
                }
                return Long.compare(b.amount(), a.amount());
            });
        }

        int totalRows = (int) Math.ceil(this.filteredItems.size() / 9.0);
        int maxScroll = Math.max(0, totalRows - 6);
        if (this.scrollRow > maxScroll) {
            this.scrollRow = maxScroll;
        }
    }

    private static String stripAccents(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    @Override
    public void onClose() {
        if (this.currentTab == Tab.STATS) {
            PacketDistributor.sendToServer(new TerminalTabPayload(this.menu.containerId, false));
        }
        super.onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.currentTab == Tab.STATS) {
            TerminalStatsPayload stats = this.menu.getClientStats();
            if (stats != null) {
                int maxMachScroll = Math.max(0, stats.machines().size() - 3);
                if (maxMachScroll > 0) {
                    if (scrollY > 0) {
                        this.machineScroll = Math.max(0, this.machineScroll - 1);
                        return true;
                    } else if (scrollY < 0) {
                        this.machineScroll = Math.min(maxMachScroll, this.machineScroll + 1);
                        return true;
                    }
                }
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int totalRows = (int) Math.ceil(this.filteredItems.size() / 9.0);
        int maxScroll = Math.max(0, totalRows - 6);
        if (maxScroll > 0) {
            if (scrollY > 0) {
                this.scrollRow = Math.max(0, this.scrollRow - 1);
                return true;
            } else if (scrollY < 0) {
                this.scrollRow = Math.min(maxScroll, this.scrollRow + 1);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component currentTitle = (this.currentTab == Tab.STATS)
                ? Component.translatable("gui.logtech.terminal.tab.stats")
                : this.title;
        guiGraphics.drawString(this.font, currentTitle, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(CONTAINER_BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.currentTab == Tab.STATS) {
            // Fondo oscuro para la pestaña de estadísticas sobre el área de ranuras
            guiGraphics.fill(this.leftPos + 7, this.topPos + 17, this.leftPos + 169, this.topPos + 129, 0xFF141418);
            guiGraphics.renderOutline(this.leftPos + 7, this.topPos + 17, 162, 112, 0xFF2A2A36);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateWidgetVisibility();
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.currentTab == Tab.ITEMS) {
            renderItemsTab(guiGraphics, mouseX, mouseY);
        } else {
            renderStatsTab(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderItemsTab(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean valid = this.menu.isNetworkValid();
        boolean powered = this.menu.isPowered();

        TerminalItemEntry hoveredEntry = null;
        int hoveredSlotX = 0;
        int hoveredSlotY = 0;

        if (!valid) {
            Component invalidComp = Component.translatable("gui.logtech.terminal.network_invalid");
            guiGraphics.drawString(this.font, invalidComp, this.leftPos + 8, this.topPos + 20, 0xD32F2F, false);
        } else if (!powered) {
            Component noEnergyComp = Component.translatable("gui.logtech.terminal.no_energy");
            guiGraphics.drawString(this.font, noEnergyComp, this.leftPos + 8, this.topPos + 20, 0xD32F2F, false);
        } else {
            List<TerminalItemEntry> items = this.filteredItems;
            int totalRows = (int) Math.ceil(items.size() / 9.0);
            int maxScroll = Math.max(0, totalRows - 6);

            if (maxScroll > 0) {
                String pageInfo = (this.scrollRow + 1) + "/" + (maxScroll + 1);
                guiGraphics.drawString(
                        this.font,
                        pageInfo,
                        this.leftPos + this.imageWidth - 8 - this.font.width(pageInfo),
                        this.topPos + 129,
                        0x555555,
                        false
                );
            }

            int startIndex = this.scrollRow * 9;
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 9; col++) {
                    int index = startIndex + row * 9 + col;
                    if (index >= items.size()) {
                        break;
                    }
                    TerminalItemEntry entry = items.get(index);
                    int slotX = this.leftPos + 8 + col * 18;
                    int slotY = this.topPos + 18 + row * 18;

                    guiGraphics.renderItem(entry.stack(), slotX, slotY);
                    renderItemCount(guiGraphics, entry.amount(), slotX, slotY);

                    if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                        hoveredEntry = entry;
                        hoveredSlotX = slotX;
                        hoveredSlotY = slotY;
                    }
                }
            }

            if (hoveredEntry != null) {
                guiGraphics.fillGradient(hoveredSlotX, hoveredSlotY, hoveredSlotX + 16, hoveredSlotY + 16, 0x80FFFFFF, 0x80FFFFFF);
            }
        }

        if (hoveredEntry != null && this.menu.getCarried().isEmpty()) {
            List<Component> tooltip = new ArrayList<>(getTooltipFromItem(this.minecraft, hoveredEntry.stack()));
            tooltip.add(
                    Component.translatable(
                            "gui.logtech.terminal.stored_amount",
                            String.format(Locale.ROOT, "%,d", hoveredEntry.amount())
                    ).withStyle(ChatFormatting.GRAY)
            );
            guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        } else {
            renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderStatsTab(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean valid = this.menu.isNetworkValid();
        boolean powered = this.menu.isPowered();

        if (!valid) {
            Component invalidComp = Component.translatable("gui.logtech.terminal.network_invalid");
            guiGraphics.drawString(this.font, invalidComp, this.leftPos + 10, this.topPos + 22, 0xD32F2F, false);
            renderTooltip(guiGraphics, mouseX, mouseY);
            return;
        }
        if (!powered) {
            Component noEnergyComp = Component.translatable("gui.logtech.terminal.no_energy");
            guiGraphics.drawString(this.font, noEnergyComp, this.leftPos + 10, this.topPos + 22, 0xD32F2F, false);
            renderTooltip(guiGraphics, mouseX, mouseY);
            return;
        }

        TerminalStatsPayload stats = this.menu.getClientStats();
        if (stats == null) {
            Component loadingComp = Component.translatable("gui.logtech.terminal.stats.loading");
            guiGraphics.drawString(this.font, loadingComp, this.leftPos + 10, this.topPos + 22, 0x888888, false);
            renderTooltip(guiGraphics, mouseX, mouseY);
            return;
        }

        int left = this.leftPos + 10;

        // 1. Capacidad usada y total con barra
        long used = stats.usedCapacity();
        long total = stats.totalCapacity();
        double pct = total > 0 ? (used * 100.0 / total) : 0.0;
        Component storageComp = Component.translatable(
                "gui.logtech.terminal.stats.storage",
                String.format(Locale.ROOT, "%,d", used),
                String.format(Locale.ROOT, "%,d", total),
                String.format(Locale.ROOT, "%.1f", pct)
        );
        drawFittedString(guiGraphics, storageComp, left, this.topPos + 19, 154, 0xEEEEEE);

        int barX = left;
        int barY = this.topPos + 29;
        int barW = 154;
        int barH = 5;
        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF222228);
        guiGraphics.renderOutline(barX - 1, barY - 1, barW + 2, barH + 2, 0xFF3E3E4C);
        int fillW = total > 0 ? (int) Math.min(barW, Math.round((double) used / total * barW)) : 0;
        int fillColor = used > total ? 0xFFE53935 : (pct >= 90.0 ? 0xFFFFA726 : 0xFF00ADB5);
        if (fillW > 0) {
            guiGraphics.fill(barX, barY, barX + fillW, barY + barH, fillColor);
        }

        // 2. Items insertados por minuto (último minuto y media de 5 min)
        Component rateComp = Component.translatable(
                "gui.logtech.terminal.stats.items_per_minute",
                stats.itemsPerMinuteLast(),
                String.format(Locale.ROOT, "%.1f", stats.itemsPerMinute5MinAvg())
        );
        drawFittedString(guiGraphics, rateComp, left, this.topPos + 38, 154, 0x00E5FF);

        // 3. Energía del Núcleo (almacenada y consumo)
        Component energyComp = Component.translatable(
                "gui.logtech.terminal.stats.energy",
                String.format(Locale.ROOT, "%,d", stats.energyStored()),
                String.format(Locale.ROOT, "%,d", stats.energyCapacity()),
                stats.energyConsumption()
        );
        drawFittedString(guiGraphics, energyComp, left, this.topPos + 48, 154, 0xFFD54F);

        // 4. Gráfica de barras de los últimos 60 minutos de items insertados
        guiGraphics.drawString(
                this.font,
                Component.translatable("gui.logtech.terminal.stats.chart_title"),
                left,
                this.topPos + 59,
                0xAAAAAA,
                false
        );

        int chartX = left;
        int chartY = this.topPos + 69;
        int chartW = 120;
        int chartH = 18;
        guiGraphics.fill(chartX, chartY, chartX + chartW, chartY + chartH, 0xFF18181E);
        guiGraphics.renderOutline(chartX - 1, chartY - 1, chartW + 2, chartH + 2, 0xFF353545);

        int[] hist = stats.hourlyHistory();
        int maxVal = 1;
        for (int v : hist) {
            if (v > maxVal) maxVal = v;
        }

        for (int i = 0; i < 60; i++) {
            int v = (i < hist.length) ? hist[i] : 0;
            int bH = (v > 0) ? Math.max(1, (int) Math.round(((double) v / maxVal) * (chartH - 2))) : 0;
            int bX = chartX + i * 2;
            int bY = chartY + chartH - 1 - bH;
            int bColor = (i == 59) ? 0xFF00E5FF : 0xFF0288D1;
            if (bH > 0) {
                guiGraphics.fill(bX, bY, bX + 2, chartY + chartH - 1, bColor);
            }
        }

        // Escala del gráfico a la derecha
        String maxStr = maxVal + "";
        guiGraphics.drawString(this.font, maxStr, left + 124, chartY, 0x777777, false);
        guiGraphics.drawString(this.font, "0", left + 124, chartY + 9, 0x555555, false);

        // 5. Lista de máquinas
        List<TerminalStatsPayload.MachineStatEntry> machines = stats.machines();
        int machLabelY = this.topPos + 90;
        Component machTitle = Component.translatable("gui.logtech.terminal.stats.machines_title", machines.size());
        guiGraphics.drawString(this.font, machTitle, left, machLabelY, 0xAAAAAA, false);

        if (machines.isEmpty()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("gui.logtech.terminal.stats.no_machines"),
                    left,
                    machLabelY + 10,
                    0x666666,
                    false
            );
        } else {
            int maxMachScroll = Math.max(0, machines.size() - 3);
            if (maxMachScroll > 0) {
                String pageStr = (this.machineScroll + 1) + "-" + Math.min(this.machineScroll + 3, machines.size()) + "/" + machines.size();
                guiGraphics.drawString(
                        this.font,
                        pageStr,
                        this.leftPos + this.imageWidth - 12 - this.font.width(pageStr),
                        machLabelY,
                        0x666666,
                        false
                );
            }

            for (int row = 0; row < 3; row++) {
                int mIdx = this.machineScroll + row;
                if (mIdx >= machines.size()) {
                    break;
                }
                TerminalStatsPayload.MachineStatEntry m = machines.get(mIdx);
                int lineY = machLabelY + 10 + row * 10;
                Component lineComp = Component.translatable(
                        "gui.logtech.terminal.stats.machine_entry",
                        m.name(),
                        m.status().getCauseComponent(),
                        m.itemsPerMinute()
                );
                int textColor = m.status().isActive() ? 0x81C784 : 0xE57373;
                drawFittedString(guiGraphics, lineComp, left, lineY, 154, textColor);
            }
        }

        // Tooltip en la gráfica si el ratón está encima
        if (mouseX >= chartX && mouseX < chartX + chartW && mouseY >= chartY && mouseY < chartY + chartH) {
            int idx = (mouseX - chartX) / 2;
            if (idx >= 0 && idx < 60) {
                int v = (idx < hist.length) ? hist[idx] : 0;
                int minsAgo = 59 - idx;
                Component tip = (minsAgo == 0)
                        ? Component.translatable("gui.logtech.terminal.stats.chart_tooltip_current", v)
                        : Component.translatable("gui.logtech.terminal.stats.chart_tooltip_past", minsAgo, v);
                guiGraphics.renderTooltip(this.font, tip, mouseX, mouseY);
                return;
            }
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawFittedString(GuiGraphics guiGraphics, Component comp, int x, int y, int maxWidth, int color) {
        int strWidth = this.font.width(comp);
        if (strWidth > maxWidth && maxWidth > 0) {
            float scale = (float) maxWidth / strWidth;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            guiGraphics.pose().scale(scale, scale, 1.0F);
            guiGraphics.drawString(this.font, comp, 0, 0, color, false);
            guiGraphics.pose().popPose();
        } else {
            guiGraphics.drawString(this.font, comp, x, y, color, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.itemsTabButton != null && this.itemsTabButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.statsTabButton != null && this.statsTabButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (this.currentTab == Tab.STATS) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (this.searchBox != null && this.searchBox.visible && this.searchBox.isMouseOver(mouseX, mouseY) && button == 1) {
            this.searchBox.setValue("");
            return true;
        }

        if (this.searchBox != null && this.searchBox.visible && this.searchBox.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.searchBox);
            return true;
        }

        if (this.sortButton != null && this.sortButton.visible && this.sortButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (this.searchBox != null && this.searchBox.isFocused()) {
            this.searchBox.setFocused(false);
        }

        if (!this.menu.isNetworkValid() || !this.menu.isPowered()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int gridLeft = this.leftPos + 8;
        int gridTop = this.topPos + 18;
        int gridRight = gridLeft + 9 * 18;
        int gridBottom = gridTop + 6 * 18;

        boolean inTerminalArea = mouseX >= this.leftPos && mouseX < this.leftPos + this.imageWidth
                && mouseY >= this.topPos && mouseY < this.topPos + 126;

        if (inTerminalArea) {
            if (!this.menu.getCarried().isEmpty()) {
                if (button == 0) {
                    PacketDistributor.sendToServer(new TerminalActionPayload(
                            this.menu.containerId,
                            TerminalActionPayload.ActionType.INSERT_CURSOR_ALL,
                            0,
                            ItemStack.EMPTY
                    ));
                    return true;
                } else if (button == 1) {
                    PacketDistributor.sendToServer(new TerminalActionPayload(
                            this.menu.containerId,
                            TerminalActionPayload.ActionType.INSERT_CURSOR_ONE,
                            0,
                            ItemStack.EMPTY
                    ));
                    return true;
                }
            } else if (mouseX >= gridLeft && mouseX < gridRight && mouseY >= gridTop && mouseY < gridBottom) {
                int col = (int) (mouseX - gridLeft) / 18;
                int row = (int) (mouseY - gridTop) / 18;
                int index = (this.scrollRow * 9) + (row * 9 + col);
                List<TerminalItemEntry> items = this.filteredItems;
                if (index >= 0 && index < items.size()) {
                    TerminalItemEntry entry = items.get(index);
                    if (hasShiftDown()) {
                        PacketDistributor.sendToServer(new TerminalActionPayload(
                                this.menu.containerId,
                                TerminalActionPayload.ActionType.EXTRACT_SHIFT,
                                0,
                                entry.stack()
                        ));
                        return true;
                    } else if (button == 0) {
                        PacketDistributor.sendToServer(new TerminalActionPayload(
                                this.menu.containerId,
                                TerminalActionPayload.ActionType.EXTRACT_STACK,
                                0,
                                entry.stack()
                        ));
                        return true;
                    } else if (button == 1) {
                        PacketDistributor.sendToServer(new TerminalActionPayload(
                                this.menu.containerId,
                                TerminalActionPayload.ActionType.EXTRACT_HALF,
                                0,
                                entry.stack()
                        ));
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.currentTab == Tab.ITEMS && this.searchBox != null && this.searchBox.canConsumeInput()) {
            if (keyCode == 256) { // Escape
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.closeContainer();
                }
                return true;
            }
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.currentTab == Tab.ITEMS && this.searchBox != null && this.searchBox.canConsumeInput()) {
            if (this.searchBox.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (this.currentTab == Tab.STATS && type == ClickType.QUICK_MOVE) {
            return;
        }
        if (type == ClickType.QUICK_MOVE && slot != null) {
            if (!this.menu.isNetworkValid() || !this.menu.isPowered()) {
                return;
            }
            PacketDistributor.sendToServer(new TerminalActionPayload(
                    this.menu.containerId,
                    TerminalActionPayload.ActionType.INSERT_SLOT,
                    slot.index,
                    ItemStack.EMPTY
            ));
            return;
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    private void renderItemCount(GuiGraphics guiGraphics, long amount, int x, int y) {
        String text = TerminalItemEntry.formatAmount(amount);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 200.0F);
        if (text.length() <= 2) {
            int width = this.font.width(text);
            guiGraphics.drawString(this.font, text, 17 - width, 9, 0xFFFFFF, true);
        } else {
            float scale = 0.65F;
            guiGraphics.pose().scale(scale, scale, 1.0F);
            int scaledWidth = this.font.width(text);
            float posX = (17.0F / scale) - scaledWidth;
            float posY = (16.0F / scale) - this.font.lineHeight;
            guiGraphics.drawString(this.font, text, (int) posX, (int) posY, 0xFFFFFF, true);
        }
        guiGraphics.pose().popPose();
    }
}
