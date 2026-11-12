package io.dejaview.core;

/**
 * Optional lifecycle callbacks for framework-owned objects.
 *
 * <p>Disposal remains {@link AutoCloseable}. These callbacks describe the
 * startup and shutdown phases of an object that may be reused between them,
 * which is different from releasing its resources.</p>
 */
public interface Lifecycle {

    /**
     * Called after the object has been created and wired.
     */
    default void onStart() {
    }

    /**
     * Called before the object is disposed.
     */
    default void onStop() {
    }
}
