package io.github.chema280101.logtech.compat.jei;

import io.github.chema280101.logtech.machine.recipe.WashingRecipe;
import io.github.chema280101.logtech.registry.ModBlocks;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.network.chat.Component;

public class WashingCategory extends ProcessingRecipeCategory<WashingRecipe> {

    public WashingCategory(IGuiHelper guiHelper) {
        super(
                LogTechJeiPlugin.WASHING_TYPE,
                Component.translatable("jei.logtech.category.washing"),
                ModBlocks.WASHER.get(),
                guiHelper
        );
    }

    @Override
    protected int getRepresentativeInputPurity() {
        return 2;
    }
}
