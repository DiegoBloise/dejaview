package io.dejaview.reactivity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Synchronous, single-threaded scheduler for effect executions.
 *
 * <p>Effects are queued when invalidated and executed in first-in-first-out
 * order. Outside a batch the queue is flushed immediately, preserving the
 * synchronous behavior of the first reactivity slice. Inside
 * {@link Reactive#batch(Runnable)} executions are deferred until the outermost
 * batch finishes, and each effect runs at most once per batch.</p>
 *
 * <p>The scheduler also guards against runaway update loops by limiting the
 * number of effect runs in a single flush.</p>
 */
final class ReactiveScheduler {

    private static final int MAX_FLUSH_STEPS = 10_000;

    private static final Deque<Effect> queue = new ArrayDeque<>();
    private static final Set<Effect> queued = new LinkedHashSet<>();

    private static int batchDepth;
    private static boolean flushing;

    private ReactiveScheduler() {
    }

    static void schedule(Effect effect) {
        if (queued.add(effect)) {
            queue.add(effect);
        }
        if (batchDepth == 0 && !flushing) {
            flush();
        }
    }

    static void batch(Runnable body) {
        Objects.requireNonNull(body, "body");

        batchDepth++;
        try {
            body.run();
        } catch (RuntimeException | Error failure) {
            batchDepth--;
            try {
                if (batchDepth == 0) {
                    flush();
                }
            } catch (RuntimeException | Error flushFailure) {
                failure.addSuppressed(flushFailure);
            }
            throw failure;
        }

        batchDepth--;
        if (batchDepth == 0) {
            flush();
        }
    }

    static boolean isBatching() {
        return batchDepth > 0;
    }

    private static void flush() {
        if (flushing) {
            return;
        }

        flushing = true;
        int steps = 0;
        try {
            while (!queue.isEmpty()) {
                if (++steps > MAX_FLUSH_STEPS) {
                    throw new IllegalStateException(
                            "Reactive update loop detected after " + MAX_FLUSH_STEPS
                                    + " effect runs");
                }
                Effect effect = queue.poll();
                queued.remove(effect);
                effect.runScheduled();
            }
        } catch (RuntimeException | Error failure) {
            // Leave the scheduler usable after a failed run instead of
            // replaying a partially processed queue.
            queue.clear();
            queued.clear();
            throw failure;
        } finally {
            flushing = false;
        }
    }
}
