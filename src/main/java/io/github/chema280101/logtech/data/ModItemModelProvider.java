package io.github.chema280101.logtech.data;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, LogTech.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.DATA_MODULE.get());
        basicItem(ModItems.DATA_MODULE_ADVANCED.get());
        basicItem(ModItems.DATA_MODULE_MASTER.get());
        basicItem(ModItems.RAW_NEOFERRITE.get());
        basicItem(ModItems.NEOFERRITE_DUST.get());
        basicItem(ModItems.NEOFERRITE_INGOT.get());
        basicItem(ModItems.RAW_CRYSTALITE.get());
        basicItem(ModItems.CRYSTALITE_DUST.get());
        basicItem(ModItems.DATA_CRYSTAL.get());
        basicItem(ModItems.SCANNER.get());
        basicItem(ModItems.SLAG.get());
        basicItem(ModItems.CRYSTAL_SHARD.get());
        basicItem(ModItems.SLUDGE.get());
        basicItem(ModItems.FIRMWARE_ECO.get());
        basicItem(ModItems.FIRMWARE_TURBO.get());
        basicItem(ModItems.FIRMWARE_SAFE.get());
        basicItem(ModItems.FIRMWARE_SELECTIVE.get());
        basicItem(ModItems.CONFIG_KEY.get());
    }
}
