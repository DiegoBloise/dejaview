package io.dejaview.ssg;

import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.ssr.PageMeta;
import io.dejaview.ssr.SsrFinding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsgGeneratorTest {

    @TempDir
    Path tempDir;

    private SsgSite.Builder site() {
        return SsgSite.builder(tempDir.resolve("site"));
    }

    private static Function<PageMeta, View> page(String title, View... children) {
        return meta -> {
            meta.title(title).description(title + " description");
            List<View> all = new ArrayList<>();
            all.add(Views.h1(title));
            all.addAll(List.of(children));
            return Views.div(all.toArray(View[]::new));
        };
    }

    private static View link(String href, String label) {
        return Views.element("a", Views.text(label)).attr("href", href);
    }

    private static String html(SsgResult result, String path) throws IOException {
        SsgPage page = result.pages().stream()
                .filter(candidate -> candidate.path().equals(path))
                .findFirst()
                .orElseThrow(() -> new AssertionError("page not generated: " + path));
        return Files.readString(page.file(), StandardCharsets.UTF_8);
    }

    @Test
    void staticRoutesProduceCompleteEscapedHtml() throws IOException {
        SsgResult result = SsgGenerator.generate(site()
                .route(SsgRoute.page("/", "en", page("Home", Views.p("Welcome"))))
                .route(SsgRoute.page("/about", "en", page("About us")))
                .route(SsgRoute.page("/404.html", "en", page("Not found")))
                .build());

        assertEquals(3, result.pageCount());
        assertEquals(tempDir.resolve("site"), result.outputDir());
        assertTrue(Files.exists(tempDir.resolve("site/index.html")));
        assertTrue(Files.exists(tempDir.resolve("site/about/index.html")));
        assertTrue(Files.exists(tempDir.resolve("site/404.html")));

        String home = html(result, "/");
        assertTrue(home.startsWith("<!DOCTYPE html>\n<html lang=\"en\">"));
        assertTrue(home.contains("<title>Home</title>"));
        assertTrue(home.contains("<h1>Home</h1><p>Welcome</p>"));
        assertEquals("Home", result.pages().getFirst().title());
    }

    @Test
    void textAndMetadataAreEscaped() throws IOException {
        SsgResult result = SsgGenerator.generate(site()
                .route(SsgRoute.page("/", "en", meta -> {
                    meta.title("<script>alert(1)</script>");
                    return Views.p("<script>alert(1)</script>");
                }))
                .build());

        String html = html(result, "/");
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
    }

    @Test
    void dynamicRoutesWriteOneFilePerParameterSet() throws IOException {
        SsgResult result = SsgGenerator.generate(site()
                .route(SsgRoute.page("/", "en", page("Products")))
                .route(SsgRoute.parametrized(
                        "/products/{slug}", "en",
                        List.of(Map.of("slug", "keyboard"), Map.of("slug", "mouse")),
                        ctx -> {
                            ctx.meta().title(ctx.param("slug"));
                            return Views.h1(ctx.param("slug"));
                        }))
                .build());

        assertEquals(3, result.pageCount());
        assertTrue(Files.exists(tempDir.resolve("site/products/keyboard/index.html")));
        assertTrue(Files.exists(tempDir.resolve("site/products/mouse/index.html")));
        assertEquals("<h1>keyboard</h1>", body(html(result, "/products/keyboard")));
        assertEquals("<h1>mouse</h1>", body(html(result, "/products/mouse")));
    }

    @Test
    void metadataCanonicalUrlsAndSitemapAreGenerated() throws IOException {
        SsgResult result = SsgGenerator.generate(site()
                .baseUrl("https://example.com/")
                .route(SsgRoute.page("/", "pt-BR", meta -> {
                    meta.title("Início").description("Primeira página");
                    return Views.h1("Início");
                }))
                .route(SsgRoute.page("/about", "pt-BR", meta -> {
                    meta.title("Sobre");
                    return Views.h1("Sobre");
                }))
                .build());

        String home = html(result, "/");
        assertTrue(home.contains("<html lang=\"pt-BR\">"));
        assertTrue(home.contains("<title>Início</title>"));
        assertTrue(home.contains("<meta name=\"description\" content=\"Primeira página\">"));
        assertTrue(home.contains("<link rel=\"canonical\" href=\"https://example.com/\">"));

        String about = html(result, "/about");
        assertTrue(about.contains("<link rel=\"canonical\" href=\"https://example.com/about/\">"));

        Path sitemap = result.sitemapFile().orElseThrow();
        String xml = Files.readString(sitemap, StandardCharsets.UTF_8);
        assertTrue(xml.contains("<loc>https://example.com/</loc>"));
        assertTrue(xml.contains("<loc>https://example.com/about/</loc>"));
        assertEquals(2, occurrences(xml, "<loc>"));
    }

    @Test
    void anExplicitCanonicalWinsOverTheGeneratedOne() throws IOException {
        SsgResult result = SsgGenerator.generate(site()
                .baseUrl("https://example.com")
                .route(SsgRoute.page("/about", "en", meta -> {
                    meta.canonical("https://example.com/custom");
                    return Views.h1("About");
                }))
                .build());

        assertTrue(html(result, "/about").contains(
                "<link rel=\"canonical\" href=\"https://example.com/custom\">"));
    }

    @Test
    void noSitemapIsWrittenWithoutABaseUrl() {
        SsgResult result = SsgGenerator.generate(site()
                .route(SsgRoute.page("/", "en", page("Home")))
                .build());

        assertTrue(result.sitemapFile().isEmpty());
        assertFalse(Files.exists(tempDir.resolve("site/sitemap.xml")));
    }

    @Test
    void assetsAreCopiedRecursivelyKeepingTheirLayout() throws IOException {
        Path css = Files.writeString(tempDir.resolve("site.css"), "body{}");
        Path directory = tempDir.resolve("static");
        Files.createDirectories(directory.resolve("nested"));
        Files.writeString(directory.resolve("app.js"), "console.log(1)");
        Files.writeString(directory.resolve("nested/logo.txt"), "logo");

        SsgResult result = SsgGenerator.generate(site()
                .route(SsgRoute.page("/", "en", meta -> {
                    meta.title("Assets");
                    return Views.div(
                            Views.element("link").attr("rel", "stylesheet").attr("href", "/assets/site.css"),
                            Views.element("script").attr("src", "/static/app.js"),
                            Views.element("img").attr("src", "/static/nested/logo.txt"));
                }))
                .asset(css, "/assets/site.css")
                .asset(directory, "static")
                .build());

        assertEquals(3, result.assetCount());
        assertEquals("body{}", Files.readString(tempDir.resolve("site/assets/site.css")));
        assertEquals("console.log(1)", Files.readString(tempDir.resolve("site/static/app.js")));
        assertEquals("logo", Files.readString(tempDir.resolve("site/static/nested/logo.txt")));
    }

    @Test
    void aBrokenLocalReferenceFailsTheBuildAndNamesThePage() {
        SsgException error = assertThrows(SsgException.class, () -> SsgGenerator.generate(site()
                .route(SsgRoute.page("/", "en", meta -> Views.div(
                        link("/missing", "Missing"))))
                .build()));

        assertTrue(error.getMessage().contains("static reference validation failed"));
        assertTrue(error.getMessage().contains("/ -> href"));
        assertTrue(error.getMessage().contains("/missing"));
        assertEquals("ssg-generation-failed", error.code());
    }

    @Test
    void relativeDirectoryAndPercentEncodedReferencesResolve() {
        Path asset = tempDir.resolve("logo file.css");
        try {
            Files.writeString(asset, "body{}");
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        SsgResult result = SsgGenerator.generate(site()
                .asset(asset, "assets/logo file.css")
                .route(SsgRoute.page("/", "en", meta -> Views.div(
                        link("/about", "About"),
                        link("about/", "About slash"),
                        link("/assets/logo%20file.css", "Logo"),
                        link("#top", "Top"),
                        link("?q=1", "Query"),
                        link("https://example.com", "External"),
                        link("mailto:dev@example.com", "Mail"))))
                .route(SsgRoute.page("/about", "en", page("About")))
                .build());

        assertEquals(2, result.pageCount());
    }

    @Test
    void aFailingRouteIdentifiesItselfAndWritesNothing() {
        Path output = tempDir.resolve("site");

        SsgException error = assertThrows(SsgException.class, () -> SsgGenerator.generate(site()
                .route(SsgRoute.page("/", "en", page("Home")))
                .route(SsgRoute.page("/broken", "en", meta -> {
                    throw new IllegalStateException("boom");
                }))
                .build()));

        assertTrue(error.getMessage().contains("/broken"));
        assertTrue(error.getMessage().contains("boom"));
        assertTrue(error.getCause() instanceof IllegalStateException);
        assertFalse(Files.exists(output), "pages are rendered before anything is written");
    }

    @Test
    void renderingFailuresFromThePipelineAlsoIdentifyTheRoute() {
        SsgException error = assertThrows(SsgException.class, () -> SsgGenerator.generate(site()
                .route(SsgRoute.page("/bad", "en", meta -> Views.element("Bad Tag")))
                .build()));

        assertTrue(error.getMessage().contains("/bad"));
    }

    @Test
    void duplicateRoutePathsFail() {
        SsgException error = assertThrows(SsgException.class, () -> SsgGenerator.generate(site()
                .route(SsgRoute.page("/about", "en", page("One")))
                .route(SsgRoute.page("/about", "en", page("Two")))
                .build()));

        assertTrue(error.getMessage().contains("duplicate route path"));
        assertTrue(error.getMessage().contains("/about"));
    }

    @Test
    void routesThatWriteTheSameFileFail() {
        SsgException error = assertThrows(SsgException.class, () -> SsgGenerator.generate(site()
                .route(SsgRoute.page("/about", "en", page("One")))
                .route(SsgRoute.page("/about/", "en", page("Two")))
                .build()));

        assertTrue(error.getMessage().contains("both write about/index.html"));
    }

    @Test
    void anAssetCannotOverwriteAPage() throws IOException {
        Path asset = Files.writeString(tempDir.resolve("index.html"), "stale");

        SsgException error = assertThrows(SsgException.class, () -> SsgGenerator.generate(site()
                .route(SsgRoute.page("/", "en", page("Home")))
                .asset(asset, "index.html")
                .build()));

        assertTrue(error.getMessage().contains("would overwrite generated page"));
    }

    @Test
    void duplicateAssetTargetsFail() throws IOException {
        Path one = Files.writeString(tempDir.resolve("one.css"), "one");
        Path two = Files.writeString(tempDir.resolve("two.css"), "two");

        SsgException error = assertThrows(SsgException.class, () -> SsgGenerator.generate(site()
                .route(SsgRoute.page("/", "en", page("Home")))
                .asset(one, "assets/site.css")
                .asset(two, "/assets/site.css")
                .build()));

        assertTrue(error.getMessage().contains("duplicate asset target"));
    }

    @Test
    void aMissingAssetSourceFails() {
        SsgException error = assertThrows(SsgException.class, () -> SsgGenerator.generate(site()
                .route(SsgRoute.page("/", "en", page("Home")))
                .asset(tempDir.resolve("missing.css"), "assets/site.css")
                .build()));

        assertTrue(error.getMessage().contains("asset source does not exist"));
    }

    @Test
    void cleaningRemovesStaleFilesByDefault() throws IOException {
        Path output = tempDir.resolve("site");
        Files.createDirectories(output);
        Files.writeString(output.resolve("stale.html"), "stale");

        SsgGenerator.generate(site()
                .route(SsgRoute.page("/", "en", page("Home")))
                .build());

        assertFalse(Files.exists(output.resolve("stale.html")));
        assertTrue(Files.exists(output.resolve("index.html")));
    }

    @Test
    void cleaningCanBeDisabled() throws IOException {
        Path output = tempDir.resolve("site");
        Files.createDirectories(output);
        Files.writeString(output.resolve("stale.html"), "stale");

        SsgGenerator.generate(site()
                .clean(false)
                .route(SsgRoute.page("/", "en", page("Home")))
                .build());

        assertTrue(Files.exists(output.resolve("stale.html")));
        assertTrue(Files.exists(output.resolve("index.html")));
    }

    @Test
    void refusesToCleanADirectoryThatLooksLikeAProject() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), "<project/>");

        SsgException error = assertThrows(SsgException.class, () -> SsgGenerator.generate(
                SsgSite.builder(tempDir)
                        .route(SsgRoute.page("/", "en", page("Home")))
                        .build()));

        assertTrue(error.getMessage().contains("refusing to clean"));
        assertTrue(error.getMessage().contains("pom.xml"));
    }

    @Test
    void warningsAreTaggedWithThePageThatProducedThem() {
        SsgResult result = SsgGenerator.generate(site()
                .route(SsgRoute.page("/", "en", meta -> Views.div(Views.h1("Home")).prop("className", "x")))
                .build());

        assertEquals(1, result.warnings().size());
        SsgWarning warning = result.warnings().getFirst();
        assertEquals("/", warning.path());
        assertEquals("skipped-property", warning.finding().code());
        assertTrue(warning.finding().message().contains("className"));
        assertEquals(List.of(new SsgWarning("/", new SsrFinding(
                warning.finding().code(), warning.finding().message()))), result.warnings());
    }

    @Test
    void pagesDoNotShareMetadataOrOutput() throws IOException {
        SsgResult result = SsgGenerator.generate(site()
                .route(SsgRoute.page("/one", "en", page("One")))
                .route(SsgRoute.page("/two", "en", page("Two")))
                .build());

        assertTrue(html(result, "/one").contains("<title>One</title>"));
        assertFalse(html(result, "/one").contains("<title>Two</title>"));
        assertTrue(html(result, "/two").contains("<title>Two</title>"));
        assertFalse(html(result, "/two").contains("<title>One</title>"));
    }

    private static String body(String html) {
        int start = html.indexOf("<body>");
        int end = html.indexOf("</body>");
        return html.substring(start + "<body>".length(), end);
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int index = text.indexOf(needle);
        while (index >= 0) {
            count++;
            index = text.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
