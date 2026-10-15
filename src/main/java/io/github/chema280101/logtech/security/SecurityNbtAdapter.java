package io.github.chema280101.logtech.security;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Adaptador de persistencia NBT para {@link NetworkSecurityManager} y {@link PlayerAccessEntry}.
 * Desacopla las clases de lógica pura de la API de Minecraft.
 */
public final class SecurityNbtAdapter {

    private SecurityNbtAdapter() {
    }

    public static CompoundTag save(NetworkSecurityManager manager) {
        CompoundTag tag = new CompoundTag();
        if (manager.getOwnerUuid() != null) {
            tag.putUUID("owner_uuid", manager.getOwnerUuid());
            tag.putString("owner_name", manager.getOwnerName());
        }
        tag.putString("mode", manager.getMode().name());

        ListTag list = new ListTag();
        for (PlayerAccessEntry entry : manager.getEntryList()) {
            list.add(saveEntry(entry));
        }
        tag.put("entries", list);
        return tag;
    }

    public static void load(NetworkSecurityManager manager, CompoundTag tag) {
        if (tag.hasUUID("owner_uuid")) {
            manager.setOwner(tag.getUUID("owner_uuid"), tag.getString("owner_name"));
        } else {
            manager.setOwner(null, "");
        }

        if (tag.contains("mode")) {
            try {
                manager.setMode(SecurityMode.valueOf(tag.getString("mode")));
            } catch (IllegalArgumentException e) {
                manager.setMode(SecurityMode.OPEN);
            }
        } else {
            manager.setMode(SecurityMode.OPEN);
        }

        manager.clearEntries();
        if (tag.contains("entries", Tag.TAG_LIST)) {
            ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                PlayerAccessEntry entry = loadEntry(list.getCompound(i));
                manager.addOrUpdatePlayer(
                        entry.getPlayerUuid(),
                        entry.getLastKnownName(),
                        entry.canView(),
                        entry.canExtract(),
                        entry.canInsert(),
                        entry.canAdmin()
                );
            }
        }
    }

    public static CompoundTag saveEntry(PlayerAccessEntry entry) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", entry.getPlayerUuid());
        tag.putString("name", entry.getLastKnownName());
        tag.putBoolean("view", entry.canView());
        tag.putBoolean("extract", entry.canExtract());
        tag.putBoolean("insert", entry.canInsert());
        tag.putBoolean("admin", entry.canAdmin());
        return tag;
    }

    public static PlayerAccessEntry loadEntry(CompoundTag tag) {
        UUID uuid = tag.hasUUID("uuid") ? tag.getUUID("uuid") : UUID.randomUUID();
        String name = tag.getString("name");
        boolean view = tag.getBoolean("view");
        boolean extract = tag.getBoolean("extract");
        boolean insert = tag.getBoolean("insert");
        boolean admin = tag.getBoolean("admin");
        return new PlayerAccessEntry(uuid, name, view, extract, insert, admin);
    }
}
