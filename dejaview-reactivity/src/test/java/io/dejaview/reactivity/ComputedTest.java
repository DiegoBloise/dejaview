package io.dejaview.reactivity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ComputedTest {

    @Test
    void isLazy() {
        int[] computations = {0};
        Computed<Integer> computed = new Computed<>(() -> {
            computations[0]++;
            return 42;
        });

        assertEquals(0, computations[0]);
        assertEquals(42, computed.get());
        assertEquals(1, computations[0]);
    }

    @Test
    void cachesValue() {
        int[] computations = {0};
        Computed<Integer> computed = new Computed<>(() -> {
            computations[0]++;
            return 42;
        });

        assertEquals(42, computed.get());
        assertEquals(42, computed.get());

        assertEquals(1, computations[0]);
    }

    @Test
    void recomputesAfterDependencyChange() {
        Signal<Integer> source = new Signal<>(1);
        int[] computations = {0};
        Computed<Integer> doubled = new Computed<>(() -> {
            computations[0]++;
            return source.get() * 2;
        });

        assertEquals(2, doubled.get());

        source.set(2);

        assertEquals(4, doubled.get());
        assertEquals(2, computations[0]);
    }

    @Test
    void recomputesOnceAfterMultipleChanges() {
        Signal<Integer> source = new Signal<>(1);
        int[] computations = {0};
        Computed<Integer> doubled = new Computed<>(() -> {
            computations[0]++;
            return source.get() * 2;
        });

        assertEquals(2, doubled.get());

        source.set(2);
        source.set(3);

        assertEquals(6, doubled.get());
        assertEquals(2, computations[0]);
    }

    @Test
    void ignoresUnrelatedSignals() {
        Signal<Integer> tracked = new Signal<>(1);
        Signal<Integer> unrelated = new Signal<>(1);
        int[] computations = {0};
        Computed<Integer> computed = new Computed<>(() -> {
            computations[0]++;
            return tracked.get();
        });

        assertEquals(1, computed.get());

        unrelated.set(2);

        assertEquals(1, computed.get());
        assertEquals(1, computations[0]);
    }

    @Test
    void notifiesEffectWhenValueChanges() {
        Signal<Integer> source = new Signal<>(1);
        Computed<Integer> doubled = new Computed<>(() -> source.get() * 2);
        List<Integer> seen = new ArrayList<>();

        new Effect(() -> seen.add(doubled.get()));

        assertEquals(List.of(2), seen);

        source.set(2);

        assertEquals(List.of(2, 4), seen);
    }

    @Test
    void computedCanDependOnComputed() {
        Signal<Integer> base = new Signal<>(1);
        Computed<Integer> doubled = new Computed<>(() -> base.get() * 2);
        Computed<Integer> quadrupled = new Computed<>(() -> doubled.get() * 2);
        List<Integer> seen = new ArrayList<>();

        new Effect(() -> seen.add(quadrupled.get()));

        assertEquals(List.of(4), seen);

        base.set(2);

        assertEquals(List.of(4, 8), seen);
    }

    @Test
    void replacesDependenciesOnEachRun() {
        Signal<Boolean> useFirst = new Signal<>(true);
        Signal<Integer> first = new Signal<>(1);
        Signal<Integer> second = new Signal<>(2);
        int[] computations = {0};
        Computed<Integer> computed = new Computed<>(() -> {
            computations[0]++;
            return useFirst.get() ? first.get() : second.get();
        });

        assertEquals(1, computed.get());

        useFirst.set(false);
        assertEquals(2, computed.get());

        first.set(10);
        assertEquals(2, computed.get());

        second.set(20);
        assertEquals(20, computed.get());

        assertEquals(3, computations[0]);
    }

    @Test
    void sharesCacheAcrossEffects() {
        Signal<Integer> source = new Signal<>(1);
        int[] computations = {0};
        Computed<Integer> doubled = new Computed<>(() -> {
            computations[0]++;
            return source.get() * 2;
        });

        new Effect(() -> doubled.get());
        new Effect(() -> doubled.get());

        assertEquals(1, computations[0]);
    }

    @Test
    void effectDoesNotDependDirectlyOnComputedSources() {
        Signal<Integer> source = new Signal<>(1);
        Computed<Integer> doubled = new Computed<>(() -> source.get() * 2);
        int[] runs = {0};

        new Effect(() -> {
            doubled.get();
            runs[0]++;
        });

        source.set(2);

        assertEquals(2, runs[0]);
    }

    @Test
    void detectsCircularDependency() {
        @SuppressWarnings("unchecked")
        Computed<Integer>[] computeds = new Computed[2];

        computeds[0] = new Computed<>(() -> computeds[1].get() + 1);
        computeds[1] = new Computed<>(() -> computeds[0].get() + 1);

        assertThrows(IllegalStateException.class, () -> computeds[0].get());
    }

    @Test
    void rejectsNullComputation() {
        assertThrows(NullPointerException.class, () -> new Computed<>(null));
    }

    @Test
    void rejectsNullComputedValue() {
        Computed<String> computed = new Computed<>(() -> null);

        assertThrows(NullPointerException.class, computed::get);
    }
}
