package io.dejaview.state;

import io.dejaview.reactivity.Signal;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A store for an asynchronous operation: one shared signal of
 * {@link AsyncState}{@code <T>} with explicit loading and error states and
 * stale-attempt rejection.
 *
 * <p>Call {@link #load()} to start an attempt. The status goes
 * {@link AsyncStatus#IDLE} → {@link AsyncStatus#LOADING} →
 * {@link AsyncStatus#SUCCESS} or {@link AsyncStatus#FAILURE}. Starting a new
 * attempt while one is in flight is allowed; callbacks from superseded
 * attempts are detected with a generation counter and silently dropped, so an
 * outdated response cannot overwrite newer state or resurrect a disposed
 * store. Completions keep the previous value on failure and while refreshing
 * (stale-while-revalidate).</p>
 *
 * <p>Exactly-once semantics are the loader's contract: exactly one of
 * {@code succeed}/{@code fail} per attempt. If a loader misbehaves and calls
 * both, the first outcome wins and the rest are dropped.</p>
 *
 * <p>Single-threaded, like the rest of the reactivity primitives.</p>
 *
 * @param <T> value type produced on success
 */
public final class AsyncStore<T> {

    private final Signal<AsyncState<T>> state = new Signal<>(AsyncState.idle());
    private final AsyncLoader<T> loader;
    private int generation;
    private int settledAttempt = -1;
    private boolean disposed;

    /**
     * Creates an async store bound to {@code loader}. Nothing runs until
     * {@link #load()} is called.
     *
     * @throws NullPointerException if {@code loader} is null
     */
    public AsyncStore(AsyncLoader<T> loader) {
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    /**
     * Returns the current async state and registers a reactive dependency.
     */
    public AsyncState<T> state() {
        ensureActive();
        return state.get();
    }

    /**
     * Whether a load is currently in flight.
     */
    public boolean isLoading() {
        return state().isLoading();
    }

    /**
     * Starts a new attempt. Any previous attempt still in flight is
     * superseded: its completion is dropped.
     *
     * @throws IllegalStateException if the store is disposed
     */
    public void load() {
        ensureActive();
        int attempt = ++generation;
        Optional<T> previous = state.get().data();
        state.set(AsyncState.loading(previous));

        Consumer<T> succeed = value -> complete(attempt, AsyncState.success(value));
        Consumer<AsyncError> fail = error -> complete(attempt, AsyncState.failure(error, previous));
        try {
            loader.load(succeed, fail);
        } catch (RuntimeException | Error unexpected) {
            complete(attempt, AsyncState.failure(
                    AsyncError.of("load-error", "the loader threw: " + unexpected),
                    previous));
        }
    }

    /**
     * Alias for {@link #load()}; reads better at call sites that own a
     * trigger button.
     */
    public void reload() {
        load();
    }

    /**
     * Disposes the store. Pending attempts are invalidated; their callbacks
     * become no-ops. Calling this method more than once has no additional
     * effect.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        generation++;
    }

    /**
     * Returns whether {@link #dispose()} has been called.
     */
    public boolean isDisposed() {
        return disposed;
    }

    private void complete(int attempt, AsyncState<T> outcome) {
        if (disposed || attempt != generation || attempt == settledAttempt) {
            return;
        }
        settledAttempt = attempt;
        state.set(outcome);
    }

    private void ensureActive() {
        if (disposed) {
            throw new IllegalStateException("Async store is disposed");
        }
    }
}
