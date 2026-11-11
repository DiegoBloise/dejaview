package io.dejaview.reactivity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EffectTest {

    @Test
    void runsImmediately() {
        int[] runs = {0};

        new Effect(() -> runs[0]++);

        assertEquals(1, runs[0]);
    }

    @Test
    void rerunsWhenDependencyChanges() {
        Signal<Integer> signal = new Signal<>(0);
        int[] runs = {0};

        new Effect(() -> {
            signal.get();
            runs[0]++;
        });

        signal.set(1);
        signal.set(2);

        assertEquals(3, runs[0]);
    }

    @Test
    void tracksMultipleSignals() {
        Signal<Integer> first = new Signal<>(0);
        Signal<Integer> second = new Signal<>(0);
        int[] runs = {0};

        new Effect(() -> {
            first.get();
            second.get();
            runs[0]++;
        });

        first.set(1);
        second.set(1);

        assertEquals(3, runs[0]);
    }

    @Test
    void ignoresUnrelatedSignals() {
        Signal<Integer> tracked = new Signal<>(0);
        Signal<Integer> unrelated = new Signal<>(0);
        int[] runs = {0};

        new Effect(() -> {
            tracked.get();
            runs[0]++;
        });

        unrelated.set(1);

        assertEquals(1, runs[0]);
    }

    @Test
    void recollectsDependenciesOnEveryRun() {
        Signal<Boolean> useFirst = new Signal<>(true);
        Signal<Integer> first = new Signal<>(0);
        Signal<Integer> second = new Signal<>(0);
        int[] runs = {0};

        new Effect(() -> {
            runs[0]++;
            if (useFirst.get()) {
                first.get();
            } else {
                second.get();
            }
        });

        first.set(1);
        assertEquals(2, runs[0]);

        useFirst.set(false);
        assertEquals(3, runs[0]);

        first.set(2);
        assertEquals(3, runs[0]);

        second.set(1);
        assertEquals(4, runs[0]);
    }

    @Test
    void disposeStopsReruns() {
        Signal<Integer> signal = new Signal<>(0);
        int[] runs = {0};
        Effect effect = new Effect(() -> {
            signal.get();
            runs[0]++;
        });

        effect.dispose();
        signal.set(1);

        assertEquals(1, runs[0]);
        assertTrue(effect.isDisposed());
    }

    @Test
    void closeDisposes() {
        Signal<Integer> signal = new Signal<>(0);
        int[] runs = {0};
        Effect effect = new Effect(() -> {
            signal.get();
            runs[0]++;
        });

        effect.close();
        signal.set(1);

        assertEquals(1, runs[0]);
        assertTrue(effect.isDisposed());
    }

    @Test
    void disposeCanBeCalledMoreThanOnce() {
        Effect effect = new Effect(() -> {
        });

        effect.dispose();
        effect.dispose();

        assertTrue(effect.isDisposed());
    }

    @Test
    void multipleEffectsObserveTheSameSignal() {
        Signal<Integer> signal = new Signal<>(0);
        int[] firstRuns = {0};
        int[] secondRuns = {0};

        new Effect(() -> {
            signal.get();
            firstRuns[0]++;
        });
        new Effect(() -> {
            signal.get();
            secondRuns[0]++;
        });

        signal.set(1);

        assertEquals(2, firstRuns[0]);
        assertEquals(2, secondRuns[0]);
    }

    @Test
    void rejectsNullBody() {
        assertThrows(NullPointerException.class, () -> new Effect(null));
    }

    @Test
    void rejectsWriteToOwnDependency() {
        Signal<Integer> signal = new Signal<>(0);

        assertThrows(IllegalStateException.class, () -> new Effect(() -> {
            int value = signal.get();
            signal.set(value + 1);
        }));

        // The failed effect must not remain registered.
        signal.set(10);
        assertEquals(10, signal.get());
    }

    @Test
    void allowsWriteToUnrelatedSignal() {
        Signal<Integer> source = new Signal<>(0);
        Signal<Integer> target = new Signal<>(0);
        Effect effect = new Effect(() -> target.set(source.get() + 1));

        assertEquals(1, target.get());

        source.set(5);

        assertEquals(6, target.get());
        assertFalse(effect.isDisposed());
    }

    @Test
    void disposedEffectDoesNotReRegisterFromInsideItsBody() {
        Signal<Integer> first = new Signal<>(0);
        Signal<Integer> second = new Signal<>(0);
        int[] runs = {0};
        Effect[] holder = new Effect[1];

        holder[0] = new Effect(() -> {
            runs[0]++;
            first.get();
            if (holder[0] != null) {
                holder[0].dispose();
            }
            second.get();
        });

        second.set(1);
        first.set(1);

        assertEquals(2, runs[0]);
        assertTrue(holder[0].isDisposed());
    }
}
