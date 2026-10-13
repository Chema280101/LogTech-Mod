package io.github.chema280101.logtech.data;

import io.github.chema280101.logtech.registry.ModBlocks;
import io.github.chema280101.logtech.registry.ModDataComponents;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class ModLootTableProvider {
    public static LootTableProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        return new LootTableProvider(
                output,
                Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootSubProvider::new, LootContextParamSets.BLOCK)),
                lookupProvider
        );
    }

    public static class ModBlockLootSubProvider extends BlockLootSubProvider {
        public ModBlockLootSubProvider(HolderLookup.Provider provider) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
        }

        @Override
        protected void generate() {
            this.add(
                    ModBlocks.CORE.get(),
                    LootTable.lootTable().withPool(
                            this.applyExplosionCondition(
                                    ModBlocks.CORE.get(),
                                    LootPool.lootPool()
                                            .setRolls(ConstantValue.exactly(1.0F))
                                            .add(
                                                    LootItem.lootTableItem(ModBlocks.CORE.get())
                                                            .apply(
                                                                    CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                                                            .include(ModDataComponents.STORAGE_CONTENT.get())
                                                                            .include(ModDataComponents.COLD_STORAGE_CONTENT.get())
                                                            )
                                            )
                            )
                    )
            );
            this.dropSelf(ModBlocks.BACKBONE.get());
            this.dropSelf(ModBlocks.RACK.get());
            this.dropSelf(ModBlocks.COLD_RACK.get());
            this.dropSelf(ModBlocks.TERMINAL.get());
            this.dropSelf(ModBlocks.CRUSHER.get());
            this.dropSelf(ModBlocks.DRILL.get());
            this.dropSelf(ModBlocks.ALARM.get());
            this.dropSelf(ModBlocks.COAL_GENERATOR.get());
            this.dropSelf(ModBlocks.CENTRIFUGE.get());
            this.dropSelf(ModBlocks.WASHER.get());
            this.dropSelf(ModBlocks.SLAG_BLOCK.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(
                    ModBlocks.CORE.get(),
                    ModBlocks.BACKBONE.get(),
                    ModBlocks.RACK.get(),
                    ModBlocks.COLD_RACK.get(),
                    ModBlocks.TERMINAL.get(),
                    ModBlocks.CRUSHER.get(),
                    ModBlocks.DRILL.get(),
                    ModBlocks.ALARM.get(),
                    ModBlocks.COAL_GENERATOR.get(),
                    ModBlocks.CENTRIFUGE.get(),
                    ModBlocks.WASHER.get(),
                    ModBlocks.SLAG_BLOCK.get()
            );
        }
    }
}
