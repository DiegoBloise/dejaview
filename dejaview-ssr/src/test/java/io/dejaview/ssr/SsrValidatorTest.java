package io.dejaview.ssr;

import io.dejaview.core.Attribute;
import io.dejaview.core.DejaViewException;
import io.dejaview.core.Property;
import io.dejaview.server.ServerElement;
import io.dejaview.server.ServerFragment;
import io.dejaview.server.ServerNode;
import io.dejaview.server.ServerText;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsrValidatorTest {

    @Test
    void aWellFormedTreeHasNoFindings() {
        ServerNode node = new ServerElement(
                "div",
                List.of(new Attribute("class", "card")),
                List.of(new Property("value", "x"), new Property("checked", "true")),
                List.of(new ServerText("hello")));

        assertEquals(List.of(), SsrValidator.validate(node));
    }

    @Test
    void reportsRawTextElements() {
        ServerNode node = new ServerElement(
                "script", List.of(), List.of(), List.of(new ServerText("if (a < b) {}")));

        assertEquals(List.of(new SsrFinding(
                        "raw-text-escaped",
                        "text inside <script> is escaped, so the browser renders it as text instead of raw content")),
                SsrValidator.validate(node));
    }

    @Test
    void reportsPropertiesThatCannotBeSerialized() {
        ServerNode node = new ServerElement(
                "div", List.of(), List.of(new Property("className", "card")), List.of());

        assertEquals(List.of(new SsrFinding(
                        "skipped-property",
                        "property 'className' has no HTML representation in this milestone and was not serialized")),
                SsrValidator.validate(node));
    }

    @Test
    void reportsAnEmptyRender() {
        assertEquals(List.of(new SsrFinding("empty-root", "the render produced no content")),
                SsrValidator.validate(new ServerFragment(List.of())));
        assertEquals(List.of(new SsrFinding("empty-root", "the render produced no content")),
                SsrValidator.validate(new ServerText("")));
    }

    @Test
    void rejectsIllegalTagNames() {
        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> SsrValidator.validate(new ServerElement("DIV", List.of(), List.of(), List.of())));

        assertEquals("ssr-validation-failed", failure.code());
        assertTrue(failure.getMessage().contains("illegal tag name 'DIV'"));
    }

    @Test
    void rejectsIllegalAttributeNames() {
        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> SsrValidator.validate(new ServerElement(
                        "div",
                        List.of(new Attribute("on click", "x")),
                        List.of(),
                        List.of())));

        assertEquals("ssr-validation-failed", failure.code());
        assertTrue(failure.getMessage().contains("illegal attribute name 'on click'"));
    }

    @Test
    void rejectsVoidElementsWithChildren() {
        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> SsrValidator.validate(new ServerElement(
                        "input", List.of(), List.of(), List.of(new ServerText("x")))));

        assertEquals("ssr-validation-failed", failure.code());
        assertTrue(failure.getMessage().contains("void element <input> must not have children"));
    }

    @Test
    void rejectsDuplicateAttributes() {
        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> SsrValidator.validate(new ServerElement(
                        "div",
                        List.of(new Attribute("class", "a"), new Attribute("class", "b")),
                        List.of(),
                        List.of())));

        assertEquals("ssr-validation-failed", failure.code());
        assertTrue(failure.getMessage().contains("duplicate attribute 'class'"));
    }

    @Test
    void rejectsTreesBeyondTheDepthLimit() {
        ServerNode node = new ServerText("x");
        for (int i = 0; i < HtmlRules.MAX_DEPTH + 2; i++) {
            node = new ServerElement("div", List.of(), List.of(), List.of(node));
        }

        ServerNode tooDeep = node;
        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> SsrValidator.validate(tooDeep));

        assertEquals("ssr-validation-failed", failure.code());
        assertTrue(failure.getMessage().contains("depth limit of " + HtmlRules.MAX_DEPTH + " exceeded"));
    }

    @Test
    void reportsEveryViolationInOneMessage() {
        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> SsrValidator.validate(new ServerFragment(List.of(
                        new ServerElement("DIV", List.of(), List.of(), List.of()),
                        new ServerElement("input", List.of(), List.of(), List.of(new ServerText("x")))))));

        String message = failure.getMessage();
        assertTrue(message.contains("illegal tag name 'DIV'"));
        assertTrue(message.contains("void element <input> must not have children"));
    }
}
