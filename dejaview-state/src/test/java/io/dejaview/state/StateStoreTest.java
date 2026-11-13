package io.dejaview.state;

import io.dejaview.reactivity.Computed;
import io.dejaview.reactivity.Effect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateStoreTest {

    @Test
    void exposesInitialStateAndNamedStores() {
        StateStore<String> store = new StateStore<>("hello");
        assertEquals("hello", store.state());
        assertEquals("", store.name());

        StateStore<Integer> named = StateStore.named("cart/count", 3);
        assertEquals("cart/count", named.name());
        assertEquals(3, named.state());
        assertThrows(IllegalArgumentException.class, () -> StateStore.named("  ", 1));
        assertThrows(NullPointerException.class, () -> new StateStore<>(null));
    }

    @Test
    void setAndUpdateReplaceStateSynchronouslyAndIgnoreEquals() {
        AtomicInteger notifications = new AtomicInteger();
        StateStore<Integer> store = new StateStore<>(1);
        Effect watcher = new Effect(() -> {
            store.state();
            notifications.incrementAndGet();
        });
        assertEquals(1, notifications.get());

        store.set(2);
        assertEquals(2, notifications.get());
        assertEquals(2, store.state());

        store.set(2); // equal: no notification
        assertEquals(2, notifications.get());

        store.update(value -> value + 10);
        assertEquals(12, store.state());
        assertEquals(3, notifications.get());

        assertThrows(NullPointerException.class, () -> store.update(null));
        watcher.dispose();
    }

    @Test
    void multipleComponentsShareTheSameState() {
        StateStore<List<String>> store = new StateStore<>(List.of());
        List<String> viewA = new ArrayList<>();
        List<String> viewB = new ArrayList<>();

        Effect effectA = new Effect(() -> viewA.add(String.join(",", store.state())));
        Effect effectB = new Effect(() -> viewB.add(String.join(",", store.state())));

        store.set(List.of("book", "pen"));

        assertEquals(List.of("", "book,pen"), viewA);
        assertEquals(List.of("", "book,pen"), viewB);

        effectA.dispose();
        effectB.dispose();
    }

    @Test
    void selectReturnsDerivedStateThatRecomputes() {
        StateStore<Integer> store = new StateStore<>(2);
        Computed<Integer> doubled = store.select(value -> value * 2);

        assertEquals(4, doubled.get());
        store.set(21);
        assertEquals(42, doubled.get());

        store.reset();
        assertEquals(2, store.state());
        assertEquals(4, doubled.get());
    }

    @Test
    void selectWithNamedSelectorKeepsDiagnostics() {
        Selector<List<String>, Integer> size = Selector.of("cart/itemCount", List::size);
        assertEquals("cart/itemCount", size.name());

        StateStore<List<String>> store = new StateStore<>(List.of());
        Computed<Integer> count = store.select(size);
        assertEquals(0, count.get());
        store.set(List.of("a", "b", "c"));
        assertEquals(3, count.get());
    }

    @Test
    void crossStoreCompositionCombinesIndependentStores() {
        StateStore<Integer> price = new StateStore<>(10);
        StateStore<Integer> quantity = new StateStore<>(2);

        Computed<Integer> total = Stores.combine(price, quantity, (p, q) -> p * q);
        assertEquals(20, total.get());

        price.set(7);
        assertEquals(14, total.get());
        quantity.set(5);
        assertEquals(35, total.get());
    }

    @Test
    void nestedCompositionComposesDerivedViews() {
        StateStore<Integer> a = new StateStore<>(1);
        StateStore<Integer> b = new StateStore<>(2);
        StateStore<Integer> c = new StateStore<>(3);

        Computed<Integer> ab = Stores.combine(a, b, Integer::sum);
        Computed<Integer> abc = new Computed<>(() -> ab.get() + c.state());
        assertEquals(6, abc.get());

        a.set(10);
        assertEquals(15, abc.get());
    }

    @Test
    void disposeMakesStoreUnreadableAndUnwritable() {
        StateStore<Integer> store = new StateStore<>(1);
        Computed<Integer> doubled = store.select(value -> value * 2);

        store.dispose();
        assertTrue(store.isDisposed());
        assertTrue(doubled.isDisposed());
        assertThrows(IllegalStateException.class, store::state);
        assertThrows(IllegalStateException.class, () -> store.set(2));
        assertThrows(IllegalStateException.class, store::reset);
        assertThrows(IllegalStateException.class, () -> store.select(value -> value));
    }

    @Test
    void disposeIsIdempotent() {
        StateStore<Integer> store = new StateStore<>(1);
        store.dispose();
        store.dispose();
        assertTrue(store.isDisposed());
    }
}
