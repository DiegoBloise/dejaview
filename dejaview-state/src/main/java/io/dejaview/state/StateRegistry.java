package io.dejaview.state;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A composition-root registry for application-scoped stores.
 *
 * <p>The registry is an anti-global: it is an ordinary object created by the
 * application (typically next to the injector or the root component) and
 * passed to the code that needs stores. Stores are keyed by name, created
 * lazily on first {@link #getOrCreate(String, Supplier)}, and disposed
 * together with {@link #disposeAll()} — usually when the root view is
 * destroyed.</p>
 *
 * <p>Stores obtained from the registry are shared by construction; nothing
 * prevents a component from creating a private store and passing it down
 * instead. Prefer the registry only for state that genuinely outlives one
 * view.</p>
 *
 * <p>Single-threaded, like the rest of the reactivity primitives.</p>
 */
public final class StateRegistry implements AutoCloseable {

    private final Map<String, StateStore<?>> stores = new LinkedHashMap<>();
    private boolean disposed;

    /**
     * Returns the store registered under {@code name}, creating it with
     * {@code initialState} on first use.
     *
     * <p>Generic type parameters are erased, so the registry cannot verify
     * that two callers request the same state type for the same name; callers
     * must use a consistent type per key (the same rule as attribute maps in
     * the servlet API, and as token-based DI elsewhere in DejaView).</p>
     *
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws NullPointerException if {@code initialState} is null
     * @throws IllegalStateException if the registry is disposed
     */
    @SuppressWarnings("unchecked")
    public <T> StateStore<T> getOrCreate(String name, Supplier<T> initialState) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(initialState, "initialState");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        ensureActive();
        StateStore<?> existing = stores.get(name);
        if (existing != null) {
            return (StateStore<T>) existing;
        }
        T initial = Objects.requireNonNull(initialState.get(), "initialState supplier returned null");
        StateStore<T> created = StateStore.named(name, initial);
        stores.put(name, created);
        return created;
    }

    /**
     * Returns the store registered under {@code name}, if any.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<StateStore<T>> find(String name) {
        Objects.requireNonNull(name, "name");
        ensureActive();
        return Optional.ofNullable((StateStore<T>) stores.get(name));
    }

    /**
     * Whether a store is registered under {@code name}.
     */
    public boolean has(String name) {
        Objects.requireNonNull(name, "name");
        ensureActive();
        return stores.containsKey(name);
    }

    /**
     * Number of registered stores.
     */
    public int size() {
        return stores.size();
    }

    /**
     * Disposes every registered store in reverse creation order and clears the
     * registry. Calling this method more than once has no additional effect.
     */
    public void disposeAll() {
        if (disposed) {
            return;
        }
        disposed = true;
        StateStore<?>[] all = stores.values().toArray(new StateStore<?>[0]);
        for (int index = all.length - 1; index >= 0; index--) {
            all[index].dispose();
        }
        stores.clear();
    }

    /**
     * Returns whether {@link #disposeAll()} has been called.
     */
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void close() {
        disposeAll();
    }

    private void ensureActive() {
        if (disposed) {
            throw new IllegalStateException("State registry is disposed");
        }
    }
}
