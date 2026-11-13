package io.dejaview.compiler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerifyWebAppTest {

    @TempDir
    Path tempDir;

    private Path write(String rel, String content) throws IOException {
        Path file = tempDir.resolve(rel);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    private Path validWebApp() throws IOException {
        write("index.html", "<!doctype html><script src=\"app.js\"></script>");
        write("app.js", "console.log('ready');");
        write("app.js.map", "{}");
        return tempDir;
    }

    @Test
    void valid_webapp_writes_the_manifest_and_does_not_throw() throws Exception {
        Path webapp = validWebApp();

        VerifyWebApp.main(new String[]{"--webapp", webapp.toString()});

        Path manifest = webapp.resolve(VerifyWebApp.MANIFEST_NAME);
        assertTrue(Files.isRegularFile(manifest));
        String json = Files.readString(manifest);
        assertTrue(json.contains("\"path\": \"index.html\""));
        assertTrue(json.contains("\"path\": \"app.js\""));
        assertTrue(json.contains("\"sha256\""));
    }

    @Test
    void invalid_webapp_fails_the_build_with_the_report() throws Exception {
        write("index.html", "<script src=\"app.js\"></script>");
        // app.js deliberately missing

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> VerifyWebApp.main(new String[]{"--webapp", tempDir.toString()}));

        assertTrue(failure.getMessage().contains("webapp verification failed"));
        assertTrue(failure.getMessage().contains("app.js is missing"));
    }

    @Test
    void missing_option_fails_with_usage() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> VerifyWebApp.main(new String[]{"--budget-bytes", "10"}));

        assertTrue(failure.getMessage().contains("Usage:"));
    }

    @Test
    void unknown_option_fails_with_usage() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> VerifyWebApp.main(new String[]{"--webapp", tempDir.toString(), "--bogus"}));

        assertTrue(failure.getMessage().contains("Unknown option: --bogus"));
    }

    @Test
    void no_index_mode_validates_a_bundle_only_output() throws Exception {
        write("app.js", "console.log('ready');");
        write("app.js.map", "{}");
        // no index.html on purpose — the host page lives elsewhere (e.g. the website shells)

        VerifyWebApp.main(new String[]{"--webapp", tempDir.toString(), "--no-index"});

        assertTrue(Files.isRegularFile(tempDir.resolve(VerifyWebApp.MANIFEST_NAME)));
    }

    @Test
    void no_index_mode_still_fails_when_the_bundle_is_missing() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> VerifyWebApp.main(new String[]{"--webapp", tempDir.toString(), "--no-index"}));

        assertTrue(failure.getMessage().contains("app.js is missing"));
    }

    @Test
    void manifest_hashes_match_the_files_on_disk() throws Exception {
        Path webapp = validWebApp();

        VerifyWebApp.main(new String[]{"--webapp", webapp.toString()});

        String json = Files.readString(webapp.resolve(VerifyWebApp.MANIFEST_NAME));
        for (Path file : Files.walk(webapp).filter(Files::isRegularFile).toList()) {
            if (file.getFileName().toString().equals(VerifyWebApp.MANIFEST_NAME)) {
                continue; // the manifest does not list itself
            }
            String relative = webapp.relativize(file).toString().replace('\\', '/');
            assertTrue(json.contains("\"path\": \"" + relative + "\""), "manifest misses " + relative);
            assertTrue(json.contains(Hashing.sha256Hex(file)), "manifest hash mismatch for " + relative);
        }
    }
}
