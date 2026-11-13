package io.dejaview.ssr;

import io.dejaview.core.AbstractComponent;
import io.dejaview.core.DejaViewException;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.di.Injector;
import io.dejaview.server.BoundaryError;
import io.dejaview.server.ServerContext;
import io.dejaview.server.ServerErrorBoundary;
import io.dejaview.server.ServerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsrRendererTest {

    private Injector root;
    private ServerContext context;

    @BeforeEach
    void setUp() {
        root = Injector.builder().name("ssr-test").build();
        context = ServerContext.create(root, ServerRequest.of("GET", "/"));
    }

    @AfterEach
    void tearDown() {
        context.close();
        root.close();
    }

    @Test
    void rendersACompleteEscapedDocument() {
        PageMeta meta = new PageMeta()
                .lang("pt-BR")
                .title("Produtos")
                .description("Catálogo <de> produtos")
                .canonical("https://example.com/produtos")
                .metaProperty("og:type", "website");

        SsrResult result = SsrRenderer.renderPage(
                Views.div(
                        Views.h1("Produtos"),
                        Views.p("<script>alert('x')</script>")),
                context,
                meta);

        assertTrue(result.isSuccess());
        assertEquals(200, result.status());
        String html = result.html();
        assertTrue(html.startsWith("<!DOCTYPE html>\n<html lang=\"pt-BR\">"));
        assertTrue(html.contains("<title>Produtos</title>"));
        assertTrue(html.contains("<meta name=\"description\" content=\"Catálogo &lt;de&gt; produtos\">"));
        assertTrue(html.contains("<link rel=\"canonical\" href=\"https://example.com/produtos\">"));
        assertTrue(html.contains("<meta property=\"og:type\" content=\"website\">"));
        assertTrue(html.contains("<body><div><h1>Produtos</h1>"
                + "<p>&lt;script&gt;alert('x')&lt;/script&gt;</p></div></body>"));
        assertFalse(html.contains("<script>"), "text must never become markup");
    }

    @Test
    void toResponseCarriesTheStatusAndHtmlContentType() {
        SsrResult result = SsrRenderer.renderPage(Views.p("Hi"), context, new PageMeta().title("T"));

        var response = result.toResponse();

        assertEquals(200, response.status());
        assertEquals(result.html(), response.body());
        assertEquals("text/html; charset=utf-8", response.headers().get("Content-Type"));
    }

    @Test
    void rendersAFragmentWithoutADocumentWrapper() {
        SsrResult result = SsrRenderer.renderFragment(
                Views.fragment(Views.p("one"), Views.p("two")), context);

        assertTrue(result.isSuccess());
        assertEquals("<p>one</p><p>two</p>", result.html());
    }

    @Test
    void reportsDroppedEvents() {
        SsrResult result = SsrRenderer.renderFragment(
                Views.button("Save").on("click", () -> { }).on("focus", () -> { }), context);

        assertTrue(result.isSuccess());
        assertEquals(List.of("click", "focus"), result.droppedEvents());
    }

    @Test
    void boundaryFailureIsContainedAndTheRenderSucceeds() {
        SsrResult result = SsrRenderer.renderPage(
                Views.div(
                        Views.h1("Page"),
                        Views.componentFactory(Boundary::new)
                                .slot(Views.componentFactory(Exploding::new))),
                context,
                new PageMeta());

        assertTrue(result.isSuccess());
        assertTrue(result.html().contains("<h1>Page</h1>"));
        assertTrue(result.html().contains("<p>Section unavailable</p>"));
        assertEquals(List.of(new BoundaryError(
                        Boundary.class.getName(),
                        RuntimeException.class.getName(),
                        "boom")),
                result.boundaryErrors());
        assertEquals(List.of(), result.warnings());
    }

    @Test
    void warningsDoNotChangeTheStatus() {
        SsrResult result = SsrRenderer.renderFragment(
                Views.element("script", Views.text("if (a < b) {}")), context);

        assertTrue(result.isSuccess());
        assertEquals(List.of(new SsrFinding(
                        "raw-text-escaped",
                        "text inside <script> is escaped, so the browser renders it as text instead of raw content")),
                result.warnings());
    }

    @Test
    void unhandledFailureReturnsTheGenericErrorPage() {
        SsrResult result = SsrRenderer.renderPage(
                Views.componentFactory(Exploding::new), context, new PageMeta());

        assertFalse(result.isSuccess());
        assertEquals(500, result.status());
        assertEquals(SsrRenderer.GENERIC_ERROR_HTML, result.html());
        assertFalse(result.html().contains("boom"), "the failure must not leak to the client");
        assertInstanceOf(RuntimeException.class, result.error());
        assertEquals("boom", result.error().getMessage());
    }

    @Test
    void validationFailureReturnsTheGenericErrorPage() {
        SsrResult result = SsrRenderer.renderPage(Views.element("DIV"), context, new PageMeta());

        assertFalse(result.isSuccess());
        assertEquals(500, result.status());
        assertEquals(SsrRenderer.GENERIC_ERROR_HTML, result.html());
        DejaViewException failure = assertInstanceOf(DejaViewException.class, result.error());
        assertEquals("ssr-validation-failed", failure.code());
    }

    @Test
    void writerVariantsMatchTheInMemoryVariants() throws IOException {
        PageMeta meta = new PageMeta().title("Title");
        View view = Views.div(Views.p("Hello"));

        SsrResult page = SsrRenderer.renderPage(view, context, meta);
        StringWriter pageOut = new StringWriter();
        SsrResult writtenPage = SsrRenderer.writePage(view, context, meta, pageOut);

        SsrResult fragment = SsrRenderer.renderFragment(view, context);
        StringWriter fragmentOut = new StringWriter();
        SsrResult writtenFragment = SsrRenderer.writeFragment(view, context, fragmentOut);

        assertEquals(page.html(), pageOut.toString());
        assertEquals(page.html(), writtenPage.html());
        assertEquals(fragment.html(), fragmentOut.toString());
        assertEquals(fragment.html(), writtenFragment.html());
    }

    @Test
    void aClosedContextFailsSafely() {
        context.close();

        SsrResult result = SsrRenderer.renderPage(Views.p("x"), context, new PageMeta());

        assertFalse(result.isSuccess());
        assertEquals(500, result.status());
        assertEquals("closed-context", assertInstanceOf(DejaViewException.class, result.error()).code());
    }

    // --- fixtures ---------------------------------------------------------

    private static final class Boundary extends AbstractComponent implements ServerErrorBoundary {
        @Override
        public View render() {
            return Views.fragment(slot().toArray(View[]::new));
        }

        @Override
        public View fallback(Throwable error) {
            return Views.p("Section unavailable");
        }
    }

    private static final class Exploding extends AbstractComponent {
        @Override
        public View render() {
            throw new RuntimeException("boom");
        }
    }
}
