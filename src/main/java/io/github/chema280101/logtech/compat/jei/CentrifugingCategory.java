package io.github.chema280101.logtech.compat.jei;

import io.github.chema280101.logtech.machine.recipe.CentrifugingRecipe;
import io.github.chema280101.logtech.registry.ModBlocks;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.network.chat.Component;

public class CentrifugingCategory extends ProcessingRecipeCategory<CentrifugingRecipe> {

    public CentrifugingCategory(IGuiHelper guiHelper) {
        super(
                LogTechJeiPlugin.CENTRIFUGING_TYPE,
                Component.translatable("jei.logtech.category.centrifuging"),
                ModBlocks.CENTRIFUGE.get(),
                guiHelper
        );
    }

    @Override
    protected int getRepresentativeInputPurity() {
        return 1;
    }
}
