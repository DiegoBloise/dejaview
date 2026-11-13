package io.dejaview.ssr;

import io.dejaview.core.Attribute;
import io.dejaview.core.DejaViewException;
import io.dejaview.core.Property;
import io.dejaview.server.ServerElement;
import io.dejaview.server.ServerFragment;
import io.dejaview.server.ServerNode;
import io.dejaview.server.ServerText;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlSerializerTest {

    @Test
    void escapesTextContent() {
        assertEquals("a &amp; b &lt;c&gt; d", HtmlSerializer.serialize(new ServerText("a & b <c> d")));
    }

    @Test
    void escapesAttributeValuesIncludingQuotes() {
        ServerNode node = new ServerElement(
                "div",
                List.of(new Attribute("title", "a \"b\" & 'c' <d>")),
                List.of(),
                List.of());

        assertEquals("<div title=\"a &quot;b&quot; &amp; &#x27;c&#x27; &lt;d&gt;\"></div>",
                HtmlSerializer.serialize(node));
    }

    @Test
    void writesNestedElementsAndTextInRenderOrder() {
        ServerNode node = new ServerElement(
                "section",
                List.of(new Attribute("id", "main")),
                List.of(),
                List.of(
                        new ServerElement("h1", List.of(), List.of(), List.of(new ServerText("Title"))),
                        new ServerText("Body")));

        assertEquals("<section id=\"main\"><h1>Title</h1>Body</section>",
                HtmlSerializer.serialize(node));
    }

    @Test
    void splicesFragmentsWithoutWrapperElements() {
        ServerNode node = new ServerFragment(List.of(
                new ServerText("a"),
                new ServerElement("br", List.of(), List.of(), List.of()),
                new ServerText("b")));

        assertEquals("a<br>b", HtmlSerializer.serialize(node));
    }

    @Test
    void voidElementsHaveNoClosingTag() {
        ServerNode node = new ServerElement(
                "img", List.of(new Attribute("src", "/a.png"), new Attribute("alt", "A")), List.of(), List.of());

        assertEquals("<img src=\"/a.png\" alt=\"A\">", HtmlSerializer.serialize(node));
    }

    @Test
    void valuePropertyBecomesTheValueAttribute() {
        ServerNode node = new ServerElement(
                "input", List.of(), List.of(new Property("value", "Ada")), List.of());

        assertEquals("<input value=\"Ada\">", HtmlSerializer.serialize(node));
    }

    @Test
    void booleanPropertiesEmitTheirAttributeOnlyWhenTrue() {
        ServerNode checked = new ServerElement(
                "input", List.of(), List.of(new Property("checked", "TRUE")), List.of());
        ServerNode unchecked = new ServerElement(
                "input", List.of(), List.of(new Property("checked", "false")), List.of());

        assertEquals("<input checked>", HtmlSerializer.serialize(checked));
        assertEquals("<input>", HtmlSerializer.serialize(unchecked));
    }

    @Test
    void unsupportedPropertiesAreNotSerialized() {
        ServerNode node = new ServerElement(
                "div", List.of(), List.of(new Property("className", "card")), List.of());

        assertEquals("<div></div>", HtmlSerializer.serialize(node));
    }

    @Test
    void anAttributeWinsOverAPRojectedPropertyOfTheSameName() {
        ServerNode node = new ServerElement(
                "input",
                List.of(new Attribute("value", "explicit")),
                List.of(new Property("value", "property")),
                List.of());

        assertEquals("<input value=\"explicit\">", HtmlSerializer.serialize(node));
    }

    @Test
    void writerVariantMatchesTheStringVariant() throws IOException {
        ServerNode node = new ServerElement(
                "p", List.of(), List.of(), List.of(new ServerText("a & b")));
        StringWriter out = new StringWriter();

        HtmlSerializer.write(node, out);

        assertEquals(HtmlSerializer.serialize(node), out.toString());
    }

    @Test
    void rejectsInvalidTreesBeforeEmittingAnything() {
        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> HtmlSerializer.serialize(new ServerElement("DIV", List.of(), List.of(), List.of())));

        assertEquals("ssr-validation-failed", failure.code());
        assertTrue(failure.getMessage().contains("illegal tag name 'DIV'"));
    }
}
