package io.dejaview.reactivity;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A side effect that reruns when any signal or computed value it reads changes.
 *
 * <p>The body runs immediately on construction and again, synchronously,
 * whenever a tracked source is invalidated. Dependencies are re-collected on
 * every run, so conditional reads are supported.</p>
 *
 * <p>An effect stays registered until {@link #dispose()} is called. There is no
 * automatic owner tree or lifecycle scope yet.</p>
 *
 * <p>Effects are not thread-safe and must be disposed when no longer needed.</p>
 */
public final class Effect extends ReactiveObserver implements AutoCloseable {

    private final Runnable body;
    private final Set<ReactiveSource> dependencies = new LinkedHashSet<>();

    private boolean disposed;
    private boolean running;

    /**
     * Creates an effect and runs it immediately.
     *
     * @throws NullPointerException if {@code body} is {@code null}
     */
    public Effect(Runnable body) {
        this.body = Objects.requireNonNull(body, "body");
        run();
    }

    /**
     * Stops the effect and releases its dependencies. Calling this method more
     * than once has no additional effect.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        clearDependencies();
    }

    /**
     * Returns whether this effect has been disposed.
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
        if (disposed) {
            return;
        }
        if (running) {
            throw new IllegalStateException(
                    "An effect was invalidated while it was running");
        }
        run();
    }

    private void run() {
        clearDependencies();
        running = true;
        try {
            ReactiveContext.run(this, body);
        } catch (RuntimeException | Error failure) {
            clearDependencies();
            throw failure;
        } finally {
            running = false;
        }
    }

    private void clearDependencies() {
        for (ReactiveSource source : dependencies) {
            source.removeObserver(this);
        }
        dependencies.clear();
    }
}
