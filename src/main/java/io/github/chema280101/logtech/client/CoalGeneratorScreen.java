package io.github.chema280101.logtech.client;

import io.github.chema280101.logtech.machine.CoalGeneratorMenu;
import io.github.chema280101.logtech.machine.MachineStatus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CoalGeneratorScreen extends AbstractContainerScreen<CoalGeneratorMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");

    public CoalGeneratorScreen(CoalGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // 1. Textura base de contenedor (horno vanilla)
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 2. Ocultar ranuras vanilla y flechas del horno
        guiGraphics.fill(x + 50, y + 15, x + 155, y + 72, 0xFFC6C6C6);

        // 3. Dibujar ranura de combustible en (79, 52)
        int slotX = x + 79;
        int slotY = y + 52;
        guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 1, 0xFF373737);
        guiGraphics.fill(slotX, slotY, slotX + 1, slotY + 18, 0xFF373737);
        guiGraphics.fill(slotX + 17, slotY, slotX + 18, slotY + 18, 0xFFFFFFFF);
        guiGraphics.fill(slotX, slotY + 17, slotX + 18, slotY + 18, 0xFFFFFFFF);
        guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF8B8B8B);

        // 4. Llama de combustión sobre la ranura en (81, 35)
        int flameX = x + 81;
        int flameY = y + 35;
        // Fondo de llama apagada (desde furnace.png x=56, y=36)
        guiGraphics.blit(TEXTURE, flameX, flameY, 56, 36, 14, 14);

        int maxBurn = this.menu.getMaxBurnTime();
        int burn = this.menu.getBurnTime();
        if (maxBurn > 0 && burn > 0) {
            int flameH = Math.min(14, (burn * 14) / maxBurn);
            if (flameH > 0) {
                // Llama encendida (desde furnace.png x=176, y=0)
                guiGraphics.blit(TEXTURE, flameX, flameY + 14 - flameH, 176, 14 - flameH, 14, flameH);
            }
        }

        // 5. Barra medidora de energía FE
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

        // 6. Texto de estado
        MachineStatus status = this.menu.getStatus();
        Component statusText = status.getDisplayName();
        int textColor = switch (status) {
            case WORKING -> 0x2E7D32;
            case NO_ENERGY, SHED, OVERHEATED -> 0xD32F2F;
            case NO_INPUT, NO_RESOURCE, NO_FLUID, IDLE -> 0x757575;
            case OUTPUT_FULL -> 0xF57C00;
        };
        int textX = x + 88 - (this.font.width(statusText) / 2);
        guiGraphics.drawString(this.font, statusText, textX, y + 20, textColor, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltip de energía en la barra
        int barX = this.leftPos + 18;
        int barY = this.topPos + 18;
        if (mouseX >= barX && mouseX < barX + 12 && mouseY >= barY && mouseY < barY + 48) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(this.menu.getEnergy() + " / " + this.menu.getMaxEnergy() + " FE"));
            if (this.menu.getStatus() == MachineStatus.WORKING) {
                tooltip.add(Component.translatable("gui.logtech.coal_generator.generating", 40));
            }
            guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }

        // Tooltip de combustión en la llama
        int flameX = this.leftPos + 81;
        int flameY = this.topPos + 35;
        if (mouseX >= flameX && mouseX < flameX + 14 && mouseY >= flameY && mouseY < flameY + 14) {
            guiGraphics.renderTooltip(this.font, Component.literal(this.menu.getBurnTime() + " / " + this.menu.getMaxBurnTime() + " t"), mouseX, mouseY);
        }
    }
}
