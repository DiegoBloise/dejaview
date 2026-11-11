package io.dejaview.reactivity;

/**
 * Something that reads reactive sources and can be invalidated when one of
 * them changes.
 *
 * <p>Implemented by {@link Effect} and {@link Computed}. The type is
 * package-private so observer management does not leak into the public API.</p>
 */
abstract class ReactiveObserver {

    abstract void addDependency(ReactiveSource source);

    abstract boolean dependsOn(ReactiveSource source);

    abstract void invalidate();
}
