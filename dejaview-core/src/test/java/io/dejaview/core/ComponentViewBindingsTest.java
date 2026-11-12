package io.dejaview.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentViewBindingsTest {

    @Test
    void inputStoresValueAndIsImmutable() {
        ComponentView base = Views.componentFactory(CounterProbe::new);
        ComponentView bound = base.input("username", "Ada").input("level", 3);

        assertTrue(base.inputs().isEmpty());
        assertEquals("Ada", bound.inputs().get("username"));
        assertEquals(3, bound.inputs().get("level"));
        assertNotSame(base, bound);
    }

    @Test
    void inputRejectsBlankNameAndNullValue() {
        ComponentView base = Views.componentFactory(CounterProbe::new);

        assertThrows(IllegalArgumentException.class, () -> base.input(" ", "x"));
        assertThrows(NullPointerException.class, () -> base.input("name", null));
    }

    @Test
    void inputOverwritesTheSameNameInOrder() {
        ComponentView bound = Views.componentFactory(CounterProbe::new)
                .input("username", "Ada")
                .input("username", "Grace");

        assertEquals("Grace", bound.inputs().get("username"));
    }

    @Test
    void onAccumulatesListenersInRegistrationOrder() {
        AtomicInteger calls = new AtomicInteger();
        List<String> received = new ArrayList<>();

        ComponentView bound = Views.componentFactory(CounterProbe::new)
                .on("select", value -> {
                    calls.incrementAndGet();
                    received.add("first:" + value);
                })
                .on("select", value -> {
                    calls.incrementAndGet();
                    received.add("second:" + value);
                });

        for (Consumer<?> consumer : bound.outputs().get("select")) {
            dispatch(consumer, "Ada");
        }

        assertEquals(2, calls.get());
        assertEquals(List.of("first:Ada", "second:Ada"), received);
    }

    @Test
    void onRejectsBlankNameAndNullConsumer() {
        ComponentView base = Views.componentFactory(CounterProbe::new);

        assertThrows(IllegalArgumentException.class, () -> base.on("", value -> {
        }));
        assertThrows(NullPointerException.class, () -> base.on("select", null));
    }

    @Test
    void defaultSlotCollectsViewsInProjectionOrder() {
        ComponentView bound = Views.componentFactory(CounterProbe::new)
                .slot(Views.text("body"))
                .slot(Views.text("tail"));

        List<View> projected = bound.slots().get(ComponentBindings.DEFAULT_SLOT);
        assertEquals(2, projected.size());
        assertEquals(new TextView("body"), projected.get(0));
        assertEquals(new TextView("tail"), projected.get(1));
    }

    @Test
    void namedSlotCollectsViewsUnderItsName() {
        ComponentView bound = Views.componentFactory(CounterProbe::new)
                .slot("header", Views.text("head"))
                .slot("footer", Views.text("foot"));

        assertEquals(1, bound.slots().get("header").size());
        assertEquals(1, bound.slots().get("footer").size());
    }

    @Test
    void namedSlotRejectsBlankNameAndNullViews() {
        ComponentView base = Views.componentFactory(CounterProbe::new);

        assertThrows(IllegalArgumentException.class, () -> base.slot(" ", Views.text("x")));
        assertThrows(NullPointerException.class, () -> base.slot("header", (View) null));
        assertThrows(NullPointerException.class, () -> base.slot("header", (List<View>) null));
    }

    @Test
    void bindingsMatchViewContents() {
        ComponentView bound = Views.componentFactory(CounterProbe::new)
                .input("username", "Ada")
                .slot(Views.text("body"))
                .on("select", value -> {
                });

        ComponentBindings bindings = bound.bindings();

        assertEquals(bound.inputs(), bindings.inputs());
        assertEquals(bound.slots(), bindings.slots());
        assertEquals(bound.outputs(), bindings.outputs());
    }

    @Test
    void keyIsCarriedByTheView() {
        ComponentView base = Views.componentFactory(CounterProbe::new);

        assertEquals(null, base.key());
        assertEquals("card-1", base.key("card-1").key());
        assertThrows(IllegalArgumentException.class, () -> base.key(" "));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void dispatch(Consumer<?> consumer, Object value) {
        ((Consumer) consumer).accept(value);
    }

    private static final class CounterProbe extends AbstractComponent {

        @Override
        public View render() {
            return Views.div();
        }
    }
}
