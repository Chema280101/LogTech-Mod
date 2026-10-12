package io.github.chema280101.logtech.storage;

import java.util.Locale;
import net.minecraft.world.item.ItemStack;

public record TerminalItemEntry(ItemStack stack, long amount) {

    /**
     * Formatea una cantidad abreviada para mostrar en la interfaz (por ejemplo 12.3K, 1.5M).
     */
    public static String formatAmount(long amount) {
        if (amount < 1000) {
            return String.valueOf(amount);
        }
        if (amount < 1_000_000) {
            return formatWithSuffix(amount / 1000.0, "K");
        }
        if (amount < 1_000_000_000L) {
            return formatWithSuffix(amount / 1_000_000.0, "M");
        }
        return formatWithSuffix(amount / 1_000_000_000.0, "B");
    }

    private static String formatWithSuffix(double val, String suffix) {
        double rounded = Math.floor(val * 10.0) / 10.0;
        if (rounded == (long) rounded) {
            return String.format(Locale.ROOT, "%.0f%s", rounded, suffix);
        } else {
            return String.format(Locale.ROOT, "%.1f%s", rounded, suffix);
        }
    }
}
