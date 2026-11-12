package io.dejaview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ComponentViewTest {

    @Test
    void createsItsComponentLazily() {
        View content = Views.div(Views.text("inside"));
        AtomicInteger created = new AtomicInteger();
        ComponentView view = Views.componentFactory(() -> {
            created.incrementAndGet();
            return () -> content;
        });

        assertEquals(0, created.get());

        Component component = view.create();

        assertEquals(1, created.get());
        assertSame(content, component.render());
    }

    @Test
    void functionalComponentsResolveContentOnEveryRender() {
        int[] calls = {0};
        ComponentView view = Views.component(() -> {
            calls[0]++;
            return Views.text("value " + calls[0]);
        });

        Component component = view.create();

        assertEquals("value 1", ((TextView) component.render()).value());
        assertEquals("value 2", ((TextView) component.render()).value());
    }

    @Test
    void instanceViewsAlwaysReturnTheSameComponent() {
        Component instance = () -> Views.text("shared");
        ComponentView view = ComponentView.of(instance);

        assertSame(instance, view.create());
        assertSame(instance, view.create());
    }

    @Test
    void factoriesCreateIndependentInstances() {
        ComponentView view = Views.componentFactory(() -> new Component() {
            @Override
            public View render() {
                return Views.text("value");
            }
        });

        assertNotSame(view.create(), view.create());
    }

    @Test
    void rejectsNullFactory() {
        assertThrows(NullPointerException.class, () -> new ComponentView(null));
        assertThrows(NullPointerException.class, () -> ComponentView.of(null));
        assertThrows(NullPointerException.class, () -> Views.componentFactory(null));
    }

    @Test
    void rejectsFactoryReturningNull() {
        ComponentView view = Views.componentFactory(() -> null);

        assertThrows(NullPointerException.class, view::create);
    }

    @Test
    void rejectsNullRenderedView() {
        ComponentView view = Views.component(() -> null);
        Component component = view.create();

        assertThrows(NullPointerException.class, component::render);
    }
}
