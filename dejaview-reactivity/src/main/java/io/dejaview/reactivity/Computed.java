package io.dejaview.reactivity;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A lazily evaluated, cached value derived from signals or other computeds.
 *
 * <p>The supplier runs on the first {@link #get()} and then only after a
 * dependency is invalidated. Dependencies are re-collected on every
 * recomputation, so conditional reads are supported.</p>
 *
 * <p>Invalidation is push-based and evaluation is pull-based. When a dependency
 * changes, the computed is marked dirty and its observers are notified; the new
 * value is only calculated when someone reads it. As a consequence, an effect
 * that depends on a computed may rerun even if the recomputed value turns out
 * to be equal. This is documented in ADR-0009.</p>
 *
 * <p>A computed can be disposed directly or through a {@link ReactiveScope}.
 * After disposal, {@link #get()} throws and the computed no longer observes its
 * dependencies.</p>
 *
 * <p>Computed values are not thread-safe.</p>
 *
 * @param <T> value type; {@code null} results are not allowed
 */
public final class Computed<T> extends ReactiveObserver implements AutoCloseable {

    private final Supplier<T> computation;
    private final ReactiveSource source = new ReactiveSource();
    private final Set<ReactiveSource> dependencies = new LinkedHashSet<>();

    private T value;
    private boolean dirty = true;
    private boolean computing;
    private boolean disposed;

    /**
     * Creates a lazy computed value. The supplier is not called yet.
     *
     * @throws NullPointerException if {@code computation} is {@code null}
     */
    public Computed(Supplier<T> computation) {
        this.computation = Objects.requireNonNull(computation, "computation");
    }

    /**
     * Returns the cached value, recomputing it first when a dependency changed.
     * Reading a computed inside an effect or another computed registers a
     * dependency on it.
     *
     * @throws NullPointerException if the supplier returns {@code null}
     * @throws IllegalStateException if the computed is disposed or a circular
     *         dependency is detected
     */
    public T get() {
        if (disposed) {
            throw new IllegalStateException("Computed value is disposed");
        }
        if (computing) {
            throw new IllegalStateException("Circular computed dependency");
        }
        if (dirty) {
            recompute();
        }
        ReactiveContext.track(source);
        return value;
    }

    /**
     * Stops observing dependencies and releases the cached value. Calling this
     * method more than once has no additional effect.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        if (computing) {
            throw new IllegalStateException("Cannot dispose a computed while it is computing");
        }
        disposed = true;
        clearDependencies();
        value = null;
    }

    /**
     * Returns whether this computed has been disposed.
     */
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void close() {
        dispose();
    }

    @Override
    void addDependency(ReactiveSource source) {
        if (disposed) {
            return;
        }
        if (dependencies.add(source)) {
            source.addObserver(this);
        }
    }

    @Override
    boolean dependsOn(ReactiveSource source) {
        return dependencies.contains(source);
    }

    @Override
    void invalidate() {
        if (disposed || dirty) {
            return;
        }
        if (computing) {
            throw new IllegalStateException(
                    "A computed value was invalidated while it was computing");
        }
        dirty = true;
        source.notifyObservers();
    }

    private void recompute() {
        computing = true;
        dirty = false;
        clearDependencies();
        try {
            value = Objects.requireNonNull(
                    ReactiveContext.call(this, computation),
                    "computed value");
        } catch (RuntimeException | Error failure) {
            clearDependencies();
            dirty = true;
            throw failure;
        } finally {
            computing = false;
        }
    }

    private void clearDependencies() {
        for (ReactiveSource source : dependencies) {
            source.removeObserver(this);
        }
        dependencies.clear();
    }
}
