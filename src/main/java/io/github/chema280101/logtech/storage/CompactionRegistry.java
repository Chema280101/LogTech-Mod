package io.github.chema280101.logtech.storage;

import io.github.chema280101.logtech.LogTechConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

public final class CompactionRegistry {

    private static final Map<Item, CompactionPair<Item>> PAIRS = new ConcurrentHashMap<>();
    private static volatile boolean dirty = true;

    private CompactionRegistry() {}

    public static void markDirty() {
        dirty = true;
    }

    public static boolean isDirty() {
        return dirty;
    }

    public static CompactionPair<Item> getPair(Item item) {
        if (item == null) {
            return null;
        }
        return PAIRS.get(item);
    }

    public static Map<Item, CompactionPair<Item>> getAllPairs() {
        return Collections.unmodifiableMap(PAIRS);
    }

    public static void ensureInitialized(RecipeManager recipeManager, HolderLookup.Provider registries) {
        if (dirty) {
            reload(recipeManager, registries);
        }
    }

    public static synchronized void reload(RecipeManager recipeManager, HolderLookup.Provider registries) {
        if (recipeManager == null || registries == null) {
            return;
        }

        // Obtener lista negra de configuración
        Set<Item> blacklist = new HashSet<>();
        try {
            List<? extends String> configList = LogTechConfig.COMPACT_BLACKLIST.get();
            if (configList != null) {
                for (String idStr : configList) {
                    if (idStr != null && !idStr.isBlank()) {
                        ResourceLocation loc = ResourceLocation.tryParse(idStr.trim());
                        if (loc != null && BuiltInRegistries.ITEM.containsKey(loc)) {
                            blacklist.add(BuiltInRegistries.ITEM.get(loc));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Durante pruebas o si la configuración aún no está enlazada
        }

        Collection<RecipeHolder<CraftingRecipe>> craftingRecipes =
                recipeManager.getAllRecipesFor(RecipeType.CRAFTING);

        List<CompactionCandidate<Item>> candidates = new ArrayList<>(craftingRecipes.size());
        for (RecipeHolder<CraftingRecipe> holder : craftingRecipes) {
            CompactionCandidate<Item> candidate = CompactionRecipeAdapter.toCandidate(holder, registries);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }

        Map<Item, CompactionPair<Item>> detected = CompactionDetector.detectPairs(candidates, blacklist);
        PAIRS.clear();
        PAIRS.putAll(detected);
        dirty = false;
    }

    public static void clear() {
        PAIRS.clear();
        dirty = true;
    }
}
