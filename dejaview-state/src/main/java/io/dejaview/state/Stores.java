package io.dejaview.state;

import io.dejaview.reactivity.Computed;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Cross-store composition. A DejaView app does not need a global store of
 * everything; it composes independent stores into derived values where
 * components actually share them.
 */
public final class Stores {

    private Stores() {
    }

    /**
     * Returns a computed that reads {@code left} and {@code right} and
     * combines them with {@code combiner}. Reading the result inside an
     * effect or another computed registers dependencies on both stores.
     *
     * <p>The returned computed is not owned by either store; dispose it (or
     * wrap it in a {@code ReactiveScope}) when the owning view is torn down.
     * Composing further views over the result is just another computed or
     * another {@code combine} call.</p>
     */
    public static <A, B, R> Computed<R> combine(
            StateStore<A> left, StateStore<B> right, BiFunction<A, B, R> combiner) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(combiner, "combiner");
        return new Computed<>(() -> combiner.apply(left.state(), right.state()));
    }
}
