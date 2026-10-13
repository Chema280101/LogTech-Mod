package io.github.chema280101.logtech.client;

import io.github.chema280101.logtech.machine.CrusherMenu;
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

public class CrusherScreen extends AbstractContainerScreen<CrusherMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");

    private Button priorityButton;

    public CrusherScreen(CrusherMenu menu, Inventory playerInventory, Component title) {
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

        // 2. Ocultar zona de combustión del horno vanilla y centrar la ranura de entrada en Y=35
        guiGraphics.fill(x + 55, y + 16, x + 73, y + 71, 0xFFC6C6C6);

        // Dibujar ranura de entrada en (55, 34)
        int slotX = x + 55;
        int slotY = y + 34;
        guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 1, 0xFF373737);
        guiGraphics.fill(slotX, slotY, slotX + 1, slotY + 18, 0xFF373737);
        guiGraphics.fill(slotX + 17, slotY, slotX + 18, slotY + 18, 0xFFFFFFFF);
        guiGraphics.fill(slotX, slotY + 17, slotX + 18, slotY + 18, 0xFFFFFFFF);
        guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF8B8B8B);

        // 3. Flecha de progreso animada
        int maxProgress = this.menu.getMaxProgress();
        int progress = this.menu.getProgress();
        int arrowWidth = maxProgress > 0 ? (progress * 24) / maxProgress : 0;
        if (arrowWidth > 0) {
            guiGraphics.blit(TEXTURE, x + 79, y + 34, 176, 14, arrowWidth, 17);
        }

        // 4. Barra medidora de energía FE
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

        // 5. Texto de estado explícito
        MachineStatus status = this.menu.getStatus();
        Component statusText = status.getDisplayName();
        int textColor = switch (status) {
            case WORKING -> 0x2E7D32;
            case NO_ENERGY, SHED -> 0xD32F2F;
            case NO_INPUT, NO_RESOURCE, NO_FLUID, IDLE -> 0x757575;
            case OUTPUT_FULL -> 0xF57C00;
        };
        int textX = x + 98 - (this.font.width(statusText) / 2);
        guiGraphics.drawString(this.font, statusText, textX, y + 58, textColor, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.priorityButton != null) {
            this.priorityButton.setMessage(getPriorityButtonText());
        }
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltip de energía al pasar el ratón por la barra
        int barX = this.leftPos + 18;
        int barY = this.topPos + 18;
        if (mouseX >= barX && mouseX < barX + 12 && mouseY >= barY && mouseY < barY + 48) {
            guiGraphics.renderTooltip(this.font, Component.literal(this.menu.getEnergy() + " / " + this.menu.getMaxEnergy() + " FE"), mouseX, mouseY);
        }

        // Tooltip de progreso en la flecha
        int arrowX = this.leftPos + 79;
        int arrowY = this.topPos + 34;
        if (mouseX >= arrowX && mouseX < arrowX + 24 && mouseY >= arrowY && mouseY < arrowY + 17) {
            guiGraphics.renderTooltip(this.font, Component.literal(this.menu.getProgress() + " / " + this.menu.getMaxProgress() + " t"), mouseX, mouseY);
        }
    }
}
