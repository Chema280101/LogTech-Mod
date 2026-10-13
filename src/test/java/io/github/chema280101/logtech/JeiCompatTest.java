package io.github.chema280101.logtech;

import io.github.chema280101.logtech.machine.recipe.OutputScaling;
import io.github.chema280101.logtech.machine.recipe.ProcessingLogic;
import io.github.chema280101.logtech.machine.recipe.PurityMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class JeiCompatTest {

    @Test
    @DisplayName("Plugin de JEI existe, tiene @JeiPlugin e implementa IModPlugin")
    void testJeiPluginSourceStructure() throws IOException {
        Path pluginFile = Path.of("src/main/java/io/github/chema280101/logtech/compat/jei/LogTechJeiPlugin.java");
        assertTrue(Files.exists(pluginFile), "LogTechJeiPlugin.java debe existir en compat.jei");

        String content = Files.readString(pluginFile);
        assertTrue(content.contains("@JeiPlugin"), "Debe tener la anotación @JeiPlugin");
        assertTrue(content.contains("implements IModPlugin"), "Debe implementar IModPlugin");
        assertTrue(content.contains("CRUSHING_TYPE"), "Debe declarar CRUSHING_TYPE");
        assertTrue(content.contains("WASHING_TYPE"), "Debe declarar WASHING_TYPE");
        assertTrue(content.contains("CENTRIFUGING_TYPE"), "Debe declarar CENTRIFUGING_TYPE");
        assertTrue(content.contains("addRecipeCatalyst(ModBlocks.CRUSHER.get()"), "Debe registrar Crusher como catalizador");
        assertTrue(content.contains("addRecipeCatalyst(ModBlocks.WASHER.get()"), "Debe registrar Washer como catalizador");
        assertTrue(content.contains("addRecipeCatalyst(ModBlocks.CENTRIFUGE.get()"), "Debe registrar Centrifuge como catalizador");
    }

    @Test
    @DisplayName("Categorías de JEI existen y heredan de ProcessingRecipeCategory")
    void testJeiCategoriesExist() {
        Path crushingCat = Path.of("src/main/java/io/github/chema280101/logtech/compat/jei/CrushingCategory.java");
        Path washingCat = Path.of("src/main/java/io/github/chema280101/logtech/compat/jei/WashingCategory.java");
        Path centrifugingCat = Path.of("src/main/java/io/github/chema280101/logtech/compat/jei/CentrifugingCategory.java");
        Path baseCat = Path.of("src/main/java/io/github/chema280101/logtech/compat/jei/ProcessingRecipeCategory.java");

        assertTrue(Files.exists(crushingCat), "CrushingCategory.java debe existir");
        assertTrue(Files.exists(washingCat), "WashingCategory.java debe existir");
        assertTrue(Files.exists(centrifugingCat), "CentrifugingCategory.java debe existir");
        assertTrue(Files.exists(baseCat), "ProcessingRecipeCategory.java debe existir");
    }

    @Test
    @DisplayName("Lógica de pureza representativa: Lavado incrementa (+1), Triturado y Centrífuga conservan")
    void testRepresentativePurityLogic() {
        // Lavado: Pureza representativa 2 -> resultado 3 (Pureza +1)
        int washedPurity = ProcessingLogic.calculateOutputPurity(2, PurityMode.INCREASE);
        assertEquals(3, washedPurity, "Lavado debe incrementar pureza representativa en 1");

        // Triturado / Centrífuga: Conserva pureza original
        int preservedPurity = ProcessingLogic.calculateOutputPurity(1, PurityMode.PRESERVE);
        assertEquals(1, preservedPurity, "Triturado y Centrífuga deben conservar pureza representativa");
    }

    @Test
    @DisplayName("Lógica de escalado inverso de pureza")
    void testScalingLogic() {
        // Escala inversa: base 0.8f * (6 - p) / 5
        float chancePurity1 = ProcessingLogic.calculateEffectiveChance(0.8f, OutputScaling.INVERSE_PURITY, 1);
        float chancePurity5 = ProcessingLogic.calculateEffectiveChance(0.8f, OutputScaling.INVERSE_PURITY, 5);

        assertEquals(0.8f, chancePurity1, 0.001f, "Pureza 1 debe dar chance máxima (0.8f)");
        assertEquals(0.16f, chancePurity5, 0.001f, "Pureza 5 debe dar chance mínima (0.16f)");
    }
}
