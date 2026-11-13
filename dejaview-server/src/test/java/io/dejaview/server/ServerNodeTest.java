package io.dejaview.server;

import io.dejaview.core.Attribute;
import io.dejaview.core.Property;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Value-type contracts of the server render tree records.
 */
class ServerNodeTest {

    @Test
    void serverElementCopiesChildrenDefensively() {
        java.util.List<ServerNode> children = new java.util.ArrayList<>();
        children.add(new ServerText("a"));
        ServerElement element = new ServerElement("div", List.of(), List.of(), children);

        children.set(0, new ServerText("b"));

        assertEquals(new ServerText("a"), element.children().get(0));
        assertThrows(UnsupportedOperationException.class, () -> element.children().add(new ServerText("c")));
    }

    @Test
    void serverElementRejectsNullChildrenInsideTheList() {
        java.util.List<ServerNode> children = new java.util.ArrayList<>();
        children.add(null);

        assertThrows(NullPointerException.class, () -> new ServerElement("div", List.of(), List.of(), children));
    }

    @Test
    void serverElementRejectsBlankTagName() {
        assertThrows(IllegalArgumentException.class,
                () -> new ServerElement(" ", List.of(), List.of(), List.of()));
    }

    @Test
    void serverElementCarriesAttributesAndPropertiesVerbatim() {
        ServerElement element = new ServerElement(
                "input",
                List.of(new Attribute("type", "text"), new Attribute("id", "name")),
                List.of(new Property("value", "abc")),
                List.of());

        assertEquals(List.of(new Attribute("type", "text"), new Attribute("id", "name")), element.attributes());
        assertEquals(List.of(new Property("value", "abc")), element.properties());
    }

    @Test
    void serverFragmentCopiesChildrenDefensively() {
        ServerFragment fragment = new ServerFragment(List.of(new ServerText("x")));

        assertThrows(UnsupportedOperationException.class, () -> fragment.children().add(new ServerText("y")));
    }

    @Test
    void serverFragmentRejectsNullChildren() {
        java.util.List<ServerNode> children = new java.util.ArrayList<>();
        children.add(null);

        assertThrows(NullPointerException.class, () -> new ServerFragment(children));
    }

    @Test
    void serverTreeCopiesDroppedEventsAndReportsCompleteness() {
        ServerTree complete = new ServerTree(new ServerText("x"), List.of());
        ServerTree dropped = new ServerTree(new ServerText("x"), List.of("click"));

        assertTrue(complete.isComplete());
        assertFalse(dropped.isComplete());
        assertThrows(UnsupportedOperationException.class, () -> dropped.droppedEvents().add("input"));
    }

    @Test
    void serverTreeRejectsNullRoot() {
        assertThrows(NullPointerException.class, () -> new ServerTree(null, List.of()));
    }
}
