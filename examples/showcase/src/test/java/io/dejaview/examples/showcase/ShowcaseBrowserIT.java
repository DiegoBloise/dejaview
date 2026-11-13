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

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
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
    void routerExampleNavigatesAndRestoresHistory() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                List<String> pageErrors = new CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                page.navigate(server.url() + "/?example=router");

                assertEquals("Home", page.textContent("h2#router-home"));

                page.click("#router-open-user");
                assertEquals("User profile", page.textContent("h2#router-user"));
                assertEquals("User id: 42", page.textContent("#router-user-id"));
                assertTrue(page.url().endsWith("/users/42"));

                page.click("#router-user-about");
                assertEquals("About", page.textContent("h2#router-about"));

                page.goBack();
                assertEquals("User id: 42", page.textContent("#router-user-id"));

                page.goBack();
                assertEquals("Home", page.textContent("h2#router-home"));

                page.goForward();
                assertEquals("User id: 42", page.textContent("#router-user-id"));

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

    @Test
    void httpExampleFetchesPingJson() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                List<String> pageErrors = new CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                page.navigate(server.url() + "/index.html?example=http");

                assertEquals("HTTP", page.textContent("h1"));
                assertEquals("—", page.textContent("#http-result"));

                page.click("#http-ping");
                assertThat(page.locator("#http-result")).hasText("pong");

                assertTrue(pageErrors.isEmpty(), () -> "Browser errors: " + pageErrors);
            } finally {
                browser.close();
            }
        }
    }

    @Test
    void formsExampleValidatesAndSubmits() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                List<String> pageErrors = new CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                page.navigate(server.url() + "/index.html?example=forms");

                assertEquals("Reactive forms", page.textContent("h1"));
                assertEquals("Status: INVALID", page.textContent("#forms-status"));

                page.click("#forms-submit");
                assertEquals("Please fix the highlighted fields.",
                        page.textContent("#forms-result"));
                assertEquals("This field is required.",
                        page.textContent("#forms-name-error"));

                page.fill("#forms-name", "Ada");
                page.fill("#forms-email", "ada@example.com");
                page.fill("#forms-username", "ada");
                page.fill("#forms-tag-0", "ui");
                page.check("#forms-newsletter");
                page.selectOption("#forms-plan", "pro");

                // The username validator resolves after 600 ms; assertions wait.
                assertThat(page.locator("#forms-status")).hasText("Status: VALID");

                page.click("#forms-submit");
                assertThat(page.locator("#forms-result")).containsText("Submitted:");
                assertThat(page.locator("#forms-result"))
                        .containsText("email=ada@example.com");

                page.click("#forms-add-tag");
                assertEquals(2, page.locator("input[id^=forms-tag-]").count());
                page.fill("#forms-tag-1", "java");
                page.click("#forms-submit");
                assertThat(page.locator("#forms-result")).containsText("tags=[ui, java]");

                page.click("#forms-remove-tag-1");
                assertEquals(1, page.locator("input[id^=forms-tag-]").count());

                assertTrue(pageErrors.isEmpty(), () -> "Browser errors: " + pageErrors);
            } finally {
                browser.close();
            }
        }
    }

    @Test
    void stateExampleManagesSharedDerivedAndAsyncState() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                List<String> pageErrors = new CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                page.navigate(server.url() + "/index.html?example=state");

                assertEquals("State management", page.textContent("h1"));
                assertEquals("Items: 0", page.textContent("#state-count"));
                assertEquals("Subtotal: 0.00", page.textContent("#state-subtotal"));
                assertEquals("Total: 0.00", page.textContent("#state-total"));

                // Checkout with an empty cart reports a stable error code.
                page.click("#state-checkout");
                assertEquals("Checkout failed: empty-cart", page.textContent("#state-result"));

                page.click("#state-add-book");
                page.click("#state-add-pen");
                assertEquals("Items: 2", page.textContent("#state-count"));
                assertEquals("Subtotal: 15.00", page.textContent("#state-subtotal"));
                assertEquals("Total: 15.00", page.textContent("#state-total"));

                page.click("#state-discount-10");
                assertEquals("Total: 13.50", page.textContent("#state-total"));

                // The loader completes after 700 ms; assertions wait.
                page.click("#state-checkout");
                assertThat(page.locator("#state-result")).containsText("Order placed: order-");

                page.click("#state-theme-toggle");
                assertEquals("Theme: light", page.textContent("#state-theme"));

                // The theme is persisted in localStorage and survives a reload.
                page.navigate(server.url() + "/index.html?example=state");
                assertEquals("Theme: light", page.textContent("#state-theme"));

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
            if (name.endsWith(".json")) {
                return "application/json; charset=utf-8";
            }
            return "application/octet-stream";
        }
    }
}
