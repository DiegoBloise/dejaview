package io.dejaview.reactivity;

import java.util.Objects;

/**
 * Entry points for reactive scheduling and lifecycle helpers.
 *
 * <p>This class keeps batching and cleanup registration out of the
 * {@link Signal}, {@link Effect} and {@link Computed} APIs, which stay focused
 * on value access.</p>
 */
public final class Reactive {

    private Reactive() {
    }

    /**
     * Runs {@code body} while deferring effect executions until the outermost
     * batch finishes. Each effect runs at most once per batch.
     *
     * <p>Batches may be nested. Only the outermost batch flushes the queue.</p>
     *
     * @throws NullPointerException if {@code body} is {@code null}
     */
    public static void batch(Runnable body) {
        ReactiveScheduler.batch(body);
    }

    /**
     * Returns whether the current thread is inside a batch.
     */
    public static boolean isBatching() {
        return ReactiveScheduler.isBatching();
    }

    /**
     * Registers a cleanup callback for the effect that is currently running.
     *
     * <p>The callback runs before the next execution of the effect and when the
     * effect is disposed. It is intended for releasing resources created by the
     * effect body, such as event listeners or timers.</p>
     *
     * @throws NullPointerException if {@code cleanup} is {@code null}
     * @throws IllegalStateException if called outside an effect body
     */
    public static void onCleanup(Runnable cleanup) {
        Objects.requireNonNull(cleanup, "cleanup");

        ReactiveObserver observer = ReactiveContext.currentObserver();
        if (!(observer instanceof Effect effect)) {
            throw new IllegalStateException("onCleanup must be called inside an Effect");
        }
        effect.addCleanup(cleanup);
    }
}
