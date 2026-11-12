package io.dejaview.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentBindingsTest {

    @Test
    void emptyBindingsExposeNoInputsSlotsOrOutputs() {
        ComponentBindings empty = ComponentBindings.empty();

        assertTrue(empty.inputs().isEmpty());
        assertTrue(empty.slots().isEmpty());
        assertTrue(empty.outputs().isEmpty());
    }

    @Test
    void constructorRejectsNullKeysAndValues() {
        assertThrows(NullPointerException.class,
                () -> new ComponentBindings(
                        java.util.Collections.singletonMap("name", null), Map.of(), Map.of()));
        assertThrows(NullPointerException.class,
                () -> new ComponentBindings(
                        Map.of(),
                        java.util.Collections.singletonMap(ComponentBindings.DEFAULT_SLOT, null),
                        Map.of()));
        assertThrows(NullPointerException.class,
                () -> new ComponentBindings(
                        Map.of(),
                        Map.of(),
                        java.util.Collections.singletonMap("out", null)));
    }

    @Test
    void slotsAreCopiedDefensively() {
        java.util.List<View> views = new java.util.ArrayList<>();
        views.add(Views.text("hello"));
        ComponentBindings bindings = new ComponentBindings(Map.of(), Map.of("header", views), Map.of());

        views.clear();

        assertEquals(1, bindings.slots().get("header").size());
        assertThrows(UnsupportedOperationException.class,
                () -> bindings.slots().get("header").add(Views.text("late")));
    }

    @Test
    void outputsKeepRegistrationOrder() {
        Consumer<?> first = value -> {
        };
        Consumer<?> second = value -> {
        };
        ComponentBindings bindings = new ComponentBindings(
                Map.of(), Map.of(), Map.of("select", List.of(first, second)));

        assertEquals(List.of(first, second), bindings.outputs().get("select"));
    }
}
