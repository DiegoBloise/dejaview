package io.dejaview.dom;

/**
 * Handle for a registered DOM event listener.
 *
 * <p>Removing a listener explicitly is required when a mounted tree is
 * unmounted or when a long-lived element is reused. The handle makes the
 * registration reversible without exposing platform-specific listener
 * types.</p>
 */
public interface DomEventRegistration extends AutoCloseable {

    /**
     * Removes the listener. Calling this method more than once has no
     * additional effect.
     */
    void remove();

    @Override
    default void close() {
        remove();
    }
}
