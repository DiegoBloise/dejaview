package io.dejaview.reactivity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReactiveScopeTest {

    @Test
    void disposesOwnedEffectsAndComputeds() {
        ReactiveScope scope = new ReactiveScope();
        Signal<Integer> signal = new Signal<>(1);
        int[] runs = {0};

        Effect effect = scope.effect(() -> {
            signal.get();
            runs[0]++;
        });
        Computed<Integer> computed = scope.computed(() -> signal.get() * 2);

        assertEquals(2, computed.get());

        scope.dispose();
        signal.set(2);

        assertEquals(1, runs[0]);
        assertTrue(effect.isDisposed());
        assertTrue(computed.isDisposed());
        assertThrows(IllegalStateException.class, computed::get);
    }

    @Test
    void disposesNestedScopes() {
        ReactiveScope parent = new ReactiveScope();
        ReactiveScope child = parent.scope();
        Signal<Integer> signal = new Signal<>(0);
        int[] runs = {0};

        child.effect(() -> {
            signal.get();
            runs[0]++;
        });

        parent.dispose();
        signal.set(1);

        assertEquals(1, runs[0]);
        assertTrue(child.isDisposed());
    }

    @Test
    void disposesResourcesInReverseOrder() {
        ReactiveScope scope = new ReactiveScope();
        List<String> events = new ArrayList<>();

        scope.add(() -> events.add("first"));
        scope.add(() -> events.add("second"));

        scope.dispose();

        assertEquals(List.of("second", "first"), events);
    }

    @Test
    void rejectsNewResourcesAfterDisposal() {
        ReactiveScope scope = new ReactiveScope();
        scope.dispose();

        assertThrows(IllegalStateException.class, () -> scope.effect(() -> {
        }));
        assertThrows(IllegalStateException.class, () -> scope.computed(() -> 1));
        assertThrows(IllegalStateException.class, scope::scope);
        assertThrows(IllegalStateException.class, () -> scope.add(() -> {
        }));
    }

    @Test
    void disposeIsIdempotent() {
        ReactiveScope scope = new ReactiveScope();
        int[] disposals = {0};

        scope.add(() -> disposals[0]++);

        scope.dispose();
        scope.dispose();

        assertEquals(1, disposals[0]);
        assertTrue(scope.isDisposed());
    }

    @Test
    void continuesDisposingAfterFailure() {
        ReactiveScope scope = new ReactiveScope();
        int[] disposals = {0};

        scope.add(() -> disposals[0]++);
        scope.add(() -> {
            throw new IllegalStateException("boom");
        });

        IllegalStateException failure = assertThrows(IllegalStateException.class, scope::dispose);

        assertEquals(1, disposals[0]);
        assertNotNull(failure.getCause());
    }

    @Test
    void computedDisposeStopsObservingDependencies() {
        Signal<Integer> signal = new Signal<>(0);
        Computed<Integer> computed = new Computed<>(() -> signal.get() * 2);

        assertEquals(0, computed.get());

        computed.dispose();

        assertTrue(computed.isDisposed());
        assertThrows(IllegalStateException.class, computed::get);
    }

    @Test
    void rejectsNullResources() {
        ReactiveScope scope = new ReactiveScope();

        assertThrows(NullPointerException.class, () -> scope.effect(null));
        assertThrows(NullPointerException.class, () -> scope.computed(null));
        assertThrows(NullPointerException.class, () -> scope.add(null));
    }
}
