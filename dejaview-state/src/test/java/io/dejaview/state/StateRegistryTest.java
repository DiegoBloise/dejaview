package io.dejaview.state;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateRegistryTest {

    @Test
    void createsStoresLazilyAndReusesThem() {
        StateRegistry registry = new StateRegistry();

        Optional<StateStore<List<String>>> missing = registry.find("cart");
        assertTrue(missing.isEmpty());
        assertFalse(registry.has("cart"));
        assertEquals(0, registry.size());

        StateStore<List<String>> first = registry.getOrCreate("cart", List::of);
        StateStore<List<String>> second = registry.getOrCreate("cart", () -> List.of("ignored"));

        assertSame(first, second);
        assertEquals(1, registry.size());
        assertTrue(registry.has("cart"));
        assertEquals("cart", first.name());

        first.set(List.of("book"));
        assertEquals(List.of("book"), second.state());
    }

    @Test
    void independentNamesGetIndependentStores() {
        StateRegistry registry = new StateRegistry();
        StateStore<List<String>> cart = registry.getOrCreate("cart", List::of);
        StateStore<Integer> counter = registry.getOrCreate("counter", () -> 0);

        cart.set(List.of("pen"));
        counter.set(41);

        assertEquals(List.of("pen"), cart.state());
        assertEquals(41, counter.state());
        assertEquals(2, registry.size());
    }

    @Test
    void disposeAllDisposesEveryRegisteredStoreAndClearsLookups() {
        StateRegistry registry = new StateRegistry();
        StateStore<String> a = registry.getOrCreate("a", () -> "A");
        StateStore<String> b = registry.getOrCreate("b", () -> "B");

        registry.disposeAll();
        assertTrue(registry.isDisposed());
        assertTrue(a.isDisposed());
        assertTrue(b.isDisposed());
        assertEquals(0, registry.size());

        assertThrows(IllegalStateException.class, () -> registry.getOrCreate("c", () -> "C"));
        assertThrows(IllegalStateException.class, () -> registry.find("a"));
        assertThrows(IllegalStateException.class, () -> registry.has("a"));

        registry.disposeAll(); // idempotent
    }

    @Test
    void rejectsBlankNamesAndNullInitials() {
        StateRegistry registry = new StateRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.getOrCreate("  ", List::of));
        assertThrows(NullPointerException.class, () -> registry.getOrCreate("cart", null));
        assertThrows(NullPointerException.class, () -> registry.getOrCreate("cart", () -> null));
    }
}
