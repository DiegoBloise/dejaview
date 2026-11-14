package io.dejaview.examples.ssg;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import io.dejaview.ssg.SsgGenerator;
import io.dejaview.ssg.SsgResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Browser end-to-end tests for the generated static site, served over plain
 * HTTP from an ephemeral loopback port. The pages are pure HTML plus CSS: the
 * tests assert the deployment shape — a complete page for every route, a
 * stylesheet that loads, and a 404 document — and that the whole thing renders
 * without JavaScript. Runs only with the {@code e2e} profile.
 */
class SsgBrowserIT {

    @TempDir
    Path tempDir;

    private Path assetsDir() throws IOException {
        Path assets = tempDir.resolve("fixture-assets");
        Files.createDirectories(assets);
        Files.writeString(assets.resolve("site.css"), "body{}");
        return assets;
    }

    @Test
    void theGeneratedSiteIsCompleteHtmlWithCss() throws IOException {
        Path output = tempDir.resolve("site");
        SsgResult result = SsgGenerator.generate(StaticSite.site(output, assetsDir()));

        String indexHtml = Files.readString(output.resolve("index.html"));
        assertTrue(indexHtml.startsWith("<!DOCTYPE html>\n"));
        assertTrue(indexHtml.contains("<title>DejaView static site</title>"));
        assertTrue(indexHtml.contains("<link rel=\"stylesheet\" href=\"/assets/site.css\">"));
        assertTrue(indexHtml.contains("<h1>DejaView static site</h1>"));
        assertTrue(Files.exists(output.resolve("assets/site.css")));
        assertEquals(7, result.pageCount());
    }

    @Test
    void theGeneratedLandingPageRendersAndServes() throws IOException {
        Path output = tempDir.resolve("site");
        SsgGenerator.generate(StaticSite.site(output, assetsDir()));

        try (StaticServer server = StaticServer.start(output)) {
            try (Playwright playwright = Playwright.create()) {
                Browser browser = playwright.chromium().launch(
                        new BrowserType.LaunchOptions().setHeadless(true));
                try (BrowserContext context = browser.newContext(
                        new Browser.NewContextOptions().setJavaScriptEnabled(false));
                     Page page = context.newPage()) {
                    Response response = page.navigate(server.url() + "/");

                    assertEquals(200, response.status());
                    assertTrue(response.headers().get("content-type").startsWith("text/html"));
                    assertEquals("DejaView static site", page.textContent("h1"));
                    assertEquals(0, page.locator("script").count(), "SSG emits no client script");
                } finally {
                    browser.close();
                }
            }
        }
    }

    @Test
    void everyGeneratedLinkResolvesToAFile() throws IOException {
        Path output = tempDir.resolve("site");
        SsgGenerator.generate(StaticSite.site(output, assetsDir()));

        try (StaticServer server = StaticServer.start(output)) {
            try (Playwright playwright = Playwright.create()) {
                Browser browser = playwright.chromium().launch(
                        new BrowserType.LaunchOptions().setHeadless(true));
                try (BrowserContext context = browser.newContext(
                        new Browser.NewContextOptions().setJavaScriptEnabled(false));
                     Page page = context.newPage()) {
                    for (String path : List.of("/", "/about/", "/products/", "/404.html")) {
                        Response response = page.navigate(server.url() + path);
                        assertEquals(200, response.status(), "expected " + path);
                    }
                    assertEquals("Not found", page.textContent("h1"), "the 404 document renders standalone");
                } finally {
                    browser.close();
                }
            }
        }
    }

    /** A minimal static file server: one directory, one context. */
    private static final class StaticServer implements AutoCloseable {
        private final com.sun.net.httpserver.HttpServer server;

        private StaticServer(com.sun.net.httpserver.HttpServer server) {
            this.server = server;
        }

        static StaticServer start(Path output) throws IOException {
            com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(
                    new java.net.InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String target = exchange.getRequestURI().getPath();
                if (target.startsWith("/")) {
                    target = target.substring(1);
                }
                String fileName;
                Path file;
                if (target.isEmpty()) {
                    fileName = "index.html";
                    file = output.resolve(fileName);
                } else if (target.endsWith("/")) {
                    fileName = target + "index.html";
                    file = output.resolve(fileName);
                } else if (target.endsWith(".html")) {
                    fileName = target;
                    file = output.resolve(fileName);
                } else {
                    fileName = target + "/index.html";
                    file = output.resolve(fileName);
                }
                byte[] body = Files.readAllBytes(file);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, body.length);
                try (var out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            return new StaticServer(server);
        }

        public String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}