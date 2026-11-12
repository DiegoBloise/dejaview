package io.dejaview.router;

/**
 * Handle for a router event listener registered with {@link Router#events}.
 *
 * <p>Removing a listener explicitly is required when a long-lived component
 * subscribes outside its own lifecycle. The handle makes the registration
 * reversible; {@link #close()} delegates to {@link #remove()} so the handle
 * works with try-with-resources.</p>
 */
public interface EventSubscription extends AutoCloseable {

    /** Removes the listener. Idempotent. */
    void remove();

    @Override
    default void close() {
        remove();
    }
}
