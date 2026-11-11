package io.dejaview.reactivity;

import java.util.function.Supplier;

/**
 * Tracks which observer is currently reading reactive sources.
 *
 * <p>The context is a simple stack: nested observers temporarily replace the
 * current one and restore the previous one when they finish.</p>
 */
final class ReactiveContext {

    private static ReactiveObserver currentObserver;

    private ReactiveContext() {
    }

    static void track(ReactiveSource source) {
        if (currentObserver != null) {
            currentObserver.addDependency(source);
        }
    }

    static ReactiveObserver currentObserver() {
        return currentObserver;
    }

    static void run(ReactiveObserver observer, Runnable body) {
        ReactiveObserver previous = currentObserver;
        currentObserver = observer;
        try {
            body.run();
        } finally {
            currentObserver = previous;
        }
    }

    static <T> T call(ReactiveObserver observer, Supplier<T> body) {
        ReactiveObserver previous = currentObserver;
        currentObserver = observer;
        try {
            return body.get();
        } finally {
            currentObserver = previous;
        }
    }
}
