package io.dejaview.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementViewTest {

    @Test
    void builderMethodsDoNotMutateTheOriginal() {
        ElementView base = Views.div();

        ElementView decorated = base.id("app").child(Views.text("Hello"));

        assertTrue(base.attributes().isEmpty());
        assertTrue(base.children().isEmpty());
        assertEquals(1, decorated.attributes().size());
        assertEquals(1, decorated.children().size());
    }

    @Test
    void addChildrenReturnsSameInstanceForEmptyInput() {
        ElementView view = Views.div();

        assertSame(view, view.addChildren());
    }

    @Test
    void rejectsBlankTagName() {
        assertThrows(IllegalArgumentException.class, () -> Views.element(" "));
    }

    @Test
    void rejectsNullChild() {
        ElementView view = Views.div();

        assertThrows(NullPointerException.class, () -> view.child(null));
    }

    @Test
    void rejectsDuplicateAttributes() {
        ElementView view = Views.div().id("first");

        assertThrows(IllegalArgumentException.class, () -> view.id("second"));
    }

    @Test
    void rejectsBlankAttributeName() {
        assertThrows(IllegalArgumentException.class, () -> Views.div().attr(" ", "value"));
    }

    @Test
    void joinsClassNamesWithSpaces() {
        ElementView view = Views.div().classes("card", "card--wide");

        assertEquals(List.of(new Attribute("class", "card card--wide")), view.attributes());
    }

    @Test
    void rejectsEmptyClassList() {
        assertThrows(IllegalArgumentException.class, () -> Views.div().classes());
    }

    @Test
    void registersEventBindingsInOrder() {
        AtomicInteger calls = new AtomicInteger();
        ElementView view = Views.button("Go")
                .onClick(calls::incrementAndGet)
                .on("click", () -> calls.addAndGet(10));

        assertEquals(2, view.events().size());
        assertEquals("click", view.events().get(0).type());
        view.events().get(0).handler().run();
        view.events().get(1).handler().run();
        assertEquals(11, calls.get());
    }

    @Test
    void copiesMutableInputCollections() {
        List<Attribute> attributes = new ArrayList<>(List.of(new Attribute("id", "app")));
        List<View> children = new ArrayList<>(List.of(Views.text("one")));
        List<EventBinding> events = new ArrayList<>(List.of(new EventBinding("click", () -> {
        })));

        ElementView view = new ElementView("div", attributes, children, events);

        attributes.clear();
        children.clear();
        events.clear();

        assertEquals(1, view.attributes().size());
        assertEquals(1, view.children().size());
        assertEquals(1, view.events().size());
        assertThrows(UnsupportedOperationException.class, () -> view.children().add(Views.text("two")));
    }

    @Test
    void fragmentCopiesChildren() {
        List<View> children = new ArrayList<>(List.of(Views.text("one")));

        FragmentView fragment = new FragmentView(children);
        children.clear();

        assertEquals(1, fragment.children().size());
    }

    @Test
    void convenienceFactoriesCreateTextChildren() {
        assertEquals(List.of(new TextView("Title")), Views.h1("Title").children());
        assertEquals(List.of(new TextView("Body")), Views.p("Body").children());
        assertEquals(List.of(new TextView("Go")), Views.button("Go").children());
    }
}
