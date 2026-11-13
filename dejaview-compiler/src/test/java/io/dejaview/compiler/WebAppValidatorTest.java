package io.dejaview.compiler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebAppValidatorTest {

    @TempDir
    Path tempDir;

    private Path write(String rel, String content) throws IOException {
        Path file = tempDir.resolve(rel);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    private Path validWebApp() throws IOException {
        write("index.html", """
                <!doctype html>
                <html><head><link rel="stylesheet" href="style.css"></head>
                <body><main id="app"></main><script src="app.js"></script></body></html>
                """);
        write("style.css", "body {}");
        write("app.js", "console.log('ready');");
        write("app.js.map", "{}");
        return tempDir;
    }

    @Test
    void a_valid_webapp_passes() throws Exception {
        validWebApp();

        ValidationReport report = WebAppValidator.validate(tempDir);

        assertFalse(report.hasErrors(), report.render());
        assertEquals(0, report.errorCount());
    }

    @Test
    void missing_index_is_an_error() throws Exception {
        write("app.js", "console.log('ready');");

        ValidationReport report = WebAppValidator.validate(tempDir);

        assertTrue(report.hasErrors());
        assertTrue(report.render().contains("index.html is missing"));
    }

    @Test
    void empty_bundle_is_an_error() throws Exception {
        write("index.html", "<script src=\"app.js\"></script>");
        write("app.js", "");

        ValidationReport report = WebAppValidator.validate(tempDir);

        assertTrue(report.hasErrors());
        assertTrue(report.render().contains("app.js is empty"));
    }

    @Test
    void missing_referenced_asset_is_reported_with_path() throws Exception {
        validWebApp();
        Files.delete(tempDir.resolve("style.css"));

        ValidationReport report = WebAppValidator.validate(tempDir);

        assertTrue(report.hasErrors());
        assertTrue(report.render().contains("missing asset: style.css"), report.render());
    }

    @Test
    void reference_escaping_the_web_root_is_an_error() throws Exception {
        write("index.html", "<script src=\"../secret.js\"></script>");
        write("app.js", "console.log('ready');");

        ValidationReport report = WebAppValidator.validate(tempDir);

        assertTrue(report.hasErrors());
        assertTrue(report.render().contains("escapes the web root"), report.render());
    }

    @Test
    void missing_source_map_is_a_warning_not_an_error() throws Exception {
        write("index.html", "<script src=\"app.js\"></script>");
        write("app.js", "console.log('ready');");

        ValidationReport report = WebAppValidator.validate(tempDir);

        assertFalse(report.hasErrors());
        assertEquals(1, report.warningCount());
        assertTrue(report.render().contains("source map"));
    }

    @Test
    void exceeding_the_size_budget_is_a_warning() throws Exception {
        validWebApp();
        Path appJs = tempDir.resolve("app.js");
        Files.writeString(appJs, "x".repeat(2048));

        ValidationReport report = WebAppValidator.validate(tempDir,
                new WebAppValidator.ValidatorOptions("app.js", 1024, true));

        assertFalse(report.hasErrors());
        assertTrue(report.render().contains("budget"), report.render());
    }

    @Test
    void require_index_false_allows_bundle_only_output() throws Exception {
        write("app.js", "console.log('ready');");
        write("app.js.map", "{}");

        ValidationReport report = WebAppValidator.validate(tempDir,
                new WebAppValidator.ValidatorOptions("app.js", 512L * 1024, true, false));

        assertFalse(report.hasErrors(), report.render());
    }

    @Test
    void root_relative_reference_is_a_warning() throws Exception {
        write("index.html", "<script src=\"/app.js\"></script><script src=\"app.js\"></script>");
        write("app.js", "console.log('ready');");
        write("app.js.map", "{}");

        ValidationReport report = WebAppValidator.validate(tempDir);

        assertFalse(report.hasErrors(), report.render());
        assertTrue(report.render().contains("root-relative"));
    }

    @Test
    void missing_webapp_directory_is_an_error() {
        ValidationReport report = WebAppValidator.validate(tempDir.resolve("nope"));

        assertTrue(report.hasErrors());
    }

    @Test
    void external_and_data_references_are_ignored() throws Exception {
        write("index.html", """
                <link rel="stylesheet" href="https://cdn.example.com/app.css">
                <link rel="icon" href="data:image/png;base64,AAAA">
                <script src="//cdn.example.com/app.js"></script>
                <script src="app.js"></script>
                """);
        write("app.js", "console.log('ready');");
        write("app.js.map", "{}");

        ValidationReport report = WebAppValidator.validate(tempDir);

        assertFalse(report.hasErrors(), report.render());
    }

    @Test
    void query_strings_and_fragments_are_ignored_for_existence_checks() throws Exception {
        write("index.html", "<script src=\"app.js?v=123#bundle\"></script>");
        write("app.js", "console.log('ready');");
        write("app.js.map", "{}");

        ValidationReport report = WebAppValidator.validate(tempDir);

        assertFalse(report.hasErrors(), report.render());
    }

    @Test
    void report_render_lists_findings_one_per_line() throws Exception {
        write("index.html", "<script src=\"app.js\"></script>");
        // app.js missing on purpose

        ValidationReport report = WebAppValidator.validate(tempDir);

        String render = report.render();
        assertTrue(render.startsWith("DejaView webapp validation:"));
        assertTrue(render.contains("ERROR  "));
    }
}
