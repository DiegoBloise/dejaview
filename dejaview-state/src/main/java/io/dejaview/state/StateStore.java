package io.dejaview.state;

import io.dejaview.reactivity.Computed;
import io.dejaview.reactivity.Signal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * An application-level store: a shared, observable holder for one piece of
 * state.
 *
 * <p>A store owns a {@link Signal} so every component, effect or computed
 * that reads {@link #state()} or a projection from {@link #select(Function)}
 * observes the same value. Writes are synchronous and reject nulls. Reads are
 * tracked: calling {@code state()} inside an effect or render function
 * registers a dependency, exactly like reading a signal.</p>
 *
 * <p>Stores are ordinary objects. Create them at the composition root (or in
 * a {@link StateRegistry}), pass them to the components that need them, and
 * dispose them when the owning scope ends. A store deliberately owns no
 * subscriptions of its own; {@link #dispose()} disposes computeds created by
 * this store through {@code select(...)} and then refuses new writes.</p>
 *
 * <p>Stores are single-threaded and follow the same model as signals.</p>
 *
 * @param <T> state type; {@code null} is not allowed
 */
public final class StateStore<T> {

    private final Signal<T> signal;
    private final T initialState;
    private final String name;
    private final List<Computed<?>> selections = new ArrayList<>();
    private boolean disposed;

    /**
     * Creates an unnamed store.
     *
     * @throws NullPointerException if {@code initialState} is null
     */
    public StateStore(T initialState) {
        this("", initialState);
    }

    private StateStore(String name, T initialState) {
        this.initialState = Objects.requireNonNull(initialState, "initialState");
        this.name = name;
        this.signal = new Signal<>(initialState);
    }

    /**
     * Creates a store with a stable name for diagnostics.
     *
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws NullPointerException if {@code initialState} is null
     */
    public static <T> StateStore<T> named(String name, T initialState) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return new StateStore<>(name, initialState);
    }

    /**
     * Returns the current state and registers a reactive dependency.
     */
    public T state() {
        ensureActive();
        return signal.get();
    }

    /**
     * Returns a lazily recomputed projection of the state.
     *
     * <p>The returned computed is owned by this store and is disposed by
     * {@link #dispose()}. Callers may dispose it earlier with
     * {@code compute.dispose()}.</p>
     */
    public <R> Computed<R> select(Function<T, R> projector) {
        Objects.requireNonNull(projector, "projector");
        return track(new Computed<>(() -> projector.apply(signal.get())));
    }

    /**
     * Returns a lazily recomputed projection built from a named selector.
     *
     * <p>Equivalent to {@code select(selector::select)}; the selector name is
     * available through {@link Selector#name()} for diagnostics.</p>
     */
    public <R> Computed<R> select(Selector<T, R> selector) {
        Objects.requireNonNull(selector, "selector");
        return select(selector::select);
    }

    /**
     * Replaces the state. Equal values are ignored; writes are synchronous.
     *
     * @throws NullPointerException if {@code newState} is null
     * @throws IllegalStateException if the store is disposed
     */
    public void set(T newState) {
        ensureActive();
        signal.set(Objects.requireNonNull(newState, "newState"));
    }

    /**
     * Replaces the state with the result of {@code updater}.
     *
     * @throws NullPointerException if {@code updater} is null
     * @throws IllegalStateException if the store is disposed
     */
    public void update(UnaryOperator<T> updater) {
        Objects.requireNonNull(updater, "updater");
        set(updater.apply(signal.get()));
    }

    /**
     * Restores the state passed at creation. Disposing and recreating a store
     * is also fine; this exists for local, predictable resets.
     */
    public void reset() {
        ensureActive();
        signal.set(initialState);
    }

    /**
     * Returns the state passed at creation.
     */
    public T initialState() {
        return initialState;
    }

    /**
     * Returns the store name, or {@code ""} for unnamed stores.
     */
    public String name() {
        return name;
    }

    /**
     * Disposes every computed created by {@link #select(Function)} and marks
     * the store read-only afterwards. Observers registered elsewhere keep
     * their last known values; they simply stop receiving updates from this
     * store. Calling this method more than once has no additional effect.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (Computed<?> selection : selections) {
            selection.dispose();
        }
        selections.clear();
    }

    /**
     * Returns whether {@link #dispose()} has been called.
     */
    public boolean isDisposed() {
        return disposed;
    }

    private <R> Computed<R> track(Computed<R> computed) {
        if (disposed) {
            computed.dispose();
            throw new IllegalStateException("Store is disposed");
        }
        selections.add(computed);
        return computed;
    }

    private void ensureActive() {
        if (disposed) {
            throw new IllegalStateException("Store is disposed");
        }
    }
}
