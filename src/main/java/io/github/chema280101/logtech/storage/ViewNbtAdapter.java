package io.github.chema280101.logtech.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Adaptador de persistencia NBT para SavedView y ViewManager.
 * Desacopla las clases de lógica pura de la API de Minecraft.
 */
public final class ViewNbtAdapter {

    private ViewNbtAdapter() {
    }

    /** Serializa un SavedView a CompoundTag. */
    public static CompoundTag saveView(SavedView view) {
        if (view == null) {
            return new CompoundTag();
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("name", view.name());
        tag.putString("search", view.searchText());
        tag.putInt("sort", view.sortMode());
        tag.putInt("tier", view.tierFilter());
        tag.putInt("purity", view.minPurity());
        return tag;
    }

    /** Deserializa un SavedView desde CompoundTag. Devuelve null si es inválido. */
    public static SavedView loadView(CompoundTag tag) {
        if (tag == null) {
            return null;
        }
        String name = tag.contains("name") ? tag.getString("name") : null;
        String sanitized = SavedView.sanitizeName(name);
        if (sanitized == null) {
            return null;
        }
        String search = SavedView.sanitizeSearch(tag.contains("search") ? tag.getString("search") : "");
        int sort = clamp(tag.contains("sort") ? tag.getInt("sort") : SavedView.SORT_AMOUNT, SavedView.SORT_AMOUNT, SavedView.SORT_NAME);
        int tier = clamp(tag.contains("tier") ? tag.getInt("tier") : SavedView.TIER_ALL, SavedView.TIER_ALL, SavedView.TIER_COLD);
        int purity = clamp(tag.contains("purity") ? tag.getInt("purity") : 0, 0, 5);
        return new SavedView(sanitized, search, sort, tier, purity);
    }

    /** Guarda todas las vistas de un ViewManager a un ListTag de NBT. */
    public static Tag saveViews(ViewManager manager, @SuppressWarnings("unused") HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        if (manager == null) {
            return list;
        }
        for (int i = 0; i < ViewManager.MAX_VIEWS; i++) {
            CompoundTag slotTag = new CompoundTag();
            slotTag.putInt("slot", i);
            SavedView v = manager.get(i);
            slotTag.putBoolean("occupied", v != null);
            if (v != null) {
                slotTag.put("view", saveView(v));
            }
            list.add(slotTag);
        }
        return list;
    }

    /** Carga las vistas desde NBT hacia un ViewManager. */
    public static void loadViews(ViewManager manager, Tag tag, @SuppressWarnings("unused") HolderLookup.Provider registries) {
        if (manager == null) {
            return;
        }
        manager.clear();
        if (!(tag instanceof ListTag list)) {
            manager.clearDirty();
            return;
        }
        for (Tag t : list) {
            if (!(t instanceof CompoundTag slotTag)) {
                continue;
            }
            int slot = slotTag.contains("slot") ? slotTag.getInt("slot") : -1;
            if (slot < 0 || slot >= ViewManager.MAX_VIEWS) {
                continue;
            }
            boolean occupied = slotTag.contains("occupied") && slotTag.getBoolean("occupied");
            if (!occupied) {
                continue;
            }
            if (slotTag.contains("view", Tag.TAG_COMPOUND)) {
                SavedView loaded = loadView(slotTag.getCompound("view"));
                manager.setDirect(slot, loaded);
            }
        }
        manager.clearDirty();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
