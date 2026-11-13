package io.dejaview.examples.ssr;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.dejaview.http.HttpHeaders;
import io.dejaview.server.ServerRequest;
import io.dejaview.server.ServerResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Browser end-to-end tests for the SSR example, served by the JDK HTTP server
 * on an ephemeral loopback port.
 *
 * <p>Every context is created with JavaScript <em>disabled</em>: the point of
 * server-side rendering is that the page is complete HTML, so links and the
 * search form must work without a single line of client-side code. Runs only
 * with the {@code e2e} profile.</p>
 */
class SsrBrowserIT {

    @Test
    void theHomePageWorksWithoutJavaScript() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = noScriptContext(browser);
                 Page page = context.newPage()) {
                List<String> pageErrors = new CopyOnWriteArrayList<>();
                page.onPageError(pageErrors::add);

                Response response = page.navigate(server.url() + "/");

                assertEquals(200, response.status());
                assertTrue(response.headers().get("content-type").startsWith("text/html"));
                assertEquals("DejaView SSR", page.textContent("h1"));
                assertEquals(0, page.locator("script").count(), "SSR emits no client script");
                assertTrue(pageErrors.isEmpty(), () -> "Browser errors: " + pageErrors);
            } finally {
                browser.close();
            }
        }
    }

    @Test
    void linksAndTheSearchFormNavigateWithoutJavaScript() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = noScriptContext(browser);
                 Page page = context.newPage()) {
                page.navigate(server.url() + "/");
                page.click("text=Browse products");
                page.waitForURL("**/products");

                assertEquals("Products", page.textContent("h1"));
                assertEquals(5, page.locator("li").count());

                page.fill("#q", "key");
                page.click("button");
                page.waitForURL("**/products?q=key");

                assertEquals("Products matching \"key\"", page.textContent("h1"));
                assertEquals(1, page.locator("li").count());
                assertTrue(page.textContent("li").contains("Mechanical keyboard"));
            } finally {
                browser.close();
            }
        }
    }

    @Test
    void aContainedWidgetFailureStillRendersThePage() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = noScriptContext(browser);
                 Page page = context.newPage()) {
                Response response = page.navigate(server.url() + "/products?fail=1");

                assertEquals(200, response.status());
                assertTrue(page.textContent("body").contains("Prices are temporarily unavailable."));
                assertEquals(5, page.locator("li").count(), "the catalogue must survive");
            } finally {
                browser.close();
            }
        }
    }

    @Test
    void unknownPathsRenderTheNotFoundPage() throws IOException {
        try (Server server = Server.create(); Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = noScriptContext(browser);
                 Page page = context.newPage()) {
                Response response = page.navigate(server.url() + "/missing");

                assertEquals(404, response.status());
                assertEquals("Not found", page.textContent("h1"));
                assertFalse(page.textContent("body").contains("Exception"));
            } finally {
                browser.close();
            }
        }
    }

    private static BrowserContext noScriptContext(Browser browser) {
        return browser.newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false));
    }

    /** The example served by the JDK HTTP server on an ephemeral loopback port. */
    private static final class Server implements AutoCloseable {
        private final HttpServer server;

        private Server(HttpServer server) {
            this.server = server;
        }

        static Server create() throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", Server::handle);
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

        private static void handle(HttpExchange exchange) throws IOException {
            ServerRequest request = new ServerRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().toString(),
                    headers(exchange),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            ServerResponse response = SsrApplication.render(request).toResponse();
            for (String name : response.headers().names()) {
                exchange.getResponseHeaders().set(name, response.headers().joined(name));
            }
            byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(response.status(), body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }

        private static HttpHeaders headers(HttpExchange exchange) {
            HttpHeaders.Builder builder = HttpHeaders.builder();
            exchange.getRequestHeaders().forEach((name, values) -> values.forEach(value -> builder.add(name, value)));
            return builder.build();
        }
    }
}
