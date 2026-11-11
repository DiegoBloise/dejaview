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
 * <p>Computed values are not thread-safe and do not have an explicit disposal
 * lifecycle yet.</p>
 *
 * @param <T> value type; {@code null} results are not allowed
 */
public final class Computed<T> extends ReactiveObserver {

    private final Supplier<T> computation;
    private final ReactiveSource source = new ReactiveSource();
    private final Set<ReactiveSource> dependencies = new LinkedHashSet<>();

    private T value;
    private boolean dirty = true;
    private boolean computing;

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
     * @throws IllegalStateException if a circular dependency is detected
     */
    public T get() {
        if (computing) {
            throw new IllegalStateException("Circular computed dependency");
        }
        if (dirty) {
            recompute();
        }
        ReactiveContext.track(source);
        return value;
    }

    @Override
    void addDependency(ReactiveSource source) {
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
        if (dirty) {
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
