package io.github.chema280101.logtech.storage;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum StorageTier implements StringRepresentable {
    HOT("hot"),
    COLD("cold");

    private final String name;

    StorageTier(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public String getName() {
        return this.name;
    }

    public static StorageTier byName(String name) {
        if (name == null) {
            return HOT;
        }
        for (StorageTier tier : values()) {
            if (tier.name.equalsIgnoreCase(name)) {
                return tier;
            }
        }
        return HOT;
    }

    @Override
    public String toString() {
        return this.name.toUpperCase(Locale.ROOT);
    }
}
