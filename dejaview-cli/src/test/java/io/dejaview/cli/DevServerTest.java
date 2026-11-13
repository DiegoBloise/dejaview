package io.dejaview.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevServerTest {

    @TempDir
    Path tempDir;

    private DevServer server;
    private HttpClient client;
    private String base;

    @BeforeEach
    void startServer() throws Exception {
        Files.createDirectories(tempDir.resolve("assets"));
        write("index.html", "<!doctype html><html><body><main id=\"app\">hola</main></body></html>");
        write("app.js", "console.log('app');");
        write("assets/data.json", "{\"ok\":true}");

        server = DevServer.create("127.0.0.1", 0, tempDir);
        server.start();
        base = "http://127.0.0.1:" + server.port();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.close();
        }
    }

    private void write(String rel, String content) throws Exception {
        Path path = tempDir.resolve(rel);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void serves_index_with_the_live_reload_client_injected() throws Exception {
        HttpResponse<String> response = get("/");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("/__dejaview__/live.js"),
                "index.html responses must inject the live-reload client");
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElse(""));
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"));
    }

    @Test
    void does_not_inject_the_client_into_javascript() throws Exception {
        HttpResponse<String> response = get("/app.js");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("console.log('app');"));
        assertTrue(!response.body().contains("/__dejaview__/live.js"));
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/javascript"));
    }

    @Test
    void serves_static_assets_with_content_types() throws Exception {
        HttpResponse<String> response = get("/assets/data.json");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
    }

    @Test
    void appends_live_reload_when_the_page_has_no_body_tag() throws Exception {
        write("fragment.html", "<main>fragment</main>");
        HttpResponse<String> response = get("/fragment.html");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().endsWith("<script src=\"/__dejaview__/live.js\" defer></script>"));
    }

    @Test
    void returns_404_for_missing_files() throws Exception {
        assertEquals(404, get("/nope.js").statusCode());
    }

    @Test
    void rejects_path_traversal() throws Exception {
        HttpResponse<String> response = get("/%2e%2e/secret.txt");
        assertEquals(403, response.statusCode());
    }

    @Test
    void exposes_the_build_version_and_bumps_it() throws Exception {
        HttpResponse<String> before = get("/__dejaview__/version");
        assertEquals("0", before.body().trim());

        server.bumpVersion();

        HttpResponse<String> after = get("/__dejaview__/version");
        assertEquals("1", after.body().trim());
        assertEquals("no-store", after.headers().firstValue("Cache-Control").orElse(""));
    }

    @Test
    void serves_the_live_reload_client() throws Exception {
        HttpResponse<String> response = get("/__dejaview__/live.js");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("/__dejaview__/version"));
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("javascript"));
    }
}
