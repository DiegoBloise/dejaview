package io.dejaview.reactivity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReactiveTest {

    @Test
    void batchDefersAndCoalescesEffectRuns() {
        Signal<Integer> signal = new Signal<>(0);
        int[] runs = {0};
        new Effect(() -> {
            signal.get();
            runs[0]++;
        });

        Reactive.batch(() -> {
            signal.set(1);
            signal.set(2);
            signal.set(3);
            assertEquals(1, runs[0]);
        });

        assertEquals(2, runs[0]);
        assertEquals(3, signal.get());
    }

    @Test
    void nestedBatchesFlushOnlyAtOutermostBoundary() {
        Signal<Integer> signal = new Signal<>(0);
        int[] runs = {0};
        new Effect(() -> {
            signal.get();
            runs[0]++;
        });

        Reactive.batch(() -> {
            signal.set(1);
            Reactive.batch(() -> signal.set(2));
            assertEquals(1, runs[0]);
        });

        assertEquals(2, runs[0]);
        assertEquals(2, signal.get());
    }

    @Test
    void effectsRunInInvalidationOrder() {
        Signal<Integer> first = new Signal<>(0);
        Signal<Integer> second = new Signal<>(0);
        List<String> order = new ArrayList<>();

        new Effect(() -> {
            first.get();
            order.add("first");
        });
        new Effect(() -> {
            second.get();
            order.add("second");
        });
        order.clear();

        Reactive.batch(() -> {
            second.set(1);
            first.set(1);
        });

        assertEquals(List.of("second", "first"), order);
    }

    @Test
    void disposedEffectIsSkippedInBatch() {
        Signal<Integer> signal = new Signal<>(0);
        int[] runs = {0};
        Effect effect = new Effect(() -> {
            signal.get();
            runs[0]++;
        });

        Reactive.batch(() -> {
            signal.set(1);
            effect.dispose();
        });

        assertEquals(1, runs[0]);
    }

    @Test
    void detectsInfiniteReactiveLoop() {
        Signal<Boolean> active = new Signal<>(false);
        Signal<Integer> first = new Signal<>(0);
        Signal<Integer> second = new Signal<>(0);

        new Effect(() -> {
            if (active.get()) {
                second.set(first.get() + 1);
            }
        });
        new Effect(() -> {
            if (active.get()) {
                first.set(second.get() + 1);
            }
        });

        assertThrows(IllegalStateException.class, () -> active.set(true));
    }

    @Test
    void failedEffectDoesNotCorruptScheduler() {
        Signal<Integer> trigger = new Signal<>(0);
        Signal<Integer> source = new Signal<>(0);
        List<Integer> seen = new ArrayList<>();

        new Effect(() -> {
            if (trigger.get() == 1) {
                throw new IllegalStateException("boom");
            }
        });
        new Effect(() -> seen.add(source.get()));

        assertThrows(IllegalStateException.class, () -> Reactive.batch(() -> {
            trigger.set(1);
            source.set(1);
        }));

        source.set(2);

        assertEquals(List.of(0, 2), seen);
    }

    @Test
    void runsCleanupBeforeReexecution() {
        Signal<Integer> signal = new Signal<>(0);
        List<String> events = new ArrayList<>();

        new Effect(() -> {
            int value = signal.get();
            events.add("run " + value);
            Reactive.onCleanup(() -> events.add("cleanup " + value));
        });

        signal.set(1);

        assertEquals(List.of("run 0", "cleanup 0", "run 1"), events);
    }

    @Test
    void runsCleanupOnDispose() {
        Signal<Integer> signal = new Signal<>(0);
        List<String> events = new ArrayList<>();
        Effect effect = new Effect(() -> {
            signal.get();
            Reactive.onCleanup(() -> events.add("cleanup"));
        });

        effect.dispose();

        assertEquals(List.of("cleanup"), events);
    }

    @Test
    void rejectsCleanupOutsideEffect() {
        assertThrows(IllegalStateException.class, () -> Reactive.onCleanup(() -> {
        }));
    }

    @Test
    void rejectsNullBatchBody() {
        assertThrows(NullPointerException.class, () -> Reactive.batch(null));
    }

    @Test
    void reportsBatchState() {
        assertFalse(Reactive.isBatching());
        Reactive.batch(() -> assertTrue(Reactive.isBatching()));
        assertFalse(Reactive.isBatching());
    }
}
