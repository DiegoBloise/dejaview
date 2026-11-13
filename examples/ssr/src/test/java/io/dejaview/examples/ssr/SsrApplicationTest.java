package io.dejaview.examples.ssr;

import io.dejaview.server.BoundaryError;
import io.dejaview.server.ServerRequest;
import io.dejaview.ssr.SsrResult;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsrApplicationTest {

    private static SsrResult get(String target) {
        return SsrApplication.render(ServerRequest.of("GET", target));
    }

    @Test
    void homePageIsACompleteServerRenderedDocument() {
        SsrResult result = get("/");

        assertTrue(result.isSuccess());
        assertEquals(200, result.status());
        assertTrue(result.html().startsWith("<!DOCTYPE html>\n"));
        assertTrue(result.html().contains("<html lang=\"en\">"));
        assertTrue(result.html().contains("<title>DejaView SSR</title>"));
        assertTrue(result.html().contains("<h1>DejaView SSR</h1>"));
        assertTrue(result.html().contains("href=\"/products\""));
        assertTrue(result.html().contains("href=\"/products?fail=1\""));
        assertEquals(List.of(), result.boundaryErrors());
        assertEquals(List.of(), result.warnings());
    }

    @Test
    void productsAreFilteredByTheQueryString() {
        SsrResult result = get("/products?q=key");

        assertTrue(result.html().contains("<h1>Products matching \"key\"</h1>"));
        assertTrue(result.html().contains("Mechanical keyboard"));
        assertFalse(result.html().contains("Wireless mouse"));
        assertTrue(result.html().contains("1 product(s) found."));
    }

    @Test
    void anEmptyQueryShowsTheWholeCatalogue() {
        SsrResult result = get("/products");

        assertTrue(result.html().contains("<h1>Products</h1>"));
        assertTrue(result.html().contains("5 product(s) found."));
        assertTrue(result.html().contains("Catalogue total: 551.00"));
    }

    @Test
    void aQueryWithoutMatchesExplainsTheEmptyResult() {
        SsrResult result = get("/products?q=zzz");

        assertTrue(result.html().contains("Nothing matches that search."));
        assertFalse(result.html().contains("<ul>"));
    }

    @Test
    void userInputIsEscapedInsteadOfBecomingMarkup() {
        String hostile = "<script>alert(1)</script>";
        String query = URLEncoder.encode(hostile, StandardCharsets.UTF_8);

        SsrResult result = get("/products?q=" + query);

        assertTrue(result.isSuccess());
        assertFalse(result.html().contains("<script>"), "echoed input must never become an element");
        assertTrue(result.html().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(result.html().contains("<title>Products matching \"&lt;script&gt;alert(1)&lt;/script&gt;\"</title>"));
    }

    @Test
    void aFailingWidgetIsContainedByTheErrorBoundary() {
        SsrResult result = get("/products?fail=1");

        assertTrue(result.isSuccess());
        assertEquals(200, result.status());
        assertTrue(result.html().contains("Prices are temporarily unavailable."));
        assertTrue(result.html().contains("Mechanical keyboard"), "the rest of the page must survive");
        assertEquals(List.of(new BoundaryError(
                        SsrApplication.SectionBoundary.class.getName(),
                        IllegalStateException.class.getName(),
                        "price service unavailable")),
                result.boundaryErrors());
    }

    @Test
    void unknownPathsRenderANotFoundPageWithStatus404() {
        SsrResult result = get("/missing");

        assertTrue(result.isSuccess());
        assertEquals(404, result.status());
        assertTrue(result.html().contains("<h1>Not found</h1>"));
        assertTrue(result.html().contains("<title>Not found</title>"));
    }

    @Test
    void requestsDoNotShareMetadataOrOutput() {
        SsrResult keyboard = get("/products?q=key");
        SsrResult mouse = get("/products?q=mouse");

        assertTrue(keyboard.html().contains("Mechanical keyboard"));
        assertFalse(mouse.html().contains("Mechanical keyboard"));
        assertTrue(mouse.html().contains("Wireless mouse"));
        assertFalse(keyboard.html().contains("Wireless mouse"));
    }

    @Test
    void renderingTheSameRequestTwiceIsDeterministic() {
        assertEquals(get("/products").html(), get("/products").html());
    }

    @Test
    void malformedQueryEncodingFallsBackToTheRawValueInsteadOfFailing() {
        SsrResult result = get("/products?q=%zz");

        assertTrue(result.isSuccess());
        assertTrue(result.html().contains("%zz"));
    }
}
