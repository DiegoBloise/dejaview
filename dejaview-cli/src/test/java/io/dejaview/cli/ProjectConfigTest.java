package io.dejaview.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void defaults_when_file_is_missing() throws Exception {
        ProjectConfig config = ProjectConfig.load(tempDir);
        assertEquals(ProjectConfig.DEFAULT_PORT, config.port());
        assertEquals(ProjectConfig.DEFAULT_HOST, config.host());
        assertEquals(Path.of(ProjectConfig.DEFAULT_WEBAPP_DIR), config.webappDir());
    }

    @Test
    void parses_custom_values_and_ignores_comments() throws Exception {
        Files.writeString(tempDir.resolve("dejaview.properties"), """
                # development server settings

                port=5000
                host=0.0.0.0
                webappDir=public
                """);
        ProjectConfig config = ProjectConfig.load(tempDir);
        assertEquals(5000, config.port());
        assertEquals("0.0.0.0", config.host());
        assertEquals(Path.of("public"), config.webappDir());
    }

    @Test
    void rejects_invalid_ports() throws Exception {
        for (String port : List.of("abc", "0", "65536", "-1")) {
            Files.writeString(tempDir.resolve("dejaview.properties"), "port=" + port + System.lineSeparator());
            assertThrows(CliException.class, () -> ProjectConfig.load(tempDir), "port=" + port);
        }
    }

    @Test
    void rejects_unknown_keys() throws Exception {
        Files.writeString(tempDir.resolve("dejaview.properties"), "colour=blue\n");
        CliException error = assertThrows(CliException.class, () -> ProjectConfig.load(tempDir));
        assertTrue(error.getMessage().contains("colour"));
    }

    @Test
    void rejects_lines_without_a_value_separator() throws Exception {
        Files.writeString(tempDir.resolve("dejaview.properties"), "port 5000\n");
        CliException error = assertThrows(CliException.class, () -> ProjectConfig.load(tempDir));
        assertTrue(error.getMessage().contains("line 1"));
    }
}
