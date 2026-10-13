package io.github.chema280101.logtech.compat.jei;

import io.github.chema280101.logtech.machine.recipe.CrushingRecipe;
import io.github.chema280101.logtech.registry.ModBlocks;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.network.chat.Component;

public class CrushingCategory extends ProcessingRecipeCategory<CrushingRecipe> {

    public CrushingCategory(IGuiHelper guiHelper) {
        super(
                LogTechJeiPlugin.CRUSHING_TYPE,
                Component.translatable("jei.logtech.category.crushing"),
                ModBlocks.CRUSHER.get(),
                guiHelper
        );
    }

    @Override
    protected int getRepresentativeInputPurity() {
        return 1;
    }
}
