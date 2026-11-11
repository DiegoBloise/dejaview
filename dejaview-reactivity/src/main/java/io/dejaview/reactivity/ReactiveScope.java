package io.dejaview.reactivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Owns effects, computed values and child scopes so they can be disposed
 * together.
 *
 * <p>A scope is the minimal ownership model for reactive resources. Effects and
 * computeds created through a scope are disposed when the scope is disposed.
 * Resources added with {@link #add(AutoCloseable)} are disposed too, in reverse
 * creation order.</p>
 *
 * <p>Scopes are not thread-safe and must be used from the same single thread as
 * the rest of the reactivity primitives.</p>
 */
public final class ReactiveScope implements AutoCloseable {

    private final List<AutoCloseable> owned = new ArrayList<>();

    private boolean disposed;

    /**
     * Creates and immediately runs an effect owned by this scope.
     *
     * @throws IllegalStateException if this scope is already disposed
     */
    public Effect effect(Runnable body) {
        Objects.requireNonNull(body, "body");
        ensureActive();
        Effect effect = new Effect(body);
        owned.add(effect);
        return effect;
    }

    /**
     * Creates a lazy computed value owned by this scope.
     *
     * @throws IllegalStateException if this scope is already disposed
     */
    public <T> Computed<T> computed(Supplier<T> computation) {
        Objects.requireNonNull(computation, "computation");
        ensureActive();
        Computed<T> computed = new Computed<>(computation);
        owned.add(computed);
        return computed;
    }

    /**
     * Creates a child scope owned by this scope.
     *
     * @throws IllegalStateException if this scope is already disposed
     */
    public ReactiveScope scope() {
        ensureActive();
        ReactiveScope child = new ReactiveScope();
        owned.add(child);
        return child;
    }

    /**
     * Adds an arbitrary disposable resource to this scope. Resources are
     * disposed in reverse creation order.
     *
     * @throws IllegalStateException if this scope is already disposed
     */
    public void add(AutoCloseable disposable) {
        Objects.requireNonNull(disposable, "disposable");
        ensureActive();
        owned.add(disposable);
    }

    /**
     * Disposes all owned resources in reverse creation order. Calling this
     * method more than once has no additional effect.
     *
     * <p>If a resource fails to close, the remaining resources are still
     * disposed and the first failure is rethrown with later failures attached
     * as suppressed exceptions.</p>
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;

        RuntimeException failure = null;
        for (int index = owned.size() - 1; index >= 0; index--) {
            try {
                owned.get(index).close();
            } catch (Exception exception) {
                if (failure == null) {
                    failure = new IllegalStateException(
                            "Failed to dispose a reactive scope resource", exception);
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        owned.clear();

        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Returns whether this scope has been disposed.
     */
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void close() {
        dispose();
    }

    private void ensureActive() {
        if (disposed) {
            throw new IllegalStateException("Reactive scope is disposed");
        }
    }
}
