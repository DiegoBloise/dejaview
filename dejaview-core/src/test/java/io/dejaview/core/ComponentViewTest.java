package io.dejaview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ComponentViewTest {

    @Test
    void resolvesItsContentLazily() {
        View content = Views.div(Views.text("inside"));
        ComponentView component = Views.component(() -> content);

        assertSame(content, component.resolve());
    }

    @Test
    void resolvesContentOnEveryCall() {
        int[] calls = {0};
        ComponentView component = Views.component(() -> {
            calls[0]++;
            return Views.text("value " + calls[0]);
        });

        assertEquals("value 1", ((TextView) component.resolve()).value());
        assertEquals("value 2", ((TextView) component.resolve()).value());
    }

    @Test
    void rejectsNullSupplier() {
        assertThrows(NullPointerException.class, () -> new ComponentView(null));
    }

    @Test
    void rejectsNullResolvedView() {
        ComponentView component = Views.component(() -> null);

        assertThrows(NullPointerException.class, component::resolve);
    }
}
