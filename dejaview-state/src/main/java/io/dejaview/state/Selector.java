package io.dejaview.state;

import java.util.Objects;
import java.util.function.Function;

/**
 * A named, reusable projection over a store's state.
 *
 * <p>Selectors are ordinary functions with a stable name. The name describes
 * the projection in diagnostics and documentation; it does not change how the
 * projection is evaluated. Create one with {@link #of(String, Function)} and
 * pass it to {@link StateStore#select(Selector)} when the projector is reused
 * across components or belongs in a catalog of shared projections.</p>
 *
 * <p>Selector results compare with {@link Objects#equals} wherever equality
 * matters; DejaView does not currently short-circuit computed values on
 * equal results (see the &quot;Equality short-circuit and topological
 * ordering&quot; refinement in Phase 2 of the roadmap).</p>
 *
 * @param <S> state type the selector projects from
 * @param <R> projection result type
 */
public final class Selector<S, R> {

    private final String name;
    private final Function<S, R> projector;

    private Selector(String name, Function<S, R> projector) {
        this.name = name;
        this.projector = projector;
    }

    /**
     * Creates a named selector.
     *
     * @throws NullPointerException if {@code name} or {@code projector} is null
     */
    public static <S, R> Selector<S, R> of(String name, Function<S, R> projector) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return new Selector<>(name, Objects.requireNonNull(projector, "projector"));
    }

    /**
     * Returns the stable selector name, e.g. {@code "cart/itemCount"}.
     */
    public String name() {
        return name;
    }

    /**
     * Projects {@code state}. Registers no dependencies of its own; tracking
     * happens because callers read signals inside the returned value's
     * computation.
     */
    public R select(S state) {
        Objects.requireNonNull(state, "state");
        return projector.apply(state);
    }

    @Override
    public String toString() {
        return "Selector[" + name + "]";
    }
}
