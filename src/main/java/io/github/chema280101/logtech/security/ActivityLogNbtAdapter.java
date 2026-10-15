package io.github.chema280101.logtech.security;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Adaptador de persistencia NBT para el registro de actividad del Núcleo.
 */
public final class ActivityLogNbtAdapter {

    private ActivityLogNbtAdapter() {
    }

    public static CompoundTag save(ActivityLogBuffer<String> buffer) {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        if (buffer != null) {
            for (ActivityLogEntry<String> entry : buffer.getEntries()) {
                CompoundTag tag = new CompoundTag();
                tag.putLong("time", entry.gameTime());
                tag.putUUID("uuid", entry.playerUuid());
                tag.putString("name", entry.playerName());
                tag.putString("action", entry.action().name());
                tag.putString("item", entry.item() != null ? entry.item() : "");
                tag.putLong("amount", entry.amount());
                list.add(tag);
            }
        }
        root.put("entries", list);
        return root;
    }

    public static void load(ActivityLogBuffer<String> buffer, CompoundTag root, int maxCapacity) {
        if (buffer == null) {
            return;
        }
        buffer.clear();
        if (root == null || !root.contains("entries", Tag.TAG_LIST)) {
            return;
        }
        ListTag list = root.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            long time = tag.getLong("time");
            UUID uuid = tag.hasUUID("uuid") ? tag.getUUID("uuid") : null;
            if (uuid == null) {
                continue;
            }
            String name = tag.getString("name");
            String actionName = tag.getString("action");
            ActivityAction action;
            try {
                action = ActivityAction.valueOf(actionName);
            } catch (IllegalArgumentException e) {
                continue;
            }
            String item = tag.getString("item");
            long amount = tag.getLong("amount");
            buffer.addRawEntry(new ActivityLogEntry<>(time, uuid, name, action, item, amount), maxCapacity);
        }
    }
}
