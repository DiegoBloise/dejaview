package io.dejaview.reactivity;

import io.dejaview.core.ResourceRegistry;

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
 * <p>Disposal delegates to {@link ResourceRegistry}, which is shared with the
 * application context. Scopes are not thread-safe and must be used from the
 * same single thread as the rest of the reactivity primitives.</p>
 */
public final class ReactiveScope implements AutoCloseable {

    private final ResourceRegistry resources = new ResourceRegistry();

    /**
     * Creates and immediately runs an effect owned by this scope.
     *
     * @throws IllegalStateException if this scope is already disposed
     */
    public Effect effect(Runnable body) {
        Objects.requireNonNull(body, "body");
        ensureActive();
        return resources.register(new Effect(body));
    }

    /**
     * Creates a lazy computed value owned by this scope.
     *
     * @throws IllegalStateException if this scope is already disposed
     */
    public <T> Computed<T> computed(Supplier<T> computation) {
        Objects.requireNonNull(computation, "computation");
        ensureActive();
        return resources.register(new Computed<>(computation));
    }

    /**
     * Creates a child scope owned by this scope.
     *
     * @throws IllegalStateException if this scope is already disposed
     */
    public ReactiveScope scope() {
        ensureActive();
        return resources.register(new ReactiveScope());
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
        resources.register(disposable);
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
        resources.dispose();
    }

    /**
     * Returns whether this scope has been disposed.
     */
    public boolean isDisposed() {
        return resources.isDisposed();
    }

    @Override
    public void close() {
        dispose();
    }

    private void ensureActive() {
        if (resources.isDisposed()) {
            throw new IllegalStateException("Reactive scope is disposed");
        }
    }
}
