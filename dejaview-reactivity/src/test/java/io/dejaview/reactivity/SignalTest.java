package io.dejaview.reactivity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SignalTest {

    @Test
    void getReturnsInitialValue() {
        Signal<String> signal = new Signal<>("initial");

        assertEquals("initial", signal.get());
    }

    @Test
    void setReplacesValue() {
        Signal<Integer> signal = new Signal<>(1);

        signal.set(2);

        assertEquals(2, signal.get());
    }

    @Test
    void updateAppliesFunction() {
        Signal<Integer> signal = new Signal<>(1);

        signal.update(value -> value + 10);

        assertEquals(11, signal.get());
    }

    @Test
    void setWithEqualValueDoesNotNotify() {
        Signal<Integer> signal = new Signal<>(1);
        int[] runs = {0};

        new Effect(() -> {
            signal.get();
            runs[0]++;
        });

        signal.set(1);

        assertEquals(1, runs[0]);
    }

    @Test
    void updateWithEqualResultDoesNotNotify() {
        Signal<Integer> signal = new Signal<>(1);
        int[] runs = {0};

        new Effect(() -> {
            signal.get();
            runs[0]++;
        });

        signal.update(value -> value);

        assertEquals(1, runs[0]);
    }

    @Test
    void rejectsNullInitialValue() {
        assertThrows(NullPointerException.class, () -> new Signal<>(null));
    }

    @Test
    void rejectsNullSetValue() {
        Signal<String> signal = new Signal<>("value");

        assertThrows(NullPointerException.class, () -> signal.set(null));
    }

    @Test
    void rejectsNullUpdater() {
        Signal<String> signal = new Signal<>("value");

        assertThrows(NullPointerException.class, () -> signal.update(null));
    }
}
