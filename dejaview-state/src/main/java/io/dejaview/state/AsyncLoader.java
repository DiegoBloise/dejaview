package io.dejaview.state;

import java.util.function.Consumer;

/**
 * The asynchronous work behind an {@link AsyncStore}.
 *
 * <p>An implementation performs the operation (a network call, an XHR via
 * {@code dejaview-http}, a timer, a file read) and reports exactly one
 * outcome through one of the two callbacks — never both. The callbacks passed
 * to {@code load(...)} belong to one attempt; completing from a superseded
 * attempt is detected and ignored by the store itself, so transports do not
 * need their own bookkeeping.</p>
 *
 * @param <T> value type produced on success
 */
@FunctionalInterface
public interface AsyncLoader<T> {

    /**
     * Starts the operation. Implementations must call one of the callbacks
     * exactly once, on any thread they like (the browser event loop in
     * practice).
     *
     * @param succeed called with the produced value
     * @param fail called with a structured error code and diagnostic message
     */
    void load(Consumer<T> succeed, Consumer<AsyncError> fail);
}
