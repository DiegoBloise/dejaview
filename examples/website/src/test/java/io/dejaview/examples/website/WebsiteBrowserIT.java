package io.dejaview.examples.website;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
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
 * Browser end-to-end tests for the website shells and the compiled
 * DejaView bundle that renders them.
 *
 * <p>The site loads its assets via absolute paths from the repository root
 * ({@code /website/...}, {@code /examples/website/target/webapp/app.js}), so
 * this test serves the repository root on an ephemeral loopback port —
 * the same layout used when previewing the site with
 * {@code python3 -m http.server 4280}. Runs only with the {@code e2e} profile.</p>
 */
class WebsiteBrowserIT {

    @Test
    void englishHomeRendersAndSwitchesDemoTabs() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                List<String> pageErrors = new CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                page.navigate(server.url() + "/website/");

                assertTrue(page.textContent("#hero-title").contains("Reimagined"));
                assertEquals(9, page.locator("#features .card").count());

                assertEquals("http://www.w3.org/2000/svg",
                        page.evaluate("() => document.querySelector('.brand-mark').namespaceURI"));
                assertEquals("http://www.w3.org/2000/svg",
                        page.evaluate("() => document.querySelector('.card-icon svg').namespaceURI"));
                assertEquals("http://www.w3.org/2000/svg",
                        page.evaluate("() => document.querySelector('.card-icon svg path').namespaceURI"));

                String frame = page.getAttribute("#demo-frame", "src");
                assertTrue(frame.contains("example=counter"), frame);

                page.click("#tab-router");
                assertTrue(page.getAttribute("#demo-frame", "src").contains("example=router"));

                assertTrue(pageErrors.isEmpty(), () -> "Browser errors: " + pageErrors);
            } finally {
                browser.close();
            }
        }
    }

    @Test
    void portugueseHomeRendersWithLanguageSwitch() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                List<String> pageErrors = new CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                page.navigate(server.url() + "/website/pt/");

                assertTrue(page.textContent("#hero-title").contains("Reimaginado"));
                assertEquals("EN", page.textContent(".lang-switch"));
                assertTrue(page.locator("#features .card").count() == 9);

                assertTrue(pageErrors.isEmpty(), () -> "Browser errors: " + pageErrors);
            } finally {
                browser.close();
            }
        }
    }

    @Test
    void getStartedGuideRendersInBothLocales() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                List<String> pageErrors = new CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                page.navigate(server.url() + "/website/get-started.html");
                assertTrue(page.locator(".docs-toc-nav a").count() >= 15);
                assertTrue(page.locator(".docs-content h2").count() >= 15);

                page.navigate(server.url() + "/website/pt/get-started.html");
                assertTrue(page.locator(".docs-content h2").count() >= 15);
                assertTrue(page.textContent(".docs-content h2") != null);
                assertTrue(page.locator(".docs-toc-nav a").allTextContents().stream()
                        .anyMatch(text -> text.contains("Pré-requisitos")));

                assertTrue(pageErrors.isEmpty(), () -> "Browser errors: " + pageErrors);
            } finally {
                browser.close();
            }
        }
    }

    @Test
    void playgroundListsExamplesAndRunsThem() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                List<String> pageErrors = new CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                page.navigate(server.url() + "/website/playground/");
                assertEquals(8, page.locator(".example-item").count());

                page.click("#btn-run");
                page.waitForTimeout(500);
                assertTrue(page.getAttribute("#preview-iframe", "src").contains("example=counter"));

                page.click(".example-item[data-example='list']");
                assertTrue(page.locator("#highlight-code").textContent().contains("KeyedListDemo")
                        || page.locator("#highlight-code").textContent().contains("order"));

                page.navigate(server.url() + "/website/pt/playground/");
                assertTrue(page.locator(".example-item-name").allTextContents()
                        .contains("Contador"));

                assertTrue(pageErrors.isEmpty(), () -> "Browser errors: " + pageErrors);
            } finally {
                browser.close();
            }
        }
    }

    /** Tiny static file server bound to an ephemeral loopback port. */
    private static final class Server implements AutoCloseable {
        private final HttpServer server;

        private Server(HttpServer server) {
            this.server = server;
        }

        static Server create() throws IOException {
            Path webRoot = Path.of("").toAbsolutePath().getParent().getParent();
            assertTrue(Files.isRegularFile(webRoot.resolve("website/index.html")),
                    "website shells are missing; the test serves the repository root");
            assertTrue(Files.isRegularFile(webRoot.resolve("examples/website/target/webapp/app.js")),
                    "app.js is missing; run `./mvnw -pl examples/website package` first");

            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> serve(exchange, webRoot));
            server.start();
            return new Server(server);
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }

        private static void serve(HttpExchange exchange, Path webRoot) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath.endsWith("/")) {
                requestPath = requestPath + "index.html";
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
            if (name.endsWith(".css")) {
                return "text/css; charset=utf-8";
            }
            if (name.endsWith(".html")) {
                return "text/html; charset=utf-8";
            }
            if (name.endsWith(".svg")) {
                return "image/svg+xml";
            }
            if (name.endsWith(".map") || name.endsWith(".json")) {
                return "application/json; charset=utf-8";
            }
            return "application/octet-stream";
        }
    }
}
