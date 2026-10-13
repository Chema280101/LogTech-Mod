package io.github.chema280101.logtech.client;

import io.github.chema280101.logtech.machine.MachineStatus;
import io.github.chema280101.logtech.machine.WasherMenu;
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

public class WasherScreen extends AbstractContainerScreen<WasherMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");

    private Button priorityButton;

    public WasherScreen(WasherMenu menu, Inventory playerInventory, Component title) {
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

        // 2. Limpiar zona central del horno vanilla para nuestro diseño personalizado
        guiGraphics.fill(x + 50, y + 14, x + 162, y + 71, 0xFFC6C6C6);

        // 3. Ranura de entrada en (55, 34)
        drawSlot(guiGraphics, x + 55, y + 34);

        // 4. Dos ranuras de salida en (115, 34) y (135, 34)
        drawSlot(guiGraphics, x + 115, y + 34);
        drawSlot(guiGraphics, x + 135, y + 34);

        // 5. Flecha de progreso animada entre entrada y salidas
        int maxProgress = this.menu.getMaxProgress();
        int progress = this.menu.getProgress();
        int arrowWidth = maxProgress > 0 ? (progress * 24) / maxProgress : 0;
        if (arrowWidth > 0) {
            guiGraphics.blit(TEXTURE, x + 82, y + 34, 176, 14, arrowWidth, 17);
        }

        // 6. Barra medidora de energía FE (columna izquierda 1)
        int energyX = x + 12;
        int energyY = y + 18;
        guiGraphics.fill(energyX, energyY, energyX + 12, energyY + 48, 0xFF373737);
        guiGraphics.fill(energyX + 11, energyY, energyX + 12, energyY + 48, 0xFFFFFFFF);
        guiGraphics.fill(energyX, energyY + 47, energyX + 12, energyY + 48, 0xFFFFFFFF);
        guiGraphics.fill(energyX + 1, energyY + 1, energyX + 11, energyY + 47, 0xFF2B2B2B);

        int maxEnergy = this.menu.getMaxEnergy();
        int energy = this.menu.getEnergy();
        int energyHeight = maxEnergy > 0 ? (int) (((long) energy * 46) / maxEnergy) : 0;
        if (energyHeight > 0) {
            guiGraphics.fill(energyX + 1, energyY + 1 + (46 - energyHeight), energyX + 11, energyY + 47, 0xFFE53935);
            guiGraphics.fill(energyX + 2, energyY + 1 + (46 - energyHeight), energyX + 4, energyY + 47, 0xFFFF7043);
        }

        // 7. Tanque medidor de agua (columna izquierda 2)
        int waterX = x + 30;
        int waterY = y + 18;
        guiGraphics.fill(waterX, waterY, waterX + 12, waterY + 48, 0xFF373737);
        guiGraphics.fill(waterX + 11, waterY, waterX + 12, waterY + 48, 0xFFFFFFFF);
        guiGraphics.fill(waterX, waterY + 47, waterX + 12, waterY + 48, 0xFFFFFFFF);
        guiGraphics.fill(waterX + 1, waterY + 1, waterX + 11, waterY + 47, 0xFF0B192C);

        int waterCapacity = this.menu.getFluidCapacity();
        int waterAmount = this.menu.getFluidAmount();
        int waterHeight = waterCapacity > 0 ? (int) (((long) waterAmount * 46) / waterCapacity) : 0;
        if (waterHeight > 0) {
            guiGraphics.fill(waterX + 1, waterY + 1 + (46 - waterHeight), waterX + 11, waterY + 47, 0xFF1976D2);
            guiGraphics.fill(waterX + 2, waterY + 1 + (46 - waterHeight), waterX + 4, waterY + 47, 0xFF64B5F6);
        }

        // 8. Texto de estado explícito
        MachineStatus status = this.menu.getStatus();
        Component statusText = status.getDisplayName();
        int textColor = switch (status) {
            case WORKING -> 0x2E7D32;
            case NO_ENERGY, SHED -> 0xD32F2F;
            case NO_FLUID -> 0x0277BD;
            case NO_INPUT, NO_RESOURCE, IDLE -> 0x757575;
            case OUTPUT_FULL -> 0xF57C00;
        };
        int textX = x + 105 - (this.font.width(statusText) / 2);
        guiGraphics.drawString(this.font, statusText, textX, y + 58, textColor, false);
    }

    private static void drawSlot(GuiGraphics guiGraphics, int slotX, int slotY) {
        guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 1, 0xFF373737);
        guiGraphics.fill(slotX, slotY, slotX + 1, slotY + 18, 0xFF373737);
        guiGraphics.fill(slotX + 17, slotY, slotX + 18, slotY + 18, 0xFFFFFFFF);
        guiGraphics.fill(slotX, slotY + 17, slotX + 18, slotY + 18, 0xFFFFFFFF);
        guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF8B8B8B);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.priorityButton != null) {
            this.priorityButton.setMessage(getPriorityButtonText());
        }
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltip de energía al pasar el ratón por la barra de energía
        int energyX = this.leftPos + 12;
        int energyY = this.topPos + 18;
        if (mouseX >= energyX && mouseX < energyX + 12 && mouseY >= energyY && mouseY < energyY + 48) {
            guiGraphics.renderTooltip(this.font, Component.literal(this.menu.getEnergy() + " / " + this.menu.getMaxEnergy() + " FE"), mouseX, mouseY);
        }

        // Tooltip del tanque de agua
        int waterX = this.leftPos + 30;
        int waterY = this.topPos + 18;
        if (mouseX >= waterX && mouseX < waterX + 12 && mouseY >= waterY && mouseY < waterY + 48) {
            Component waterLabel = Component.translatable("gui.logtech.washer.water");
            guiGraphics.renderTooltip(this.font, Component.literal(this.menu.getFluidAmount() + " / " + this.menu.getFluidCapacity() + " mB (")
                    .append(waterLabel).append(")"), mouseX, mouseY);
        }

        // Tooltip de progreso en la flecha
        int arrowX = this.leftPos + 82;
        int arrowY = this.topPos + 34;
        if (mouseX >= arrowX && mouseX < arrowX + 24 && mouseY >= arrowY && mouseY < arrowY + 17) {
            guiGraphics.renderTooltip(this.font, Component.literal(this.menu.getProgress() + " / " + this.menu.getMaxProgress() + " t"), mouseX, mouseY);
        }
    }
}
