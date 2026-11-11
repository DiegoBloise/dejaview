package io.dejaview.reactivity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A side effect that reruns when any signal or computed value it reads changes.
 *
 * <p>The body runs immediately on construction and again when a tracked source
 * is invalidated. Outside a batch, executions are synchronous. Inside
 * {@link Reactive#batch(Runnable)}, executions are deferred and coalesced.</p>
 *
 * <p>Dependencies are re-collected on every run, so conditional reads are
 * supported. Cleanup callbacks registered with {@link Reactive#onCleanup(Runnable)}
 * run before the next execution and on disposal.</p>
 *
 * <p>An effect stays registered until {@link #dispose()} is called. A
 * {@link ReactiveScope} can dispose effects together with other reactive
 * resources.</p>
 *
 * <p>Effects are not thread-safe and must be disposed when no longer needed.</p>
 */
public final class Effect extends ReactiveObserver implements AutoCloseable {

    private final Runnable body;
    private final Set<ReactiveSource> dependencies = new LinkedHashSet<>();
    private final List<Runnable> cleanups = new ArrayList<>();

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
     * Stops the effect, releases its dependencies and runs pending cleanup
     * callbacks. Calling this method more than once has no additional effect.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        clearDependencies();
        runCleanups();
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
        ReactiveScheduler.schedule(this);
    }

    void addCleanup(Runnable cleanup) {
        if (disposed) {
            throw new IllegalStateException("Cannot register cleanup on a disposed effect");
        }
        cleanups.add(cleanup);
    }

    void runScheduled() {
        if (!disposed) {
            run();
        }
    }

    private void run() {
        clearDependencies();
        runCleanups();

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

    private void runCleanups() {
        if (cleanups.isEmpty()) {
            return;
        }

        List<Runnable> pending = new ArrayList<>(cleanups);
        cleanups.clear();

        RuntimeException failure = null;
        for (Runnable cleanup : pending) {
            try {
                cleanup.run();
            } catch (RuntimeException | Error cleanupFailure) {
                if (failure == null) {
                    failure = new IllegalStateException(
                            "Effect cleanup failed", cleanupFailure);
                } else {
                    failure.addSuppressed(cleanupFailure);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void clearDependencies() {
        for (ReactiveSource source : dependencies) {
            source.removeObserver(this);
        }
        dependencies.clear();
    }
}
