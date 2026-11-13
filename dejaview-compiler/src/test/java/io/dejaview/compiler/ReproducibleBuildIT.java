package io.dejaview.compiler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance criterion for Sprint 15: the same source under equivalent build
 * conditions must produce byte-identical JavaScript. Runs two consecutive
 * {@code clean package} builds of the counter example and compares the bundle
 * and the asset manifest.
 *
 * <p>Opt-in end-to-end test ({@code ./mvnw -Pe2e -pl dejaview-compiler -am verify}),
 * because it shells out to two nested Maven builds of an example application.</p>
 */
class ReproducibleBuildIT {

    private static Path repoRoot;

    @BeforeAll
    static void resolveRepositoryRoot() {
        repoRoot = Path.of("").toAbsolutePath().normalize().getParent();
        assertNotNull(repoRoot, "could not resolve the repository root");
        assertTrue(Files.isRegularFile(repoRoot.resolve("mvnw")),
                () -> "repository wrapper not found at " + repoRoot);
    }

    @Test
    void two_consecutive_builds_produce_byte_identical_artifacts() throws Exception {
        // Build #1: install (verify runs the webapp validation and writes the
        // manifest). Build #2: clean verify — same artifact set, same phases,
        // no reinstall; the local repository already holds the artifacts.
        runMaven(null, "-q", "-Dmaven.test.skip=true", "-pl", "examples/counter", "-am", "install");
        Snapshot first = snapshot();

        runMaven(null, "-q", "-Dmaven.test.skip=true", "-pl", "examples/counter", "clean", "verify");
        Snapshot second = snapshot();

        assertEquals(first, second,
                "equivalent builds must produce identical bundle and manifest bytes");
    }

    /** Directory-relative SHA-256 snapshot of every file the build leaves in the webapp. */
    private record Snapshot(String bundleSha, String manifestSha, String fileNames) {

        @Override
        public String toString() {
            return "app.js=" + bundleSha + " manifest=" + manifestSha + " files=" + fileNames;
        }
    }

    private static Snapshot snapshot() throws IOException {
        Path webapp = repoRoot.resolve("examples/counter/target/webapp");
        String bundleSha = Hashing.sha256Hex(webapp.resolve("app.js"));
        Path manifest = webapp.resolve(AssetManifest.MANIFEST_NAME);
        String manifestSha = Files.isRegularFile(manifest) ? Hashing.sha256Hex(manifest) : "none";
        String fileNames;
        try (var walk = Files.walk(webapp)) {
            fileNames = walk.filter(Files::isRegularFile)
                    .map(path -> webapp.relativize(path).toString().replace('\\', '/'))
                    .sorted()
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
        }
        return new Snapshot(bundleSha, manifestSha, fileNames);
    }

    private static void runMaven(Path cwd, String... command) throws IOException, InterruptedException {
        Path wrapper = repoRoot.resolve("mvnw");
        java.util.List<String> args = new java.util.ArrayList<>();
        args.add(wrapper.toString());
        args.addAll(java.util.Arrays.asList(command));
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.directory(cwd != null ? cwd.toFile() : repoRoot.toFile());
        builder.inheritIO();
        int exit = builder.start().waitFor();
        assertEquals(0, exit, () -> "nested Maven build failed: " + String.join(" ", args));
    }
}
