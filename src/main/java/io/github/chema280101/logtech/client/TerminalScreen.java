package io.github.chema280101.logtech.client;

import io.github.chema280101.logtech.block.TerminalMenu;
import io.github.chema280101.logtech.machine.MachineStatus;
import io.github.chema280101.logtech.network.EnergyPriority;
import io.github.chema280101.logtech.network.TerminalActionPayload;
import io.github.chema280101.logtech.network.TerminalEnergyPayload;
import io.github.chema280101.logtech.network.TerminalPriorityPayload;
import io.github.chema280101.logtech.network.TerminalStatsPayload;
import io.github.chema280101.logtech.network.TerminalLogPagePayload;
import io.github.chema280101.logtech.network.TerminalLogRequestPayload;
import io.github.chema280101.logtech.network.TerminalTabPayload;
import io.github.chema280101.logtech.network.TerminalViewUpdatePayload;
import io.github.chema280101.logtech.registry.ModDataComponents;
import io.github.chema280101.logtech.security.ActivityAction;
import io.github.chema280101.logtech.storage.RateTracker;
import io.github.chema280101.logtech.storage.SavedView;
import io.github.chema280101.logtech.storage.TerminalItemEntry;
import io.github.chema280101.logtech.storage.ViewManager;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
    private static final ResourceLocation CONTAINER_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public enum Tab {
        ITEMS,
        STATS,
        ENERGY,
        TRASH,
        LOG
    }

    public enum SortMode {
        AMOUNT,
        NAME
    }

    private static SortMode currentSortMode = SortMode.AMOUNT;

    // Filtros persistentes por sesión (solo cliente)
    private static int currentTierFilter = SavedView.TIER_ALL;   // 0=todos, 1=caliente, 2=frío
    private static int currentMinPurity  = 0;                     // 0=sin filtro, 1-5=pureza mínima

    private Tab currentTab = Tab.ITEMS;
    private Button itemsTabButton;
    private Button statsTabButton;
    private Button energyTabButton;
    private Button trashTabButton;
    private Button logTabButton;
    private Button destroyButton;
    private int machineScroll = 0;
    private int energyMachineScroll = 0;
    private int trashScrollRow = 0;

    private EditBox logFilterBox;
    private Button prevLogPageButton;
    private Button nextLogPageButton;

    private final List<TerminalItemEntry> filteredItems = new ArrayList<>();
    private EditBox searchBox;
    private Button sortButton;
    private Button tierFilterButton;
    private Button purityFilterButton;
    private final Button[] viewButtons = new Button[ViewManager.MAX_VIEWS];
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
        this.menu.setOnEnergyUpdatedCallback(this::onEnergyUpdated);
        this.menu.setOnTrashUpdatedCallback(this::onTrashUpdated);
        this.menu.setOnViewsUpdatedCallback(this::onViewsUpdated);
        this.menu.setOnLogUpdatedCallback(this::onLogUpdated);

        String prevSearch = this.searchBox != null ? this.searchBox.getValue() : "";
        String prevFilter = this.logFilterBox != null ? this.logFilterBox.getValue() : "";

        boolean canViewLog = this.menu.canClientViewLog();

        // Botones de pestañas superiores (Ítems, Estadísticas, Energía, Papelera, Registro)
        int tabButtonY = this.topPos - 18;
        int itemsTabX, itemsTabW, statsTabX, statsTabW, energyTabX, energyTabW, trashTabX, trashTabW;
        int logTabX = 0, logTabW = 0;

        if (canViewLog) {
            itemsTabX = this.leftPos + 3;
            itemsTabW = 32;
            statsTabX = itemsTabX + itemsTabW + 2;
            statsTabW = 32;
            energyTabX = statsTabX + statsTabW + 2;
            energyTabW = 34;
            trashTabX = energyTabX + energyTabW + 2;
            trashTabW = 34;
            logTabX = trashTabX + trashTabW + 2;
            logTabW = 32;
        } else {
            itemsTabX = this.leftPos + 4;
            itemsTabW = 38;
            statsTabX = itemsTabX + itemsTabW + 2;
            statsTabW = 40;
            energyTabX = statsTabX + statsTabW + 2;
            energyTabW = 44;
            trashTabX = energyTabX + energyTabW + 2;
            trashTabW = 42;
        }

        this.itemsTabButton = Button.builder(
                Component.translatable(canViewLog ? "gui.logtech.terminal.tab.items_short" : "gui.logtech.terminal.tab.items"),
                btn -> selectTab(Tab.ITEMS)
        ).bounds(itemsTabX, tabButtonY, itemsTabW, 18)
         .tooltip(Tooltip.create(Component.translatable("gui.logtech.terminal.tab.items")))
         .build();

        this.statsTabButton = Button.builder(
                Component.translatable(canViewLog ? "gui.logtech.terminal.tab.stats_short" : "gui.logtech.terminal.tab.stats"),
                btn -> selectTab(Tab.STATS)
        ).bounds(statsTabX, tabButtonY, statsTabW, 18)
         .tooltip(Tooltip.create(Component.translatable("gui.logtech.terminal.tab.stats")))
         .build();

        this.energyTabButton = Button.builder(
                Component.translatable(canViewLog ? "gui.logtech.terminal.tab.energy_short" : "gui.logtech.terminal.tab.energy"),
                btn -> selectTab(Tab.ENERGY)
        ).bounds(energyTabX, tabButtonY, energyTabW, 18)
         .tooltip(Tooltip.create(Component.translatable("gui.logtech.terminal.tab.energy")))
         .build();

        this.trashTabButton = Button.builder(
                Component.translatable(canViewLog ? "gui.logtech.terminal.tab.trash_short" : "gui.logtech.terminal.tab.trash"),
                btn -> selectTab(Tab.TRASH)
        ).bounds(trashTabX, tabButtonY, trashTabW, 18)
         .tooltip(Tooltip.create(Component.translatable("gui.logtech.terminal.tab.trash")))
         .build();

        if (canViewLog) {
            this.logTabButton = Button.builder(
                    Component.translatable("gui.logtech.terminal.tab.log_short"),
                    btn -> selectTab(Tab.LOG)
            ).bounds(logTabX, tabButtonY, logTabW, 18)
             .tooltip(Tooltip.create(Component.translatable("gui.logtech.terminal.tab.log")))
             .build();
            this.addRenderableWidget(this.logTabButton);
        }

        this.destroyButton = Button.builder(
                Component.translatable("gui.logtech.terminal.destroy").withStyle(ChatFormatting.RED),
                btn -> {
                    if (!this.menu.getCarried().isEmpty()) {
                        PacketDistributor.sendToServer(new TerminalActionPayload(
                                this.menu.containerId,
                                TerminalActionPayload.ActionType.DESTROY_CURSOR,
                                0,
                                this.menu.getCarried()
                        ));
                    }
                }
        )
        .bounds(this.leftPos + 116, this.topPos + 124, 52, 14)
        .tooltip(Tooltip.create(Component.translatable("gui.logtech.terminal.destroy_tooltip")))
        .build();

        this.addRenderableWidget(this.itemsTabButton);
        this.addRenderableWidget(this.statsTabButton);
        this.addRenderableWidget(this.energyTabButton);
        this.addRenderableWidget(this.trashTabButton);
        this.addRenderableWidget(this.destroyButton);

        // Controles de la pestaña Registro
        int logFilterW = 92;
        int logFilterH = 12;
        int logFilterX = this.leftPos + 8;
        int logFilterY = this.topPos + 3;

        this.logFilterBox = new EditBox(
                this.font,
                logFilterX,
                logFilterY,
                logFilterW,
                logFilterH,
                Component.translatable("gui.logtech.terminal.log.filter_hint")
        );
        this.logFilterBox.setMaxLength(30);
        this.logFilterBox.setBordered(true);
        this.logFilterBox.setVisible(false);
        this.logFilterBox.setTextColor(0xFFFFFF);
        this.logFilterBox.setHint(
                Component.translatable("gui.logtech.terminal.log.filter_hint").withStyle(ChatFormatting.DARK_GRAY)
        );
        this.logFilterBox.setValue(prevFilter);
        this.logFilterBox.setResponder(text -> {
            PacketDistributor.sendToServer(new TerminalLogRequestPayload(this.menu.containerId, 0, text));
        });

        int navBtnW = 14;
        int navBtnH = 13;
        int prevBtnX = this.leftPos + 104;
        int nextBtnX = this.leftPos + 154;
        int navBtnY = this.topPos + 3;

        this.prevLogPageButton = Button.builder(
                Component.literal("<"),
                btn -> {
                    int cur = this.menu.getClientLogPage();
                    if (cur > 0) {
                        PacketDistributor.sendToServer(new TerminalLogRequestPayload(
                                this.menu.containerId,
                                cur - 1,
                                this.logFilterBox != null ? this.logFilterBox.getValue() : ""
                        ));
                    }
                }
        ).bounds(prevBtnX, navBtnY, navBtnW, navBtnH).build();

        this.nextLogPageButton = Button.builder(
                Component.literal(">"),
                btn -> {
                    int cur = this.menu.getClientLogPage();
                    int total = this.menu.getClientTotalLogPages();
                    if (cur + 1 < total) {
                        PacketDistributor.sendToServer(new TerminalLogRequestPayload(
                                this.menu.containerId,
                                cur + 1,
                                this.logFilterBox != null ? this.logFilterBox.getValue() : ""
                        ));
                    }
                }
        ).bounds(nextBtnX, navBtnY, navBtnW, navBtnH).build();

        this.addRenderableWidget(this.logFilterBox);
        this.addRenderableWidget(this.prevLogPageButton);
        this.addRenderableWidget(this.nextLogPageButton);

        // Controles de la pestaña Ítems
        int sortButtonWidth = 20;
        int sortButtonHeight = 13;
        int sortButtonX = this.leftPos + 148;
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

        // Botones de filtro: Tier [T] y Pureza [P]
        int filterBtnH = 13;
        int filterBtnW = 18;
        int filterBtnY = this.topPos + 3;
        int tierBtnX = this.leftPos + 128;
        int purBtnX = this.leftPos + 108;

        this.tierFilterButton = Button.builder(
                getTierButtonText(),
                btn -> {
                    currentTierFilter = (currentTierFilter + 1) % 3;
                    updateFilterButtons();
                    refreshFilteredItems();
                }
        )
        .bounds(tierBtnX, filterBtnY, filterBtnW, filterBtnH)
        .tooltip(getTierButtonTooltip())
        .build();

        this.purityFilterButton = Button.builder(
                getPurityButtonText(),
                btn -> {
                    currentMinPurity = (currentMinPurity + 1) % 6;  // 0..5 -> 0
                    updateFilterButtons();
                    refreshFilteredItems();
                }
        )
        .bounds(purBtnX, filterBtnY, filterBtnW, filterBtnH)
        .tooltip(getPurityButtonTooltip())
        .build();

        int searchX = this.leftPos + 52;
        int searchY = this.topPos + 3;
        int searchWidth = purBtnX - searchX - 3;
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
        this.addRenderableWidget(this.purityFilterButton);
        this.addRenderableWidget(this.tierFilterButton);
        this.addRenderableWidget(this.sortButton);

        // 8 botones de vistas numerados [1]..[8] encima de las pestañas
        int viewBtnW = 18;
        int viewBtnH = 13;
        int viewRowY = this.topPos - 32;
        int viewRowX = this.leftPos + 16;
        for (int i = 0; i < ViewManager.MAX_VIEWS; i++) {
            final int slot = i;
            this.viewButtons[i] = Button.builder(
                    Component.literal(String.valueOf(i + 1)),
                    btn -> onViewButtonClicked(slot)
            )
            .bounds(viewRowX + i * viewBtnW, viewRowY, viewBtnW, viewBtnH)
            .build();
            this.addRenderableWidget(this.viewButtons[i]);
        }
        updateViewButtons();

        updateWidgetVisibility();
        refreshFilteredItems();
    }

    private void selectTab(Tab tab) {
        if (this.currentTab == tab) {
            return;
        }
        this.currentTab = tab;
        int activeTabId = switch (tab) {
            case ITEMS -> TerminalTabPayload.TAB_ITEMS;
            case STATS -> TerminalTabPayload.TAB_STATS;
            case ENERGY -> TerminalTabPayload.TAB_ENERGY;
            case TRASH -> TerminalTabPayload.TAB_TRASH;
            case LOG -> TerminalTabPayload.TAB_LOG;
        };
        PacketDistributor.sendToServer(new TerminalTabPayload(this.menu.containerId, activeTabId));
        if (tab == Tab.LOG) {
            PacketDistributor.sendToServer(new TerminalLogRequestPayload(
                    this.menu.containerId,
                    0,
                    this.logFilterBox != null ? this.logFilterBox.getValue() : ""
            ));
        }
        updateWidgetVisibility();
    }

    private void updateWidgetVisibility() {
        boolean isItems = (this.currentTab == Tab.ITEMS);
        boolean isStats = (this.currentTab == Tab.STATS);
        boolean isEnergy = (this.currentTab == Tab.ENERGY);
        boolean isTrash = (this.currentTab == Tab.TRASH);
        boolean isLog = (this.currentTab == Tab.LOG);
        boolean active = this.menu.isNetworkValid() && this.menu.isPowered();

        if (this.itemsTabButton != null) {
            this.itemsTabButton.active = !isItems;
        }
        if (this.statsTabButton != null) {
            this.statsTabButton.active = !isStats;
        }
        if (this.energyTabButton != null) {
            this.energyTabButton.active = !isEnergy;
        }
        if (this.trashTabButton != null) {
            this.trashTabButton.active = !isTrash;
        }
        if (this.logTabButton != null) {
            this.logTabButton.active = !isLog;
            this.logTabButton.visible = this.menu.canClientViewLog();
        }
        if (this.destroyButton != null) {
            this.destroyButton.visible = (isItems || isTrash) && active;
            this.destroyButton.active = !this.menu.getCarried().isEmpty();
        }
        if (this.logFilterBox != null) {
            this.logFilterBox.setVisible(isLog && active);
        }
        if (this.prevLogPageButton != null) {
            this.prevLogPageButton.visible = isLog && active;
            this.prevLogPageButton.active = isLog && active && (this.menu.getClientLogPage() > 0);
        }
        if (this.nextLogPageButton != null) {
            this.nextLogPageButton.visible = isLog && active;
            this.nextLogPageButton.active = isLog && active && (this.menu.getClientLogPage() + 1 < this.menu.getClientTotalLogPages());
        }

        if (this.searchBox != null) {
            this.searchBox.visible = isItems && active;
        }
        if (this.sortButton != null) {
            this.sortButton.visible = isItems && active;
        }
        boolean showItemControls = isItems && active;
        if (this.tierFilterButton != null) {
            this.tierFilterButton.visible = showItemControls;
        }
        if (this.purityFilterButton != null) {
            this.purityFilterButton.visible = showItemControls;
        }
        int maxViews = getMaxSavedViews();
        for (int i = 0; i < this.viewButtons.length; i++) {
            Button vb = this.viewButtons[i];
            if (vb != null) {
                vb.visible = showItemControls && (i < maxViews);
                vb.active = showItemControls && (i < maxViews);
            }
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

    private void onEnergyUpdated() {
        // Callback al recibir nueva telemetría de energía del servidor
    }

    private void onTrashUpdated() {
        // Callback al recibir nueva sincronización de la papelera
    }

    private void onLogUpdated() {
        updateWidgetVisibility();
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

        // Filtro de nivel de almacenamiento (solo cliente)
        if (currentTierFilter == SavedView.TIER_HOT) {
            this.filteredItems.removeIf(e -> e.hotAmount() <= 0);
        } else if (currentTierFilter == SavedView.TIER_COLD) {
            this.filteredItems.removeIf(e -> e.coldAmount() <= 0);
        }

        // Filtro de pureza mínima (solo ítems con pureza explícita >= minPurity)
        if (currentMinPurity > 0) {
            this.filteredItems.removeIf(e -> !e.stack().has(ModDataComponents.PURITY.get())
                    || ModDataComponents.getPurity(e.stack()) < currentMinPurity);
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
        if (this.currentTab != Tab.ITEMS) {
            PacketDistributor.sendToServer(new TerminalTabPayload(this.menu.containerId, TerminalTabPayload.TAB_ITEMS));
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

        if (this.currentTab == Tab.ENERGY) {
            TerminalEnergyPayload energy = this.menu.getClientEnergy();
            if (energy != null) {
                int maxMachScroll = Math.max(0, energy.machines().size() - 3);
                if (maxMachScroll > 0) {
                    if (scrollY > 0) {
                        this.energyMachineScroll = Math.max(0, this.energyMachineScroll - 1);
                        return true;
                    } else if (scrollY < 0) {
                        this.energyMachineScroll = Math.min(maxMachScroll, this.energyMachineScroll + 1);
                        return true;
                    }
                }
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        if (this.currentTab == Tab.TRASH) {
            int totalRows = (int) Math.ceil(this.menu.getClientTrashEntries().size() / 9.0);
            int maxScroll = Math.max(0, totalRows - 6);
            if (maxScroll > 0) {
                if (scrollY > 0) {
                    this.trashScrollRow = Math.max(0, this.trashScrollRow - 1);
                    return true;
                } else if (scrollY < 0) {
                    this.trashScrollRow = Math.min(maxScroll, this.trashScrollRow + 1);
                    return true;
                }
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        if (this.currentTab == Tab.LOG) {
            int cur = this.menu.getClientLogPage();
            int total = this.menu.getClientTotalLogPages();
            if (scrollY > 0 && cur > 0) {
                PacketDistributor.sendToServer(new TerminalLogRequestPayload(
                        this.menu.containerId,
                        cur - 1,
                        this.logFilterBox != null ? this.logFilterBox.getValue() : ""
                ));
                return true;
            } else if (scrollY < 0 && cur + 1 < total) {
                PacketDistributor.sendToServer(new TerminalLogRequestPayload(
                        this.menu.containerId,
                        cur + 1,
                        this.logFilterBox != null ? this.logFilterBox.getValue() : ""
                ));
                return true;
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
        Component currentTitle = switch (this.currentTab) {
            case STATS -> Component.translatable("gui.logtech.terminal.tab.stats");
            case ENERGY -> Component.translatable("gui.logtech.terminal.tab.energy");
            case TRASH -> Component.translatable("gui.logtech.terminal.tab.trash");
            case LOG -> Component.translatable("gui.logtech.terminal.tab.log");
            case ITEMS -> this.title;
        };
        guiGraphics.drawString(this.font, currentTitle, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(CONTAINER_BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.currentTab == Tab.STATS || this.currentTab == Tab.ENERGY || this.currentTab == Tab.LOG) {
            // Fondo oscuro para pestañas de estadísticas, energía y registro sobre el área de ranuras
            guiGraphics.fill(this.leftPos + 7, this.topPos + 17, this.leftPos + 169, this.topPos + 129, 0xFF141418);
            guiGraphics.renderOutline(this.leftPos + 7, this.topPos + 17, 162, 112, 0xFF2A2A36);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.menu.hasActiveColdRetrieval()) {
            this.menu.clientTick();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateWidgetVisibility();
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.currentTab == Tab.ITEMS) {
            renderItemsTab(guiGraphics, mouseX, mouseY);
        } else if (this.currentTab == Tab.STATS) {
            renderStatsTab(guiGraphics, mouseX, mouseY);
        } else if (this.currentTab == Tab.ENERGY) {
            renderEnergyTab(guiGraphics, mouseX, mouseY);
        } else if (this.currentTab == Tab.TRASH) {
            renderTrashTab(guiGraphics, mouseX, mouseY);
        } else if (this.currentTab == Tab.LOG) {
            renderLogTab(guiGraphics, mouseX, mouseY);
        }

        if (this.menu.hasActiveColdRetrieval()) {
            renderColdRetrievalBar(guiGraphics, mouseX, mouseY);
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
            tooltip.add(
                    Component.translatable(
                            "gui.logtech.terminal.stored_hot",
                            String.format(Locale.ROOT, "%,d", hoveredEntry.hotAmount())
                    ).withStyle(ChatFormatting.GOLD)
            );
            tooltip.add(
                    Component.translatable(
                            "gui.logtech.terminal.stored_cold",
                            String.format(Locale.ROOT, "%,d", hoveredEntry.coldAmount())
                    ).withStyle(ChatFormatting.AQUA)
            );
            Component policyComp = switch (hoveredEntry.pinMode()) {
                case FIJAR_CALIENTE -> Component.translatable("gui.logtech.terminal.policy.fijar_caliente").withStyle(ChatFormatting.GOLD);
                case SOLO_FRIO -> Component.translatable("gui.logtech.terminal.policy.solo_frio").withStyle(ChatFormatting.AQUA);
                case AUTO -> Component.translatable("gui.logtech.terminal.policy.auto").withStyle(ChatFormatting.GREEN);
            };
            tooltip.add(
                    Component.translatable("gui.logtech.terminal.stored_policy", policyComp)
                            .withStyle(ChatFormatting.GRAY)
            );

            // Tendencia por minuto («+120/min», «−45/min» o «0/min») y aviso de agotamiento si baja
            long itemRate = hoveredEntry.ratePerMinute();
            String rateStr = itemRate > 0 ? ("+" + itemRate + "/min") : (itemRate < 0 ? ("\u2212" + Math.abs(itemRate) + "/min") : "0/min");
            ChatFormatting rateColor = itemRate > 0 ? ChatFormatting.GREEN : (itemRate < 0 ? ChatFormatting.RED : ChatFormatting.GRAY);
            tooltip.add(
                    Component.translatable(
                            "gui.logtech.terminal.stored_trend",
                            Component.literal(rateStr).withStyle(rateColor)
                    ).withStyle(ChatFormatting.GRAY)
            );
            if (itemRate < 0) {
                long depletionMins = RateTracker.calculateDepletionTimeMinutes(hoveredEntry.amount(), itemRate);
                String depStr = RateTracker.formatDuration(depletionMins);
                tooltip.add(
                        Component.translatable("gui.logtech.terminal.item_depleted_in", depStr)
                                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC)
                );
            }

            tooltip.add(
                    Component.translatable("gui.logtech.terminal.policy_hint")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
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
        drawFittedString(guiGraphics, storageComp, left, this.topPos + 18, 154, 0xEEEEEE);

        int barX = left;
        int barY = this.topPos + 27;
        int barW = 154;
        int barH = 4;
        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF222228);
        guiGraphics.renderOutline(barX - 1, barY - 1, barW + 2, barH + 2, 0xFF3E3E4C);
        int fillW = total > 0 ? (int) Math.min(barW, Math.round((double) used / total * barW)) : 0;
        int fillColor = used > total ? 0xFFE53935 : (pct >= 90.0 ? 0xFFFFA726 : 0xFF00ADB5);
        if (fillW > 0) {
            guiGraphics.fill(barX, barY, barX + fillW, barY + barH, fillColor);
        }

        // 2. Estimaciones de llenado (calculado con el ritmo neto de los últimos 5 minutos)
        long totalRem = Math.max(0L, total - used);
        String totalEstStr = (stats.totalNetRate() <= 0L || totalRem <= 0L)
                ? Component.translatable("gui.logtech.terminal.stats.stable").getString()
                : Component.translatable("gui.logtech.terminal.stats.fill_time",
                        RateTracker.formatDuration(RateTracker.calculateFillTimeMinutes(totalRem, stats.totalNetRate()))).getString();
        Component totalEstComp = Component.translatable("gui.logtech.terminal.stats.fill_total", totalEstStr);
        drawFittedString(guiGraphics, totalEstComp, left, this.topPos + 34, 154, stats.totalNetRate() > 0 ? 0x00E5FF : 0x81C784);

        long hotRem = Math.max(0L, stats.hotTotalCapacity() - stats.hotUsedCapacity());
        long coldRem = Math.max(0L, stats.coldTotalCapacity() - stats.coldUsedCapacity());
        String hotEstStr = (stats.hotNetRate() <= 0L || hotRem <= 0L)
                ? Component.translatable("gui.logtech.terminal.stats.stable").getString()
                : Component.translatable("gui.logtech.terminal.stats.fill_time",
                        RateTracker.formatDuration(RateTracker.calculateFillTimeMinutes(hotRem, stats.hotNetRate()))).getString();
        String coldEstStr = (stats.coldNetRate() <= 0L || coldRem <= 0L)
                ? Component.translatable("gui.logtech.terminal.stats.stable").getString()
                : Component.translatable("gui.logtech.terminal.stats.fill_time",
                        RateTracker.formatDuration(RateTracker.calculateFillTimeMinutes(coldRem, stats.coldNetRate()))).getString();
        Component levelsEstComp = Component.translatable("gui.logtech.terminal.stats.fill_levels", hotEstStr, coldEstStr);
        drawFittedString(guiGraphics, levelsEstComp, left, this.topPos + 43, 154, 0xAAAAAA);

        // 3. Items insertados por minuto (último minuto y media de 5 min)
        Component rateComp = Component.translatable(
                "gui.logtech.terminal.stats.items_per_minute",
                stats.itemsPerMinuteLast(),
                String.format(Locale.ROOT, "%.1f", stats.itemsPerMinute5MinAvg())
        );
        drawFittedString(guiGraphics, rateComp, left, this.topPos + 52, 154, 0x00E5FF);

        // 4. Energía del Núcleo y Cuarentena
        String energyStr = Component.translatable(
                "gui.logtech.terminal.stats.energy",
                String.format(Locale.ROOT, "%,d", stats.energyStored()),
                String.format(Locale.ROOT, "%,d", stats.energyCapacity()),
                stats.energyConsumption()
        ).getString();
        if (stats.quarantineCount() > 0) {
            energyStr += " (Cuar: " + String.format(Locale.ROOT, "%,d", stats.quarantineCount()) + ")";
        }
        drawFittedString(guiGraphics, Component.literal(energyStr), left, this.topPos + 61, 154, 0xFFD54F);

        // 5. Gráfica de barras de los últimos 60 minutos de items insertados
        guiGraphics.drawString(
                this.font,
                Component.translatable("gui.logtech.terminal.stats.chart_title"),
                left,
                this.topPos + 71,
                0xAAAAAA,
                false
        );

        int chartX = left;
        int chartY = this.topPos + 80;
        int chartW = 120;
        int chartH = 14;
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
        guiGraphics.drawString(this.font, "0", left + 124, chartY + 6, 0x555555, false);

        // Tooltip interactivo sobre la barra de almacenamiento y estimaciones
        if (mouseX >= barX && mouseX < barX + barW && mouseY >= this.topPos + 18 && mouseY < this.topPos + 51) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.logtech.terminal.stats.fill_tooltip_title").withStyle(ChatFormatting.GOLD));
            String totalRateStr = stats.totalNetRate() > 0 ? ("+" + stats.totalNetRate()) : String.valueOf(stats.totalNetRate());
            String hotRateStr = stats.hotNetRate() > 0 ? ("+" + stats.hotNetRate()) : String.valueOf(stats.hotNetRate());
            String coldRateStr = stats.coldNetRate() > 0 ? ("+" + stats.coldNetRate()) : String.valueOf(stats.coldNetRate());
            tip.add(Component.translatable("gui.logtech.terminal.stats.fill_tooltip_total", totalEstStr, totalRateStr).withStyle(ChatFormatting.WHITE));
            tip.add(Component.translatable("gui.logtech.terminal.stats.fill_tooltip_hot", hotEstStr, hotRateStr).withStyle(ChatFormatting.GOLD));
            tip.add(Component.translatable("gui.logtech.terminal.stats.fill_tooltip_cold", coldEstStr, coldRateStr).withStyle(ChatFormatting.AQUA));
            guiGraphics.renderComponentTooltip(this.font, tip, mouseX, mouseY);
            return;
        }

        // 6. Lista de máquinas
        List<TerminalStatsPayload.MachineStatEntry> machines = stats.machines();
        int machLabelY = this.topPos + 96;
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
                Component lineComp;
                if (m.firmware() != null && m.firmware() != io.github.chema280101.logtech.machine.firmware.FirmwareType.NONE) {
                    Component fwName = Component.translatable("item.logtech.firmware_" + m.firmware().getId());
                    lineComp = Component.translatable(
                            "gui.logtech.terminal.stats.machine_entry_firmware_heat",
                            m.name(),
                            fwName,
                            m.status().getCauseComponent(),
                            m.itemsPerMinute(),
                            m.heat()
                    );
                } else {
                    lineComp = Component.translatable(
                            "gui.logtech.terminal.stats.machine_entry_heat",
                            m.name(),
                            m.status().getCauseComponent(),
                            m.itemsPerMinute(),
                            m.heat()
                    );
                }
                int textColor = m.status() == MachineStatus.OVERHEATED ? 0xE53935
                        : (m.heat() > 70 ? 0xFF9800 : (m.status().isActive() ? 0x81C784 : 0xE57373));
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

    private void renderEnergyTab(GuiGraphics guiGraphics, int mouseX, int mouseY) {
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

        TerminalEnergyPayload energy = this.menu.getClientEnergy();
        if (energy == null) {
            Component loadingComp = Component.translatable("gui.logtech.terminal.energy.loading");
            guiGraphics.drawString(this.font, loadingComp, this.leftPos + 10, this.topPos + 22, 0x888888, false);
            renderTooltip(guiGraphics, mouseX, mouseY);
            return;
        }

        int left = this.leftPos + 10;

        // 1. FE almacenada y capacidad con barra
        int stored = energy.energyStored();
        int cap = energy.energyCapacity();
        double pct = cap > 0 ? (stored * 100.0 / cap) : 0.0;
        Component energyComp = Component.translatable(
                "gui.logtech.terminal.energy.storage",
                String.format(Locale.ROOT, "%,d", stored),
                String.format(Locale.ROOT, "%,d", cap),
                String.format(Locale.ROOT, "%.1f", pct)
        );
        drawFittedString(guiGraphics, energyComp, left, this.topPos + 19, 154, 0xFFD54F);

        int barX = left;
        int barY = this.topPos + 28;
        int barW = 154;
        int barH = 5;
        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF222228);
        guiGraphics.renderOutline(barX - 1, barY - 1, barW + 2, barH + 2, 0xFF3E3E4C);
        int fillW = cap > 0 ? (int) Math.min(barW, Math.round((double) stored / cap * barW)) : 0;
        int fillColor = pct < 20.0 ? 0xFFE53935 : (pct >= 90.0 ? 0xFF43A047 : 0xFFFFA000);
        if (fillW > 0) {
            guiGraphics.fill(barX, barY, barX + fillW, barY + barH, fillColor);
        }

        // 2. FE recibida y consumida por minuto
        Component ratesComp = Component.translatable(
                "gui.logtech.terminal.energy.rates",
                String.format(Locale.ROOT, "%,d", energy.energyReceivedPerMinute()),
                String.format(Locale.ROOT, "%,d", energy.energyConsumedPerMinute())
        );
        drawFittedString(guiGraphics, ratesComp, left, this.topPos + 36, 154, 0xEEEEEE);

        // 3. Consumo por rama: Almacenamiento (Núcleo y Terminal) y Minerales (Triturador, Lavadora, Centrífuga y Perforadora)
        Component branchesComp = Component.translatable(
                "gui.logtech.terminal.energy.branches",
                String.format(Locale.ROOT, "%,d", energy.storageBranchConsumption()),
                String.format(Locale.ROOT, "%,d", energy.mineralsBranchConsumption())
        );
        drawFittedString(guiGraphics, branchesComp, left, this.topPos + 46, 154, 0x80D8FF);

        // 4. Gráfica de los últimos 60 minutos (FE recibida y consumida)
        guiGraphics.drawString(
                this.font,
                Component.translatable("gui.logtech.terminal.energy.chart_title"),
                left,
                this.topPos + 56,
                0xAAAAAA,
                false
        );

        int chartX = left;
        int chartY = this.topPos + 66;
        int chartW = 120;
        int chartH = 18;
        guiGraphics.fill(chartX, chartY, chartX + chartW, chartY + chartH, 0xFF18181E);
        guiGraphics.renderOutline(chartX - 1, chartY - 1, chartW + 2, chartH + 2, 0xFF353545);

        int[] recHist = energy.receivedHistory();
        int[] conHist = energy.consumedHistory();
        int maxVal = 1;
        for (int v : recHist) if (v > maxVal) maxVal = v;
        for (int v : conHist) if (v > maxVal) maxVal = v;

        for (int i = 0; i < 60; i++) {
            int rVal = (i < recHist.length) ? recHist[i] : 0;
            int cVal = (i < conHist.length) ? conHist[i] : 0;
            int rH = (rVal > 0) ? Math.max(1, (int) Math.round(((double) rVal / maxVal) * (chartH - 2))) : 0;
            int cH = (cVal > 0) ? Math.max(1, (int) Math.round(((double) cVal / maxVal) * (chartH - 2))) : 0;
            int bX = chartX + i * 2;

            // Barra de entrada (recibida) en el primer pixel
            if (rH > 0) {
                int rY = chartY + chartH - 1 - rH;
                guiGraphics.fill(bX, rY, bX + 1, chartY + chartH - 1, (i == 59) ? 0xFF69F0AE : 0xFF2E7D32);
            }
            // Barra de consumo en el segundo pixel
            if (cH > 0) {
                int cY = chartY + chartH - 1 - cH;
                guiGraphics.fill(bX + 1, cY, bX + 2, chartY + chartH - 1, (i == 59) ? 0xFFFFAB40 : 0xFFE65100);
            }
        }

        // Escala del gráfico a la derecha
        String maxStr = maxVal >= 1000 ? (maxVal / 1000) + "k" : maxVal + "";
        guiGraphics.drawString(this.font, maxStr, left + 124, chartY, 0x777777, false);
        guiGraphics.drawString(this.font, "0", left + 124, chartY + 9, 0x555555, false);

        // 5. Consumo por máquina, ordenado de mayor a menor, con su prioridad
        List<TerminalEnergyPayload.MachineEnergyEntry> machines = energy.machines();
        int machLabelY = this.topPos + 87;
        Component machTitle = Component.translatable("gui.logtech.terminal.energy.machines_title", machines.size());
        guiGraphics.drawString(this.font, machTitle, left, machLabelY, 0xAAAAAA, false);

        if (machines.isEmpty()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("gui.logtech.terminal.energy.no_machines"),
                    left,
                    machLabelY + 10,
                    0x666666,
                    false
            );
        } else {
            int maxMachScroll = Math.max(0, machines.size() - 3);
            if (maxMachScroll > 0) {
                String pageStr = (this.energyMachineScroll + 1) + "-" + Math.min(this.energyMachineScroll + 3, machines.size()) + "/" + machines.size();
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
                int mIdx = this.energyMachineScroll + row;
                if (mIdx >= machines.size()) break;
                TerminalEnergyPayload.MachineEnergyEntry m = machines.get(mIdx);
                int lineY = machLabelY + 10 + row * 10;

                // Botón / badge de prioridad a la derecha
                int pBadgeW = 24;
                int pBadgeH = 9;
                int pBadgeX = this.leftPos + this.imageWidth - 10 - pBadgeW;
                int pBadgeY = lineY;

                // Color del badge según prioridad
                int badgeBg = m.isCore() ? 0xFF37474F : switch (m.priority()) {
                    case HIGH -> 0xFF2E7D32;
                    case NORMAL -> 0xFF0277BD;
                    case LOW -> 0xFFC62828;
                };
                int badgeBorder = m.isCore() ? 0xFF546E7A : switch (m.priority()) {
                    case HIGH -> 0xFF66BB6A;
                    case NORMAL -> 0xFF4FC3F7;
                    case LOW -> 0xFFEF5350;
                };
                guiGraphics.fill(pBadgeX, pBadgeY, pBadgeX + pBadgeW, pBadgeY + pBadgeH, badgeBg);
                guiGraphics.renderOutline(pBadgeX, pBadgeY, pBadgeW, pBadgeH, badgeBorder);

                String pLabel = m.isCore() ? "CORE" : switch (m.priority()) {
                    case HIGH -> "ALTA";
                    case NORMAL -> "NORM";
                    case LOW -> "BAJA";
                };
                int pLabelW = this.font.width(pLabel);
                guiGraphics.drawString(this.font, pLabel, pBadgeX + (pBadgeW - pLabelW) / 2, pBadgeY + 1, 0xFFFFFF, false);

                // Nombre y consumo
                Component textComp = Component.literal(m.name().getString() + ": -" + String.format(Locale.ROOT, "%,d", m.energyConsumed()) + " FE/m");
                drawFittedString(guiGraphics, textComp, left, lineY + 1, pBadgeX - left - 4, 0xCCCCCC);
            }
        }

        // Tooltip en la gráfica si el ratón está encima
        if (mouseX >= chartX && mouseX < chartX + chartW && mouseY >= chartY && mouseY < chartY + chartH) {
            int idx = (mouseX - chartX) / 2;
            if (idx >= 0 && idx < 60) {
                int rV = (idx < recHist.length) ? recHist[idx] : 0;
                int cV = (idx < conHist.length) ? conHist[idx] : 0;
                int minsAgo = 59 - idx;
                String timeLabel = (minsAgo == 0)
                        ? Component.translatable("gui.logtech.terminal.energy.current_minute").getString()
                        : Component.translatable("gui.logtech.terminal.energy.minutes_ago", minsAgo).getString();
                int net = rV - cV;
                String netStr = (net >= 0 ? "+" : "") + String.format(Locale.ROOT, "%,d", net) + " FE";
                Component tip = Component.translatable(
                        "gui.logtech.terminal.energy.chart_tooltip",
                        timeLabel,
                        String.format(Locale.ROOT, "%,d", rV),
                        String.format(Locale.ROOT, "%,d", cV),
                        netStr
                );
                guiGraphics.renderTooltip(this.font, tip, mouseX, mouseY);
                return;
            }
        }

        // Tooltip en el badge de prioridad si el ratón está encima
        for (int row = 0; row < 3; row++) {
            int mIdx = this.energyMachineScroll + row;
            if (mIdx >= machines.size()) break;
            TerminalEnergyPayload.MachineEnergyEntry m = machines.get(mIdx);
            int lineY = machLabelY + 10 + row * 10;
            int pBadgeW = 24;
            int pBadgeH = 9;
            int pBadgeX = this.leftPos + this.imageWidth - 10 - pBadgeW;
            if (mouseX >= pBadgeX && mouseX < pBadgeX + pBadgeW && mouseY >= lineY && mouseY < lineY + pBadgeH) {
                Component tip = m.isCore()
                        ? Component.translatable("gui.logtech.terminal.energy.priority_fixed_tooltip")
                        : Component.translatable("gui.logtech.terminal.energy.priority_tooltip");
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

    private void renderTrashTab(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean valid = this.menu.isNetworkValid();
        boolean powered = this.menu.isPowered();

        io.github.chema280101.logtech.network.TerminalTrashPayload.TrashItemEntry hoveredEntry = null;
        int hoveredSlotX = 0;
        int hoveredSlotY = 0;

        if (!valid) {
            Component invalidComp = Component.translatable("gui.logtech.terminal.network_invalid");
            guiGraphics.drawString(this.font, invalidComp, this.leftPos + 8, this.topPos + 20, 0xD32F2F, false);
            renderTooltip(guiGraphics, mouseX, mouseY);
            return;
        }
        if (!powered) {
            Component noEnergyComp = Component.translatable("gui.logtech.terminal.no_energy");
            guiGraphics.drawString(this.font, noEnergyComp, this.leftPos + 8, this.topPos + 20, 0xD32F2F, false);
            renderTooltip(guiGraphics, mouseX, mouseY);
            return;
        }

        List<io.github.chema280101.logtech.network.TerminalTrashPayload.TrashItemEntry> entries = this.menu.getClientTrashEntries();
        if (entries.isEmpty()) {
            Component emptyComp = Component.translatable("gui.logtech.terminal.trash.empty");
            guiGraphics.drawString(this.font, emptyComp, this.leftPos + 10, this.topPos + 22, 0x888888, false);
            renderTooltip(guiGraphics, mouseX, mouseY);
            return;
        }

        int totalRows = (int) Math.ceil(entries.size() / 9.0);
        int maxScroll = Math.max(0, totalRows - 6);
        if (maxScroll > 0) {
            String pageInfo = (this.trashScrollRow + 1) + "/" + (maxScroll + 1);
            guiGraphics.drawString(
                    this.font,
                    pageInfo,
                    this.leftPos + this.imageWidth - 8 - this.font.width(pageInfo),
                    this.topPos + 129,
                    0x555555,
                    false
            );
        }

        int startIndex = this.trashScrollRow * 9;
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int index = startIndex + row * 9 + col;
                if (index >= entries.size()) {
                    break;
                }
                io.github.chema280101.logtech.network.TerminalTrashPayload.TrashItemEntry entry = entries.get(index);
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

        if (hoveredEntry != null && this.menu.getCarried().isEmpty()) {
            List<Component> tooltip = new ArrayList<>(getTooltipFromItem(this.minecraft, hoveredEntry.stack()));
            tooltip.add(
                    Component.translatable(
                            "gui.logtech.terminal.trash.amount",
                            String.format(Locale.ROOT, "%,d", hoveredEntry.amount())
                    ).withStyle(ChatFormatting.GOLD)
            );
            long secs = hoveredEntry.ticksRemaining() / 20L;
            long mins = secs / 60L;
            long remSecs = secs % 60L;
            String timeStr = mins > 0 ? (mins + "m " + remSecs + "s") : (remSecs + "s");
            tooltip.add(
                    Component.translatable("gui.logtech.terminal.trash.expires_in", timeStr)
                            .withStyle(ChatFormatting.RED)
            );
            tooltip.add(
                    Component.translatable("gui.logtech.terminal.trash.restore_hint")
                            .withStyle(ChatFormatting.GREEN)
            );
            guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        } else {
            renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderLogTab(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean valid = this.menu.isNetworkValid();
        boolean powered = this.menu.isPowered();

        if (!valid) {
            Component invalidComp = Component.translatable("gui.logtech.terminal.network_invalid");
            guiGraphics.drawString(this.font, invalidComp, this.leftPos + 8, this.topPos + 20, 0xD32F2F, false);
            renderTooltip(guiGraphics, mouseX, mouseY);
            return;
        }
        if (!powered) {
            Component noEnergyComp = Component.translatable("gui.logtech.terminal.no_energy");
            guiGraphics.drawString(this.font, noEnergyComp, this.leftPos + 8, this.topPos + 20, 0xD32F2F, false);
            renderTooltip(guiGraphics, mouseX, mouseY);
            return;
        }

        int curPage = this.menu.getClientLogPage();
        int totalPages = this.menu.getClientTotalLogPages();
        String pageInfo = (curPage + 1) + "/" + Math.max(1, totalPages);
        int pageTextW = this.font.width(pageInfo);
        guiGraphics.drawString(this.font, pageInfo, this.leftPos + 119 + (34 - pageTextW) / 2, this.topPos + 6, 0x888888, false);

        List<TerminalLogPagePayload.LogEntryDto> entries = this.menu.getClientLogEntries();
        if (entries.isEmpty()) {
            Component emptyComp = Component.translatable("gui.logtech.terminal.log.empty");
            guiGraphics.drawString(this.font, emptyComp, this.leftPos + 10, this.topPos + 22, 0x888888, false);
            renderTooltip(guiGraphics, mouseX, mouseY);
            return;
        }

        long clientGameTime = (this.minecraft != null && this.minecraft.level != null) ? this.minecraft.level.getGameTime() : 0L;
        TerminalLogPagePayload.LogEntryDto hoveredEntry = null;

        int rowH = 11;
        for (int i = 0; i < entries.size() && i < 10; i++) {
            TerminalLogPagePayload.LogEntryDto entry = entries.get(i);
            int rowY = this.topPos + 19 + i * rowH;

            boolean isHovered = mouseX >= this.leftPos + 8 && mouseX < this.leftPos + 168
                    && mouseY >= rowY - 1 && mouseY < rowY + 10;
            if (isHovered) {
                guiGraphics.fill(this.leftPos + 8, rowY - 1, this.leftPos + 168, rowY + 10, 0x22FFFFFF);
                hoveredEntry = entry;
            }

            // Tiempo relativo
            long diffTicks = Math.max(0, clientGameTime - entry.gameTime());
            long diffSecs = diffTicks / 20L;
            String timeStr = diffSecs < 60 ? (diffSecs + "s") : (diffSecs < 3600 ? (diffSecs / 60 + "m") : (diffSecs / 3600 + "h"));
            guiGraphics.drawString(this.font, timeStr, this.leftPos + 9, rowY, 0x777777, false);

            // Símbolo de acción y color
            String actionSymbol;
            int actionColor;
            switch (entry.action()) {
                case INSERT -> { actionSymbol = "+"; actionColor = 0x4CAF50; }
                case EXTRACT -> { actionSymbol = "-"; actionColor = 0xFFA726; }
                case DESTROY -> { actionSymbol = "x"; actionColor = 0xEF5350; }
                case RESTORE -> { actionSymbol = "↺"; actionColor = 0x26C6DA; }
                case CHANGE_RULES -> { actionSymbol = "⚙"; actionColor = 0xFFEB3B; }
                case CHANGE_ACCESS -> { actionSymbol = "🛡"; actionColor = 0xAB47BC; }
                default -> { actionSymbol = "•"; actionColor = 0xAAAAAA; }
            }
            guiGraphics.drawString(this.font, actionSymbol, this.leftPos + 32, rowY, actionColor, false);

            // Nombre de jugador (hasta 7 caracteres si es largo)
            String pName = entry.playerName();
            if (pName.length() > 7) {
                pName = pName.substring(0, 6) + "…";
            }
            guiGraphics.drawString(this.font, pName, this.leftPos + 41, rowY, 0x80D8FF, false);

            // Detalle: cantidad + item o nombre de acción si es regla/acceso
            if (entry.action() == ActivityAction.CHANGE_RULES) {
                guiGraphics.drawString(this.font, Component.translatable("gui.logtech.activity.action.change_rules"), this.leftPos + 82, rowY, 0xCCCCCC, false);
            } else if (entry.action() == ActivityAction.CHANGE_ACCESS) {
                guiGraphics.drawString(this.font, Component.translatable("gui.logtech.activity.action.change_access"), this.leftPos + 82, rowY, 0xCCCCCC, false);
            } else {
                String amtStr = entry.amount() > 9999 ? (entry.amount() / 1000 + "k") : String.valueOf(entry.amount());
                String prefix = amtStr + "x ";
                guiGraphics.drawString(this.font, prefix, this.leftPos + 82, rowY, 0xFFD54F, false);
                int amtWidth = this.font.width(prefix);

                String itemName = resolveItemDisplayName(entry.itemId());
                int maxNameW = (this.leftPos + 167) - (this.leftPos + 82 + amtWidth);
                if (maxNameW > 10) {
                    String trimmedName = this.font.plainSubstrByWidth(itemName, maxNameW);
                    guiGraphics.drawString(this.font, trimmedName, this.leftPos + 82 + amtWidth, rowY, 0xE0E0E0, false);
                }
            }
        }

        if (hoveredEntry != null && this.menu.getCarried().isEmpty()) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(hoveredEntry.playerName()).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("gui.logtech.terminal.log.action_desc", Component.translatable(getActionTranslationKey(hoveredEntry.action()))).withStyle(ChatFormatting.GRAY));
            if (hoveredEntry.action() != ActivityAction.CHANGE_RULES && hoveredEntry.action() != ActivityAction.CHANGE_ACCESS) {
                String fullItemName = resolveItemDisplayName(hoveredEntry.itemId());
                tooltip.add(Component.translatable("gui.logtech.terminal.log.item_desc", fullItemName, String.format(Locale.ROOT, "%,d", hoveredEntry.amount())).withStyle(ChatFormatting.GOLD));
            }
            long diffTicks = Math.max(0, clientGameTime - hoveredEntry.gameTime());
            long diffSecs = diffTicks / 20L;
            tooltip.add(Component.translatable("gui.logtech.terminal.log.time_desc", diffSecs, hoveredEntry.gameTime()).withStyle(ChatFormatting.DARK_GRAY));
            guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        } else {
            renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    private String resolveItemDisplayName(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return "-";
        }
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id != null) {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) {
                return item.getDescription().getString();
            }
        }
        return itemId;
    }

    private static String getActionTranslationKey(ActivityAction action) {
        return switch (action) {
            case INSERT -> "gui.logtech.activity.action.insert";
            case EXTRACT -> "gui.logtech.activity.action.extract";
            case DESTROY -> "gui.logtech.activity.action.destroy";
            case RESTORE -> "gui.logtech.activity.action.restore";
            case CHANGE_RULES -> "gui.logtech.activity.action.change_rules";
            case CHANGE_ACCESS -> "gui.logtech.activity.action.change_access";
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.itemsTabButton != null && this.itemsTabButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.statsTabButton != null && this.statsTabButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.energyTabButton != null && this.energyTabButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.trashTabButton != null && this.trashTabButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.logTabButton != null && this.logTabButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.destroyButton != null && this.destroyButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (this.currentTab == Tab.LOG) {
            if (this.logFilterBox != null && this.logFilterBox.visible && this.logFilterBox.isMouseOver(mouseX, mouseY) && button == 1) {
                this.logFilterBox.setValue("");
                return true;
            }
            if (this.logFilterBox != null && this.logFilterBox.visible && this.logFilterBox.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(this.logFilterBox);
                return true;
            }
            if (this.prevLogPageButton != null && this.prevLogPageButton.visible && this.prevLogPageButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (this.nextLogPageButton != null && this.nextLogPageButton.visible && this.nextLogPageButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (this.logFilterBox != null && this.logFilterBox.isFocused()) {
                this.logFilterBox.setFocused(false);
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (this.currentTab == Tab.STATS) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (this.currentTab == Tab.ENERGY) {
            TerminalEnergyPayload energy = this.menu.getClientEnergy();
            if (energy != null && !energy.machines().isEmpty()) {
                int machLabelY = this.topPos + 87;
                int pBadgeW = 24;
                int pBadgeH = 9;
                int pBadgeX = this.leftPos + this.imageWidth - 10 - pBadgeW;
                for (int row = 0; row < 3; row++) {
                    int mIdx = this.energyMachineScroll + row;
                    if (mIdx >= energy.machines().size()) break;
                    TerminalEnergyPayload.MachineEnergyEntry m = energy.machines().get(mIdx);
                    int lineY = machLabelY + 10 + row * 10;
                    if (!m.isCore() && mouseX >= pBadgeX && mouseX < pBadgeX + pBadgeW && mouseY >= lineY && mouseY < lineY + pBadgeH) {
                        EnergyPriority next = m.priority().next();
                        PacketDistributor.sendToServer(new TerminalPriorityPayload(this.menu.containerId, m.pos(), next.ordinal()));
                        return true;
                    }
                }
            }
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

        if (this.currentTab == Tab.TRASH) {
            if (mouseX >= gridLeft && mouseX < gridRight && mouseY >= gridTop && mouseY < gridBottom) {
                int col = (int) (mouseX - gridLeft) / 18;
                int row = (int) (mouseY - gridTop) / 18;
                int index = (this.trashScrollRow * 9) + (row * 9 + col);
                List<io.github.chema280101.logtech.network.TerminalTrashPayload.TrashItemEntry> trashEntries = this.menu.getClientTrashEntries();
                if (index >= 0 && index < trashEntries.size()) {
                    io.github.chema280101.logtech.network.TerminalTrashPayload.TrashItemEntry entry = trashEntries.get(index);
                    PacketDistributor.sendToServer(new TerminalActionPayload(
                            this.menu.containerId,
                            TerminalActionPayload.ActionType.RESTORE_TRASH,
                            hasShiftDown() ? 1 : 0,
                            entry.stack()
                    ));
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

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
                    if (hasControlDown() && button == 1) {
                        PacketDistributor.sendToServer(new TerminalActionPayload(
                                this.menu.containerId,
                                TerminalActionPayload.ActionType.CYCLE_PIN_POLICY,
                                0,
                                entry.stack()
                        ));
                        return true;
                    } else if (hasShiftDown()) {
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
        if (this.currentTab == Tab.LOG && this.logFilterBox != null && this.logFilterBox.canConsumeInput()) {
            if (keyCode == 256) { // Escape
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.closeContainer();
                }
                return true;
            }
            if (this.logFilterBox.keyPressed(keyCode, scanCode, modifiers)) {
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
        if (this.currentTab == Tab.LOG && this.logFilterBox != null && this.logFilterBox.canConsumeInput()) {
            if (this.logFilterBox.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if ((this.currentTab == Tab.STATS || this.currentTab == Tab.ENERGY || this.currentTab == Tab.LOG) && type == ClickType.QUICK_MOVE) {
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

    private void renderColdRetrievalBar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack item = this.menu.getClientColdItem();
        if (item.isEmpty()) {
            return;
        }
        int amount = this.menu.getClientColdAmount();
        int remTicks = this.menu.getClientColdRemainingTicks();
        int totTicks = Math.max(1, this.menu.getClientColdTotalTicks());
        int queueSize = this.menu.getClientColdQueueSize();

        int barW = 162;
        int barH = 20;
        int barX = this.leftPos + 7;
        int barY = (this.topPos + this.imageHeight + barH + 2 <= this.height)
                ? this.topPos + this.imageHeight + 2
                : this.topPos - barH - 2;

        // Fondo oscuro y contorno cyan/hielo
        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xEE101622);
        guiGraphics.renderOutline(barX, barY, barW, barH, 0xFF0288D1);

        // Icono del ítem
        guiGraphics.renderItem(item, barX + 2, barY + 2);

        // Cantidad
        String amountStr = "x" + amount;
        if (queueSize > 1) {
            amountStr += " (+" + (queueSize - 1) + ")";
        }
        guiGraphics.drawString(this.font, amountStr, barX + 21, barY + 6, 0xFFFFFF, false);

        // Barra de progreso y cuenta atrás
        float seconds = Math.max(0.0f, remTicks / 20.0f);
        String timeStr = String.format(Locale.ROOT, "%.1fs", seconds);
        int timeWidth = this.font.width(timeStr);
        int countdownX = barX + barW - timeWidth - 4;

        int textWidth = this.font.width(amountStr);
        int progX = barX + 23 + textWidth + 4;
        int progW = countdownX - progX - 4;

        if (progW > 15) {
            int progY = barY + 6;
            int progH = 8;
            guiGraphics.fill(progX, progY, progX + progW, progY + progH, 0xFF0A0F18);
            guiGraphics.renderOutline(progX, progY, progW, progH, 0xFF1E2D3D);

            float progress = Math.min(1.0f, Math.max(0.0f, (float) (totTicks - remTicks) / totTicks));
            int fillW = (int) Math.round(progress * (progW - 2));
            if (fillW > 0) {
                guiGraphics.fillGradient(progX + 1, progY + 1, progX + 1 + fillW, progY + progH - 1, 0xFF00B0FF, 0xFF00E5FF);
            }
        }

        guiGraphics.drawString(this.font, timeStr, countdownX, barY + 6, 0x80D8FF, false);

        // Tooltip explicativo
        if (mouseX >= barX && mouseX < barX + barW && mouseY >= barY && mouseY < barY + barH) {
            Component name = item.getHoverName();
            Component tooltip = Component.translatable(
                    "gui.logtech.terminal.cold_retrieval_tooltip",
                    name,
                    amount,
                    String.format(Locale.ROOT, "%.1f", seconds)
            );
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private int getMaxSavedViews() {
        try {
            return io.github.chema280101.logtech.LogTechConfig.MAX_SAVED_VIEWS.get();
        } catch (Exception e) {
            return ViewManager.MAX_VIEWS;
        }
    }

    private void onViewsUpdated() {
        updateViewButtons();
    }

    private void updateViewButtons() {
        int maxViews = getMaxSavedViews();
        List<SavedView> views = this.menu.getClientViews();
        for (int i = 0; i < ViewManager.MAX_VIEWS; i++) {
            if (this.viewButtons[i] == null) {
                continue;
            }
            if (i >= maxViews) {
                this.viewButtons[i].visible = false;
                this.viewButtons[i].active = false;
                continue;
            }
            SavedView v = (i < views.size()) ? views.get(i) : null;
            boolean occupied = v != null;
            String label = String.valueOf(i + 1);
            if (occupied) {
                this.viewButtons[i].setMessage(Component.literal(label).withStyle(ChatFormatting.YELLOW));
                this.viewButtons[i].setTooltip(Tooltip.create(
                        Component.literal(v.name()).withStyle(ChatFormatting.GOLD)
                                .append("\n")
                                .append(Component.translatable("gui.logtech.terminal.view.save_hint")
                                        .withStyle(ChatFormatting.GRAY))
                ));
            } else {
                this.viewButtons[i].setMessage(Component.literal(label).withStyle(ChatFormatting.DARK_GRAY));
                this.viewButtons[i].setTooltip(Tooltip.create(
                        Component.translatable("gui.logtech.terminal.view.empty")
                                .append("\n")
                                .append(Component.translatable("gui.logtech.terminal.view.save_hint")
                                        .withStyle(ChatFormatting.GRAY))
                ));
            }
        }
    }

    private void onViewButtonClicked(int slot) {
        List<SavedView> views = this.menu.getClientViews();
        boolean shift = hasShiftDown();
        if (shift) {
            saveCurrentView(slot);
        } else {
            SavedView v = (slot < views.size()) ? views.get(slot) : null;
            if (v != null) {
                loadView(v);
            }
        }
    }

    private void loadView(SavedView v) {
        if (this.searchBox != null) {
            this.searchBox.setValue(v.searchText());
        }
        currentSortMode = v.sortMode() == SavedView.SORT_NAME ? SortMode.NAME : SortMode.AMOUNT;
        currentTierFilter = v.tierFilter();
        currentMinPurity = v.minPurity();
        this.scrollRow = 0;
        updateSortButton();
        updateFilterButtons();
        refreshFilteredItems();
    }

    private void saveCurrentView(int slot) {
        String search = this.searchBox != null ? this.searchBox.getValue() : "";
        int sort = (currentSortMode == SortMode.NAME) ? SavedView.SORT_NAME : SavedView.SORT_AMOUNT;
        String name = "Vista " + (slot + 1);
        List<SavedView> views = this.menu.getClientViews();
        if (slot < views.size() && views.get(slot) != null) {
            name = views.get(slot).name();
        }
        SavedView toSave = new SavedView(name, search, sort, currentTierFilter, currentMinPurity);
        PacketDistributor.sendToServer(new TerminalViewUpdatePayload(
                this.menu.containerId,
                TerminalViewUpdatePayload.Action.SAVE,
                slot,
                toSave
        ));
    }

    private void updateFilterButtons() {
        if (this.tierFilterButton != null) {
            this.tierFilterButton.setMessage(getTierButtonText());
            this.tierFilterButton.setTooltip(getTierButtonTooltip());
        }
        if (this.purityFilterButton != null) {
            this.purityFilterButton.setMessage(getPurityButtonText());
            this.purityFilterButton.setTooltip(getPurityButtonTooltip());
        }
    }

    private Component getTierButtonText() {
        return switch (currentTierFilter) {
            case SavedView.TIER_HOT -> Component.literal("T:H").withStyle(ChatFormatting.GOLD);
            case SavedView.TIER_COLD -> Component.literal("T:C").withStyle(ChatFormatting.AQUA);
            default -> Component.literal("T:*");
        };
    }

    private Tooltip getTierButtonTooltip() {
        return Tooltip.create(switch (currentTierFilter) {
            case SavedView.TIER_HOT -> Component.translatable("gui.logtech.terminal.filter.tier.hot");
            case SavedView.TIER_COLD -> Component.translatable("gui.logtech.terminal.filter.tier.cold");
            default -> Component.translatable("gui.logtech.terminal.filter.tier.all");
        });
    }

    private Component getPurityButtonText() {
        if (currentMinPurity == 0) {
            return Component.literal("P:*");
        }
        return Component.literal("P:" + currentMinPurity).withStyle(ChatFormatting.YELLOW);
    }

    private Tooltip getPurityButtonTooltip() {
        if (currentMinPurity == 0) {
            return Tooltip.create(Component.translatable("gui.logtech.terminal.filter.purity.none"));
        }
        return Tooltip.create(Component.translatable("gui.logtech.terminal.filter.purity.min", currentMinPurity));
    }
}
