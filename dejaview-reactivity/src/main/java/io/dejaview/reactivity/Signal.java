package io.dejaview.reactivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A mutable reactive value.
 *
 * <p>Reading a signal inside an {@link Effect} registers that effect as an
 * observer. Writing a different value notifies the observers synchronously.</p>
 *
 * <p>Signals are not thread-safe. They are intended for the single-threaded
 * browser runtime and for JVM tests that follow the same model.</p>
 *
 * @param <T> value type; {@code null} is not allowed
 */
public final class Signal<T> {

    private final List<Effect> observers = new ArrayList<>();

    private T value;

    /**
     * Creates a signal with an initial value.
     *
     * @throws NullPointerException if {@code initialValue} is {@code null}
     */
    public Signal(T initialValue) {
        this.value = Objects.requireNonNull(initialValue, "initialValue");
    }

    /**
     * Returns the current value and, when called inside an effect, registers a
     * dependency on this signal.
     */
    public T get() {
        ReactiveContext.track(this);
        return value;
    }

    /**
     * Replaces the value. If the new value is equal to the current one, nothing
     * happens: no observer is notified.
     *
     * @throws NullPointerException if {@code newValue} is {@code null}
     * @throws IllegalStateException if an effect tries to write to a signal it
     *         depends on
     */
    public void set(T newValue) {
        Objects.requireNonNull(newValue, "newValue");

        Effect currentEffect = ReactiveContext.currentEffect();
        if (currentEffect != null && currentEffect.dependsOn(this)) {
            throw new IllegalStateException(
                    "An effect cannot write to a signal it depends on");
        }

        if (Objects.equals(value, newValue)) {
            return;
        }

        value = newValue;
        notifyObservers();
    }

    /**
     * Replaces the value with the result of {@code updater}.
     *
     * @throws NullPointerException if {@code updater} is {@code null}
     */
    public void update(UnaryOperator<T> updater) {
        Objects.requireNonNull(updater, "updater");
        set(updater.apply(value));
    }

    void addObserver(Effect effect) {
        if (!observers.contains(effect)) {
            observers.add(effect);
        }
    }

    void removeObserver(Effect effect) {
        observers.remove(effect);
    }

    private void notifyObservers() {
        // Copy so an effect can dispose itself or another effect while running.
        for (Effect effect : new ArrayList<>(observers)) {
            effect.invalidate();
        }
    }
}
