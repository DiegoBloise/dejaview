package io.dejaview.cli;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * End-to-end workflow test: generate a project with the CLI, build it with the
 * generated Maven wrapper, serve it through the development server, exercise it
 * in a real browser and verify that a source change triggers a rebuild and a
 * live reload.
 *
 * <p>Runs only with the {@code e2e} profile. Requires the DejaView modules to be
 * installed in the local Maven repository; {@link #ensureDejaViewInstalled()}
 * installs them from the repository root when missing.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CliWorkflowIT {

    private static final String APP_NAME = "it-app";

    private static Path workDir;
    private static Path projectDir;
    private static DevServer server;
    private static BuildWatcher watcher;
    private static int port;

    @BeforeAll
    static void setUp() throws Exception {
        ensureDejaViewInstalled();
        workDir = Files.createTempDirectory("dejaview-cli-it");
        projectDir = workDir.resolve(APP_NAME);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        ProjectGenerator.generate(new Context(workDir, stream, stream), APP_NAME, null, null, projectDir);

        ProcessRunner.Result build = ProcessRunner.build(projectDir, List.of("package"), null);
        if (!build.succeeded()) {
            fail("Generated project failed to build:\n" + String.join(System.lineSeparator(), build.outputTail()));
        }

        server = DevServer.create("127.0.0.1", 0, projectDir.resolve("target").resolve("webapp"));
        server.start();
        port = server.port();
        watcher = new BuildWatcher(projectDir, () -> {
            ProcessRunner.Result rebuild = ProcessRunner.build(projectDir, List.of("package"), null);
            if (rebuild.succeeded()) {
                server.bumpVersion();
            }
        }, stream);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (watcher != null) {
            watcher.close();
        }
        if (server != null) {
            server.close();
        }
        if (workDir != null) {
            Files.walkFileTree(workDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    @Test
    @Order(1)
    void generates_a_valid_project_layout() {
        assertTrue(Files.isRegularFile(projectDir.resolve("pom.xml")));
        assertTrue(Files.isRegularFile(projectDir.resolve("mvnw")));
        assertTrue(Files.isRegularFile(projectDir.resolve("dejaview.properties")));
        assertTrue(Files.isRegularFile(projectDir.resolve("src/main/webapp/index.html")));
        try (var stream = Files.walk(projectDir.resolve("src/main/java"))) {
            assertTrue(stream.anyMatch(path -> path.toString().endsWith("App.java")),
                    "generated project should contain App.java");
        } catch (IOException e) {
            fail(e::getMessage);
        }
    }

    @Test
    @Order(2)
    void generated_project_produces_the_tea_vm_artifacts() {
        assertTrue(Files.isRegularFile(projectDir.resolve("target/webapp/index.html")));
        assertTrue(Files.isRegularFile(projectDir.resolve("target/webapp/app.js")));
    }

    @Test
    @Order(3)
    void dev_server_serves_the_app_and_the_live_reload_client() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String base = "http://127.0.0.1:" + port;

        HttpResponse<String> index = client.send(HttpRequest.newBuilder(URI.create(base + "/")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, index.statusCode());
        assertTrue(index.body().contains("<main id=\"app\">"));
        assertTrue(index.body().contains("/__dejaview__/live.js"));

        HttpResponse<String> version = client.send(HttpRequest.newBuilder(URI.create(base + "/__dejaview__/version")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, version.statusCode());

        HttpResponse<String> appJs = client.send(HttpRequest.newBuilder(URI.create(base + "/app.js")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, appJs.statusCode());
        assertTrue(appJs.headers().firstValue("Content-Type").orElse("").contains("application/javascript"));
    }

    @Test
    @Order(4)
    void browser_runs_the_generated_app() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            try {
                Page page = browser.newPage();
                List<String> pageErrors = new java.util.concurrent.CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                page.navigate("http://127.0.0.1:" + port + "/");
                page.waitForSelector("#count");

                assertEquals("Count: 0", page.textContent("#count"));
                page.click("button");
                assertEquals("Count: 1", page.textContent("#count"));

                assertTrue(pageErrors.isEmpty(), () -> "Browser errors: " + pageErrors);
            } finally {
                browser.close();
            }
        }
    }

    @Test
    @Order(5)
    void source_changes_trigger_a_rebuild_and_a_browser_reload() throws Exception {
        // A fresh page, then an edit the watcher sees; the dev server rebuilds
        // the project and the live-reload client reloads the page.
        Path component = projectDir.resolve("src/main/java/com/example/itapp/CounterComponent.java");
        assertTrue(Files.isRegularFile(component), () -> "template layout changed unexpectedly: " + component);
        String original = Files.readString(component);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            try {
                Page page = browser.newPage();
                page.navigate("http://127.0.0.1:" + port + "/");
                page.waitForSelector("#count");
                page.click("button");
                assertEquals("Count: 1", page.textContent("#count"));

                Files.writeString(component, original + System.lineSeparator()
                        + "// CliWorkflowIT: trigger a rebuild" + System.lineSeparator());

                page.waitForFunction(
                        "() => document.querySelector('#count') && document.querySelector('#count').textContent.includes('Count: 0')",
                        null,
                        new Page.WaitForFunctionOptions().setTimeout(120_000));
                assertEquals(1, server.version(), "the dev server should have bumped the build version exactly once");
            } finally {
                browser.close();
            }
        }
    }

    @Test
    @Order(6)
    void package_writes_an_asset_manifest_for_the_generated_app() throws Exception {
        Path manifest = projectDir.resolve("target/webapp/dejaview-assets.json");
        assertTrue(Files.isRegularFile(manifest),
                () -> "the generated build should write dejaview-assets.json during package");
        String json = Files.readString(manifest, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"path\": \"app.js\""), json);
        assertTrue(json.contains("\"path\": \"index.html\""), json);
        assertTrue(json.contains("\"sha256\": \""), json);
        assertTrue(json.contains("\"totalBytes\": "), json);
    }

    private static void ensureDejaViewInstalled() throws IOException, InterruptedException {
        Path repository = Path.of(System.getProperty("user.home"), ".m2", "repository", "io", "dejaview");
        boolean compilerInstalled = Files.isRegularFile(repository
                .resolve("dejaview-compiler").resolve(Version.current())
                .resolve("dejaview-compiler-" + Version.current() + ".jar"));
        boolean coreInstalled = Files.isRegularFile(repository
                .resolve("dejaview-core").resolve(Version.current())
                .resolve("dejaview-core-" + Version.current() + ".jar"));
        if (compilerInstalled && coreInstalled) {
            return;
        }
        Path repoRoot = Path.of("").toAbsolutePath().normalize().getParent();
        assertNotNull(repoRoot, "could not resolve the repository root");
        Path wrapper = repoRoot.resolve("mvnw");
        // Install only what the generated application needs. Building the whole
        // reactor (or this module) would rewrite this module's target/test-classes
        // while the forked failsafe JVM still has it on the classpath.
        ProcessBuilder builder = new ProcessBuilder(wrapper.toString(), "-q", "install", "-Dmaven.test.skip=true",
                "-pl", "dejaview-reactivity,dejaview-dom,dejaview-compiler", "-am");
        builder.directory(repoRoot.toFile());
        builder.inheritIO();
        int exit = builder.start().waitFor();
        assertEquals(0, exit, "failed to install the DejaView modules into the local repository");
    }
}
