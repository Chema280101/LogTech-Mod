package io.github.chema280101.logtech;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompatIsolationTest {

    @Test
    void testNoThirdPartyCompatImportsInCoreCode() throws IOException {
        Path mainJavaPath = Path.of("src/main/java");
        assertTrue(Files.exists(mainJavaPath), "src/main/java must exist");

        List<String> forbiddenPrefixes = List.of(
                "import snownee.jade",
                "import mezz.jei",
                "import dev.emi"
        );

        try (Stream<Path> paths = Files.walk(mainJavaPath)) {
            List<Path> javaFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("compat" + File.separator))
                    .filter(p -> !p.toString().contains("compat/"))
                    .toList();

            for (Path file : javaFiles) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    for (String forbidden : forbiddenPrefixes) {
                        if (line.startsWith(forbidden)) {
                            throw new AssertionError(
                                    "Forbidden import '" + forbidden + "' found at " + file + ":" + (i + 1)
                                            + ". Third-party integrations must be isolated in compat packages!"
                            );
                        }
                    }
                }
            }
        }
    }
}
