package io.github.chema280101.logtech.storage;

import java.util.Objects;

/**
 * Modelo puro de una vista guardada del Terminal.
 * Solo guarda parámetros de filtrado y orden; nunca listas de ítems.
 * Clase Java pura desacoplada de Minecraft.
 *
 * @param name        nombre de la vista (1–20 caracteres, ya saneado)
 * @param searchText  texto de búsqueda libre (max 50 chars)
 * @param sortMode    0 = por cantidad, 1 = por nombre
 * @param tierFilter  0 = todos, 1 = solo caliente, 2 = solo frío
 * @param minPurity   pureza mínima 0–5 (0 = sin filtro)
 */
public record SavedView(String name, String searchText, int sortMode, int tierFilter, int minPurity) {

    public static final int SORT_AMOUNT = 0;
    public static final int SORT_NAME   = 1;

    public static final int TIER_ALL  = 0;
    public static final int TIER_HOT  = 1;
    public static final int TIER_COLD = 2;

    public static final int MAX_NAME_LENGTH   = 20;
    public static final int MAX_SEARCH_LENGTH = 50;

    /** Construye una vista por defecto para el slot dado (1-indexed). */
    public static SavedView defaultFor(int slot) {
        return new SavedView("Vista " + slot, "", SORT_AMOUNT, TIER_ALL, 0);
    }

    /**
     * Sanea el nombre: elimina caracteres de control, corta a MAX_NAME_LENGTH y aplica trim.
     * Devuelve null si el nombre queda vacío tras sanear.
     */
    public static String sanitizeName(String raw) {
        if (raw == null) return null;
        String sanitized = raw.replaceAll("[\\p{Cc}\\p{Cf}]", "").trim();
        if (sanitized.isEmpty()) return null;
        if (sanitized.length() > MAX_NAME_LENGTH) {
            sanitized = sanitized.substring(0, MAX_NAME_LENGTH).trim();
        }
        return sanitized.isEmpty() ? null : sanitized;
    }

    /** Sanea el texto de búsqueda: elimina chars de control y corta a MAX_SEARCH_LENGTH. */
    public static String sanitizeSearch(String raw) {
        if (raw == null) return "";
        String sanitized = raw.replaceAll("[\\p{Cc}\\p{Cf}]", "");
        return sanitized.length() > MAX_SEARCH_LENGTH
                ? sanitized.substring(0, MAX_SEARCH_LENGTH)
                : sanitized;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SavedView v)) return false;
        return sortMode == v.sortMode && tierFilter == v.tierFilter && minPurity == v.minPurity
                && Objects.equals(name, v.name) && Objects.equals(searchText, v.searchText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, searchText, sortMode, tierFilter, minPurity);
    }
}
