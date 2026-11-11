package io.dejaview.core;

import java.util.Objects;

/**
 * Binds a platform event type to a Java callback.
 *
 * <p>The event type is a browser event name such as {@code "click"} or
 * {@code "input"}. It is intentionally a string rather than a framework enum:
 * the set of DOM events is large and extensible, and the core module must not
 * depend on a platform event model.</p>
 *
 * @param type    non-blank event type
 * @param handler callback invoked when the event fires
 */
public record EventBinding(String type, Runnable handler) {

    public EventBinding {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");
        if (type.isBlank()) {
            throw new IllegalArgumentException("Event type must not be blank");
        }
    }
}
