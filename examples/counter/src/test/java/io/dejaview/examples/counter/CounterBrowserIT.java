package io.dejaview.examples.counter;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Browser end-to-end test for the compiled example.
 *
 * <p>Runs only with the {@code e2e} profile because it requires Playwright's
 * Chromium build. The test serves the TeaVM output and verifies rendering,
 * event handling and the resulting DOM update.</p>
 */
class CounterBrowserIT {

    @Test
    void rendersAndUpdatesTheCounter() throws IOException {
        Path webRoot = Path.of("target", "webapp").toAbsolutePath();
        assertTrue(Files.isRegularFile(webRoot.resolve("index.html")), "index.html is missing");
        assertTrue(Files.isRegularFile(webRoot.resolve("app.js")), "app.js is missing");

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> serve(exchange, webRoot));
        server.start();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try {
                Page page = browser.newPage();
                List<String> pageErrors = new CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                page.navigate("http://127.0.0.1:" + server.getAddress().getPort() + "/");

                assertEquals("DejaView", page.textContent("h1"));
                assertEquals("Count: 0", page.textContent("#count"));
                assertEquals("0", page.getAttribute("#count", "data-count"));

                page.click("button");
                assertEquals("Count: 1", page.textContent("#count"));
                assertEquals("1", page.getAttribute("#count", "data-count"));

                page.click("button");
                assertEquals("Count: 2", page.textContent("#count"));
                assertEquals("2", page.getAttribute("#count", "data-count"));

                assertTrue(pageErrors.isEmpty(), () -> "Browser errors: " + pageErrors);
            } finally {
                browser.close();
            }
        } finally {
            server.stop(0);
        }
    }

    private static void serve(HttpExchange exchange, Path webRoot) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath.equals("/")) {
            requestPath = "/index.html";
        }

        Path file = webRoot.resolve(requestPath.substring(1)).normalize();
        if (!file.startsWith(webRoot) || !Files.isRegularFile(file)) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        byte[] body = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", contentType(file));
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }
        if (name.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (name.endsWith(".map")) {
            return "application/json; charset=utf-8";
        }
        return "application/octet-stream";
    }
}
