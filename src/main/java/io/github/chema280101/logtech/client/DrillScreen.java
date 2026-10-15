package io.github.chema280101.logtech.client;

import io.github.chema280101.logtech.machine.DrillMenu;
import io.github.chema280101.logtech.machine.MachineStatus;
import io.github.chema280101.logtech.network.EnergyPriority;
import io.github.chema280101.logtech.network.MachinePriorityPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class DrillScreen extends AbstractContainerScreen<DrillMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");

    private Button priorityButton;

    public DrillScreen(DrillMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        this.priorityButton = Button.builder(
                getPriorityButtonText(),
                btn -> {
                    EnergyPriority next = this.menu.getEnergyPriority().next();
                    PacketDistributor.sendToServer(new MachinePriorityPayload(this.menu.containerId, next.ordinal()));
                }
        )
        .bounds(this.leftPos + 116, this.topPos + 4, 54, 12)
        .tooltip(Tooltip.create(Component.translatable("gui.logtech.energy_priority.tooltip")))
        .build();
        addRenderableWidget(this.priorityButton);
    }

    private Component getPriorityButtonText() {
        return Component.translatable(
                "gui.logtech.energy_priority.button",
                Component.translatable(this.menu.getEnergyPriority().getTranslationKey())
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // 1. Textura base de contenedor
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 2. Limpiar zona central
        guiGraphics.fill(x + 50, y + 13, x + 162, y + 71, 0xFFC6C6C6);

        // 3. Ranura de firmware en (79, 19) para slot (80, 20)
        drawSlot(guiGraphics, x + 79, y + 19);

        // 4. Ranura de búfer/salida en (79, 47) para slot (80, 48)
        drawSlot(guiGraphics, x + 79, y + 47);

        // 5. Barra de progreso vertical u horizontal
        int maxProgress = this.menu.getMaxProgress();
        int progress = this.menu.getProgress();
        int progWidth = maxProgress > 0 ? (progress * 18) / maxProgress : 0;
        if (progWidth > 0) {
            guiGraphics.fill(x + 79, y + 41, x + 79 + progWidth, y + 44, 0xFF4CAF50);
        }

        // 6. Barra medidora de energía FE
        int barX = x + 18;
        int barY = y + 18;
        guiGraphics.fill(barX, barY, barX + 12, barY + 48, 0xFF373737);
        guiGraphics.fill(barX + 11, barY, barX + 12, barY + 48, 0xFFFFFFFF);
        guiGraphics.fill(barX, barY + 47, barX + 12, barY + 48, 0xFFFFFFFF);
        guiGraphics.fill(barX + 1, barY + 1, barX + 11, barY + 47, 0xFF2B2B2B);

        int maxEnergy = this.menu.getMaxEnergy();
        int energy = this.menu.getEnergy();
        int energyHeight = maxEnergy > 0 ? (int) (((long) energy * 46) / maxEnergy) : 0;
        if (energyHeight > 0) {
            guiGraphics.fill(barX + 1, barY + 1 + (46 - energyHeight), barX + 11, barY + 47, 0xFFE53935);
            guiGraphics.fill(barX + 2, barY + 1 + (46 - energyHeight), barX + 4, barY + 47, 0xFFFF7043);
        }

        // 7. Barra medidora de calor
        int heatX = x + 34;
        int heatY = y + 18;
        guiGraphics.fill(heatX, heatY, heatX + 12, heatY + 48, 0xFF373737);
        guiGraphics.fill(heatX + 11, heatY, heatX + 12, heatY + 48, 0xFFFFFFFF);
        guiGraphics.fill(heatX, heatY + 47, heatX + 12, heatY + 48, 0xFFFFFFFF);
        guiGraphics.fill(heatX + 1, heatY + 1, heatX + 11, heatY + 47, 0xFF2B2B2B);

        int heat = this.menu.getHeat();
        int heatHeight = Math.min(46, (int) Math.round((heat * 46.0) / 100.0));
        if (heatHeight > 0) {
            int barColor = (heat >= 100) ? 0xFFB71C1C : (heat > 70 ? 0xFFFF5722 : 0xFFFF9800);
            int highlightColor = (heat >= 100) ? 0xFFE53935 : (heat > 70 ? 0xFFFF8A65 : 0xFFFFB74D);
            guiGraphics.fill(heatX + 1, heatY + 1 + (46 - heatHeight), heatX + 11, heatY + 47, barColor);
            guiGraphics.fill(heatX + 2, heatY + 1 + (46 - heatHeight), heatX + 4, heatY + 47, highlightColor);
        }

        // 8. Texto de estado explícito
        MachineStatus status = this.menu.getStatus();
        Component statusText = status.getDisplayName();
        int textColor = switch (status) {
            case WORKING -> 0x2E7D32;
            case NO_ENERGY, SHED, OVERHEATED -> 0xD32F2F;
            case NO_INPUT, NO_RESOURCE, NO_FLUID, IDLE -> 0x757575;
            case OUTPUT_FULL -> 0xF57C00;
        };
        int textX = x + 115 - (this.font.width(statusText) / 2);
        guiGraphics.drawString(this.font, statusText, textX, y + 40, textColor, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.priorityButton != null) {
            this.priorityButton.setMessage(getPriorityButtonText());
        }
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltip de energía
        int barX = this.leftPos + 18;
        int barY = this.topPos + 18;
        if (mouseX >= barX && mouseX < barX + 12 && mouseY >= barY && mouseY < barY + 48) {
            guiGraphics.renderTooltip(this.font, Component.literal(this.menu.getEnergy() + " / " + this.menu.getMaxEnergy() + " FE"), mouseX, mouseY);
        }

        // Tooltip de calor
        int heatX = this.leftPos + 34;
        int heatY = this.topPos + 18;
        if (mouseX >= heatX && mouseX < heatX + 12 && mouseY >= heatY && mouseY < heatY + 48) {
            java.util.List<Component> tooltip = new java.util.ArrayList<>();
            int heat = this.menu.getHeat();
            tooltip.add(Component.translatable("gui.logtech.heat.tooltip", heat));
            tooltip.add(Component.translatable("gui.logtech.cooling.tooltip", String.format(java.util.Locale.ROOT, "%.1f", this.menu.getCooling())).withStyle(net.minecraft.ChatFormatting.AQUA));
            if (this.menu.getStatus() == MachineStatus.OVERHEATED) {
                tooltip.add(Component.translatable("gui.logtech.heat.overheated").withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD));
            } else if (heat > 70) {
                tooltip.add(Component.translatable("gui.logtech.heat.slowdown").withStyle(net.minecraft.ChatFormatting.GOLD));
            }
            guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }

        // Tooltip de progreso en la barrita de progreso
        int progX = this.leftPos + 79;
        int progY = this.topPos + 39;
        if (mouseX >= progX && mouseX < progX + 18 && mouseY >= progY && mouseY < progY + 7) {
            guiGraphics.renderTooltip(this.font, Component.literal(this.menu.getProgress() + " / " + this.menu.getMaxProgress() + " t"), mouseX, mouseY);
        }
    }

    private static void drawSlot(GuiGraphics guiGraphics, int slotX, int slotY) {
        guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 1, 0xFF373737);
        guiGraphics.fill(slotX, slotY, slotX + 1, slotY + 18, 0xFF373737);
        guiGraphics.fill(slotX + 17, slotY, slotX + 18, slotY + 18, 0xFFFFFFFF);
        guiGraphics.fill(slotX, slotY + 17, slotX + 18, slotY + 18, 0xFFFFFFFF);
        guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF8B8B8B);
    }
}
