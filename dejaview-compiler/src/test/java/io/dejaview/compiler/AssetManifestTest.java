package io.dejaview.compiler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetManifestTest {

    @TempDir
    Path tempDir;

    @Test
    void entries_are_sorted_by_relative_path() throws Exception {
        Files.createDirectories(tempDir.resolve("assets"));
        Files.writeString(tempDir.resolve("app.js"), "js");
        Files.writeString(tempDir.resolve("assets/z.css"), "css");
        Files.writeString(tempDir.resolve("index.html"), "html");

        AssetManifest manifest = AssetManifest.generate(tempDir);

        assertEquals(java.util.List.of("app.js", "assets/z.css", "index.html"),
                manifest.entries().stream().map(AssetManifest.AssetEntry::path).toList());
    }

    @Test
    void hashes_match_the_file_contents() throws Exception {
        Files.writeString(tempDir.resolve("app.js"), "console.log(1);");

        AssetManifest manifest = AssetManifest.generate(tempDir);

        assertEquals(Hashing.sha256Hex(tempDir.resolve("app.js")), manifest.entries().get(0).sha256());
        assertEquals(Files.size(tempDir.resolve("app.js")), manifest.entries().get(0).bytes());
    }

    @Test
    void total_bytes_sums_all_entries() throws Exception {
        Files.writeString(tempDir.resolve("a.txt"), "12345");
        Files.writeString(tempDir.resolve("b.txt"), "67");

        assertEquals(7, AssetManifest.generate(tempDir).totalBytes());
    }

    @Test
    void manifest_excludes_itself() throws Exception {
        Files.writeString(tempDir.resolve("app.js"), "js");
        Files.writeString(tempDir.resolve(AssetManifest.MANIFEST_NAME), "stale");

        AssetManifest manifest = AssetManifest.generate(tempDir);

        assertEquals(1, manifest.entries().size());
        assertEquals("app.js", manifest.entries().get(0).path());
    }

    @Test
    void json_output_is_stable_and_contains_entries() throws Exception {
        Files.writeString(tempDir.resolve("app.js"), "js");

        AssetManifest manifest = AssetManifest.generate(tempDir);
        String json = manifest.toJson();

        assertTrue(json.contains("\"version\": 1"));
        assertTrue(json.contains("\"path\": \"app.js\""));
        assertTrue(json.contains("\"totalBytes\": 2"));
        // Deterministic across calls
        assertEquals(json, AssetManifest.generate(tempDir).toJson());
    }

    @Test
    void path_escaping_is_valid_json() throws Exception {
        Path weird = tempDir.resolve("quote\"name.js");
        Files.writeString(weird, "x");

        String json = AssetManifest.generate(tempDir).toJson();

        assertTrue(json.contains("quote\\\"name.js"));
        assertFalse(json.contains("quote\"name.js"));
    }

    @Test
    void write_creates_the_manifest_file() throws Exception {
        Files.writeString(tempDir.resolve("app.js"), "js");
        Path target = tempDir.resolve(AssetManifest.MANIFEST_NAME);

        AssetManifest.generate(tempDir).write(target);

        assertTrue(Files.readString(target).contains("\"entries\""));
    }
}
