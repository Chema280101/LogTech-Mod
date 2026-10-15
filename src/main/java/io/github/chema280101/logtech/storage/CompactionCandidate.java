package io.github.chema280101.logtech.storage;

import java.util.List;
import java.util.Objects;

/**
 * Descriptor puro en Java de un candidato a receta de crafteo para análisis de reversibilidad.
 *
 * @param <K> tipo de clave del ítem
 */
public record CompactionCandidate<K>(
        String recipeId,
        List<K> ingredients,
        boolean isGrid,
        int gridWidth,
        int gridHeight,
        boolean hasRemainder,
        K resultItem,
        int resultCount
) {
    public CompactionCandidate {
        Objects.requireNonNull(ingredients, "ingredients no puede ser nulo");
    }

    public static <K> CompactionCandidate<K> shaped(
            String recipeId,
            int width,
            int height,
            List<K> ingredients,
            boolean hasRemainder,
            K resultItem,
            int resultCount
    ) {
        return new CompactionCandidate<>(recipeId, ingredients, true, width, height, hasRemainder, resultItem, resultCount);
    }

    public static <K> CompactionCandidate<K> shapeless(
            String recipeId,
            List<K> ingredients,
            boolean hasRemainder,
            K resultItem,
            int resultCount
    ) {
        return new CompactionCandidate<>(recipeId, ingredients, false, ingredients.size(), 1, hasRemainder, resultItem, resultCount);
    }
}
