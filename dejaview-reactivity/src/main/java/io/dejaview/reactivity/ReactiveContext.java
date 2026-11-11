package io.dejaview.reactivity;

/**
 * Tracks which effect is currently reading signals.
 *
 * <p>The context is a simple stack: nested effects temporarily replace the
 * current effect and restore the previous one when they finish.</p>
 */
final class ReactiveContext {

    private static Effect currentEffect;

    private ReactiveContext() {
    }

    static void track(Signal<?> signal) {
        if (currentEffect != null) {
            currentEffect.addDependency(signal);
        }
    }

    static Effect currentEffect() {
        return currentEffect;
    }

    static void run(Effect effect, Runnable body) {
        Effect previous = currentEffect;
        currentEffect = effect;
        try {
            body.run();
        } finally {
            currentEffect = previous;
        }
    }
}
