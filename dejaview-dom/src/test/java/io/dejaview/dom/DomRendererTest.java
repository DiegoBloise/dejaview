package io.dejaview.dom;

import io.dejaview.core.View;
import io.dejaview.core.Views;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomRendererTest {

    private final FakeDomDocument document = new FakeDomDocument();
    private final DomRenderer renderer = new DomRenderer();

    @Test
    void rendersNestedElementsInOrder() {
        FakeDomElement container = document.createElement("main");

        renderer.render(
                Views.div(
                        Views.h1("DejaView"),
                        Views.p("Foundation")),
                document,
                container);

        FakeDomElement root = assertInstanceOf(FakeDomElement.class, container.children().get(0));
        assertEquals("div", root.tagName());
        assertEquals(2, root.children().size());

        FakeDomElement heading = assertInstanceOf(FakeDomElement.class, root.children().get(0));
        assertEquals("h1", heading.tagName());
        assertEquals("DejaView", assertInstanceOf(FakeText.class, heading.children().get(0)).text());

        FakeDomElement paragraph = assertInstanceOf(FakeDomElement.class, root.children().get(1));
        assertEquals("p", paragraph.tagName());
        assertEquals("Foundation", assertInstanceOf(FakeText.class, paragraph.children().get(0)).text());
    }

    @Test
    void appliesAttributesBeforeAppendingChildren() {
        FakeDomElement container = document.createElement("main");

        renderer.render(Views.div().id("app").classes("shell", "shell--ready"), document, container);

        FakeDomElement root = assertInstanceOf(FakeDomElement.class, container.children().get(0));
        assertEquals("app", root.attributes().get("id"));
        assertEquals("shell shell--ready", root.attributes().get("class"));
    }

    @Test
    void registersListenersThatCanBeFired() {
        AtomicInteger clicks = new AtomicInteger();
        FakeDomElement container = document.createElement("main");

        renderer.render(Views.button("Increment").onClick(clicks::incrementAndGet), document, container);

        FakeDomElement button = assertInstanceOf(FakeDomElement.class, container.children().get(0));
        button.fire("click");
        button.fire("click");

        assertEquals(2, clicks.get());
    }

    @Test
    void flattensFragmentsIntoTheParent() {
        FakeDomElement container = document.createElement("main");

        renderer.render(
                Views.div(Views.fragment(Views.span("one"), Views.span("two"))),
                document,
                container);

        FakeDomElement root = assertInstanceOf(FakeDomElement.class, container.children().get(0));
        assertEquals(2, root.children().size());
        assertEquals("span", assertInstanceOf(FakeDomElement.class, root.children().get(0)).tagName());
        assertEquals("span", assertInstanceOf(FakeDomElement.class, root.children().get(1)).tagName());
    }

    @Test
    void supportsEmptyTextAndFragments() {
        FakeDomElement container = document.createElement("main");

        renderer.render(Views.div(Views.text(""), Views.fragment()), document, container);

        FakeDomElement root = assertInstanceOf(FakeDomElement.class, container.children().get(0));
        assertEquals(1, root.children().size());
        assertEquals("", assertInstanceOf(FakeText.class, root.children().get(0)).text());
    }

    @Test
    void rejectsNullArguments() {
        FakeDomElement container = document.createElement("main");
        View view = Views.div();

        assertThrows(NullPointerException.class, () -> renderer.render(null, document, container));
        assertThrows(NullPointerException.class, () -> renderer.render(view, null, container));
        assertThrows(NullPointerException.class, () -> renderer.render(view, document, null));
    }

    @Test
    void createsNodesThroughTheDocument() {
        FakeDomElement container = document.createElement("main");

        renderer.render(Views.div(Views.text("one"), Views.span("two")), document, container);

        List<FakeDomElement> created = document.createdElements();
        assertEquals(3, created.size()); // container + div + span
        assertEquals(2, document.createdTexts().size()); // "one" and "two"
    }
}
