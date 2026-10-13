package io.github.chema280101.logtech.compat.jei;

import io.github.chema280101.logtech.machine.recipe.OutputScaling;
import io.github.chema280101.logtech.machine.recipe.ProcessingLogic;
import io.github.chema280101.logtech.machine.recipe.ProcessingOutput;
import io.github.chema280101.logtech.machine.recipe.ProcessingRecipe;
import io.github.chema280101.logtech.machine.recipe.PurityMode;
import io.github.chema280101.logtech.registry.ModDataComponents;
import io.github.chema280101.logtech.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class ProcessingRecipeCategory<T extends ProcessingRecipe> implements IRecipeCategory<T> {

    protected final RecipeType<T> recipeType;
    protected final Component title;
    protected final IDrawable background;
    protected final IDrawable icon;
    protected final IDrawableAnimated arrow;

    public ProcessingRecipeCategory(
            RecipeType<T> recipeType,
            Component title,
            ItemLike iconItem,
            IGuiHelper guiHelper
    ) {
        this.recipeType = recipeType;
        this.title = title;
        this.background = guiHelper.createBlankDrawable(166, 74);
        this.icon = guiHelper.createDrawableItemLike(iconItem);
        this.arrow = guiHelper.createAnimatedRecipeArrow(50);
    }

    @Override
    public RecipeType<T> getRecipeType() {
        return this.recipeType;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return 166;
    }

    @Override
    public int getHeight() {
        return 74;
    }

    @SuppressWarnings("removal")
    @Override
    public @Nullable IDrawable getBackground() {
        return this.background;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    protected int getRepresentativeInputPurity() {
        return 1;
    }

    protected static boolean canHavePurity(Item item) {
        return item == ModItems.RAW_NEOFERRITE.get()
                || item == ModItems.NEOFERRITE_DUST.get()
                || item == ModItems.RAW_CRYSTALITE.get()
                || item == ModItems.CRYSTALITE_DUST.get();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
        int repInputPurity = getRepresentativeInputPurity();

        // 1. Ingrediente de entrada con pureza representativa
        ItemStack[] items = recipe.ingredient().getItems();
        List<ItemStack> inputStacks = new ArrayList<>();
        boolean inputHasPurity = false;
        for (ItemStack item : items) {
            ItemStack copy = item.copy();
            if (canHavePurity(copy.getItem())) {
                ModDataComponents.setPurity(copy, repInputPurity);
                inputHasPurity = true;
            }
            inputStacks.add(copy);
        }

        var inSlot = builder.addInputSlot(12, 10).setStandardSlotBackground();
        inSlot.addItemStacks(inputStacks);
        if (inputHasPurity) {
            inSlot.addRichTooltipCallback((slotView, tooltip) -> {
                tooltip.add(Component.translatable("jei.logtech.representative_purity", repInputPurity)
                        .withStyle(ChatFormatting.DARK_AQUA));
            });
        }

        // 2. Salidas con probabilidad y pureza resultante
        List<ProcessingOutput> outputs = recipe.outputs();
        for (int i = 0; i < outputs.size(); i++) {
            ProcessingOutput output = outputs.get(i);
            ItemStack outStack = output.createStack();
            if (canHavePurity(outStack.getItem())) {
                int outPurity = ProcessingLogic.calculateOutputPurity(repInputPurity, recipe.purityMode());
                ModDataComponents.setPurity(outStack, outPurity);
            }

            int slotX = 74 + (i * 22);
            var outSlot = builder.addOutputSlot(slotX, 10).setStandardSlotBackground();
            outSlot.addItemStack(outStack);

            outSlot.addRichTooltipCallback((slotView, tooltip) -> {
                if (output.scaling() == OutputScaling.INVERSE_PURITY) {
                    tooltip.add(Component.translatable("jei.logtech.chance.scaling", Math.round(output.chance() * 100))
                            .withStyle(ChatFormatting.GOLD));
                } else if (output.chance() < 1.0f) {
                    tooltip.add(Component.translatable("jei.logtech.chance", Math.round(output.chance() * 100))
                            .withStyle(ChatFormatting.GOLD));
                }
            });
        }
    }

    @Override
    public void draw(T recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // Flecha animada entre entrada y salidas
        this.arrow.draw(guiGraphics, 40, 10);

        Font font = Minecraft.getInstance().font;

        // Línea 1: Energía y Tiempo
        int energyPerTick = recipe.getEnergyPerTick();
        float seconds = recipe.ticks() / 20.0f;
        String secondsStr = String.format(Locale.ROOT, "%.1f", seconds);
        Component energyTime = Component.translatable(
                "jei.logtech.energy_time",
                recipe.energy(),
                energyPerTick,
                secondsStr,
                recipe.ticks()
        );
        guiGraphics.drawString(font, energyTime, 4, 34, 0x444444, false);

        int nextY = 46;

        // Línea 2 (si aplica): Agua
        if (recipe.fluidAmount() > 0) {
            Component water = Component.translatable("jei.logtech.water", recipe.fluidAmount());
            guiGraphics.drawString(font, water, 4, nextY, 0x1976D2, false);
            nextY += 12;
        }

        // Línea de pureza
        if (recipe.purityMode() == PurityMode.INCREASE) {
            Component purityEffect = Component.translatable("jei.logtech.purity.increase");
            guiGraphics.drawString(font, purityEffect, 4, nextY, 0x2E7D32, false);
        } else {
            Component purityEffect = Component.translatable("jei.logtech.purity.preserve");
            guiGraphics.drawString(font, purityEffect, 4, nextY, 0x666666, false);
        }

        // Nota adicional si hay salidas con escalado inverso por pureza
        boolean hasScaling = recipe.outputs().stream().anyMatch(o -> o.scaling() == OutputScaling.INVERSE_PURITY);
        if (hasScaling && nextY + 11 <= 72) {
            Component note = Component.translatable("jei.logtech.chance_note");
            guiGraphics.drawString(font, note, 4, nextY + 11, 0xD84315, false);
        }
    }
}
