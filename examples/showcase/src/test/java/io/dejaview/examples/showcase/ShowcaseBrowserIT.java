package io.dejaview.examples.showcase;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Browser end-to-end tests for the showcase examples.
 *
 * <p>Each test serves the compiled TeaVM {@code target/webapp} output over
 * HTTP and drives it with Playwright's Chromium, exercising the same
 * JavaScript bundle that the website playground loads. Runs only with the
 * {@code e2e} profile.</p>
 */
class ShowcaseBrowserIT {

    @Test
    void counterExampleIncrements() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                List<String> pageErrors = new CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                page.navigate(server.url() + "/index.html?example=counter");

                assertEquals("Counter", page.textContent("h1"));
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
    void greetingExampleUpdatesGreeting() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                List<String> pageErrors = new CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                page.navigate(server.url() + "/index.html?example=greeting");

                assertEquals("Hello, ", page.textContent("h1"));

                page.click("button");
                assertEquals("Hello, DejaView", page.textContent("h1"));

                assertTrue(pageErrors.isEmpty(), () -> "Browser errors: " + pageErrors);
            } finally {
                browser.close();
            }
        }
    }

    @Test
    void compositionExampleKeepsIndependentState() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                List<String> pageErrors = new CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                page.navigate(server.url() + "/index.html?example=composition");

                Locator buttons = page.locator("button");
                assertEquals(2, buttons.count());
                assertEquals("Clicks: 0", buttons.nth(0).textContent());
                assertEquals("Clicks: 0", buttons.nth(1).textContent());

                buttons.nth(0).click();
                assertEquals("Clicks: 1", buttons.nth(0).textContent());
                assertEquals("Clicks: 0", buttons.nth(1).textContent());

                buttons.nth(1).click();
                buttons.nth(1).click();
                assertEquals("Clicks: 1", buttons.nth(0).textContent());
                assertEquals("Clicks: 2", buttons.nth(1).textContent());

                assertTrue(pageErrors.isEmpty(), () -> "Browser errors: " + pageErrors);
            } finally {
                browser.close();
            }
        }
    }

    @Test
    void conditionalExampleTogglesFragment() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                List<String> pageErrors = new CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                page.navigate(server.url() + "/index.html?example=conditional");

                assertEquals("Visible", page.textContent("p"));

                page.click("button");
                assertEquals("Hidden", page.textContent("p"));

                page.click("button");
                assertEquals("Visible", page.textContent("p"));

                assertTrue(pageErrors.isEmpty(), () -> "Browser errors: " + pageErrors);
            } finally {
                browser.close();
            }
        }
    }

    @Test
    void userCardExampleDrivesInputsOutputsAndSlots() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                List<String> pageErrors = new CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                page.navigate(server.url() + "/index.html?example=usercard");

                assertEquals("Team directory", page.textContent("h1"));
                assertEquals("@Ada", page.textContent("h3"));
                assertEquals("Core team", page.textContent("strong"));
                assertEquals("Event: —", page.textContent("#event-log"));

                page.click("button:has-text('Select Ada')");
                assertEquals("Event: Selected Ada", page.textContent("#event-log"));

                page.click("button:has-text('Switch member')");
                assertEquals("@Grace", page.textContent("h3"));
                assertEquals("Guest", page.textContent("strong"));

                assertTrue(pageErrors.isEmpty(), () -> "Browser errors: " + pageErrors);
            } finally {
                browser.close();
            }
        }
    }

    @Test
    void keyedListExampleRotatesOrder() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                List<String> pageErrors = new CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                page.navigate(server.url() + "/index.html?example=list");

                assertEquals("Alpha", page.locator("li").nth(0).textContent());
                assertEquals("Beta", page.locator("li").nth(1).textContent());
                assertEquals("Gamma", page.locator("li").nth(2).textContent());

                page.click("button:has-text('Rotate')");
                assertEquals("Gamma", page.locator("li").nth(0).textContent());
                assertEquals("Alpha", page.locator("li").nth(1).textContent());
                assertEquals("Beta", page.locator("li").nth(2).textContent());

                assertTrue(pageErrors.isEmpty(), () -> "Browser errors: " + pageErrors);
            } finally {
                browser.close();
            }
        }
    }

    @Test
    void unknownExampleFallsBackToCounter() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                page.navigate(server.url() + "/index.html?example=does-not-exist");
                assertEquals("Counter", page.textContent("h1"));
                assertFalse(page.isVisible("text=Composition"));
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
            Path webRoot = Path.of("target", "webapp").toAbsolutePath();
            assertTrue(Files.isRegularFile(webRoot.resolve("index.html")), "index.html is missing");
            assertTrue(Files.isRegularFile(webRoot.resolve("app.js")), "app.js is missing");

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
}
