package io.dejaview.ssr;

import io.dejaview.core.DejaViewException;
import io.dejaview.server.ServerElement;
import io.dejaview.server.ServerNode;
import io.dejaview.server.ServerText;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlPageTest {

    private static final ServerNode BODY =
            new ServerElement("p", List.of(), List.of(), List.of(new ServerText("Hello")));

    @Test
    void writesADocumentThatWorksWithoutJavaScript() {
        String html = HtmlPage.render(new PageMeta(), BODY);

        assertTrue(html.startsWith("<!DOCTYPE html>\n"));
        assertTrue(html.contains("<html lang=\"en\">"));
        assertTrue(html.contains("<meta charset=\"utf-8\">"));
        assertTrue(html.contains("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"));
        assertTrue(html.contains("<body><p>Hello</p></body>"));
        assertTrue(html.trim().endsWith("</html>"));
    }

    @Test
    void omitsOptionalHeadTagsWhenEmpty() {
        String html = HtmlPage.render(new PageMeta(), BODY);

        assertEquals(-1, html.indexOf("<title>"));
        assertEquals(-1, html.indexOf("name=\"description\""));
        assertEquals(-1, html.indexOf("rel=\"canonical\""));
    }

    @Test
    void writesAndEscapesAllMetadataInOrder() {
        PageMeta meta = new PageMeta()
                .lang("pt-BR")
                .title("A <b> & \"c\"")
                .description("D & \"q\"")
                .canonical("https://example.com/?a=1&b=2")
                .meta("robots", "noindex")
                .metaProperty("og:title", "OG \"title\"");

        String html = HtmlPage.render(meta, BODY);

        assertTrue(html.contains("<html lang=\"pt-BR\">"));
        assertTrue(html.contains("<title>A &lt;b&gt; &amp; \"c\"</title>"));
        assertTrue(html.contains("<meta name=\"description\" content=\"D &amp; &quot;q&quot;\">"));
        assertTrue(html.contains("<link rel=\"canonical\" href=\"https://example.com/?a=1&amp;b=2\">"));
        assertTrue(html.contains("<meta name=\"robots\" content=\"noindex\">"));
        assertTrue(html.contains("<meta property=\"og:title\" content=\"OG &quot;title&quot;\">"));
        assertTrue(html.indexOf("name=\"robots\"") < html.indexOf("property=\"og:title\""));
    }

    @Test
    void validatesTheBodyBeforeWriting() {
        ServerNode invalid = new ServerElement("DIV", List.of(), List.of(), List.of());

        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> HtmlPage.render(new PageMeta(), invalid));

        assertEquals("ssr-validation-failed", failure.code());
    }

    @Test
    void writerVariantMatchesTheStringVariant() throws IOException {
        PageMeta meta = new PageMeta().title("Title");
        StringWriter out = new StringWriter();

        HtmlPage.write(meta, BODY, out);

        assertEquals(HtmlPage.render(meta, BODY), out.toString());
    }
}
