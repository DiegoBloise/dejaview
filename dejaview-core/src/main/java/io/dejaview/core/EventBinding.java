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
 * <p>A binding carries either a plain {@link Runnable} or an
 * {@link EventValueHandler} that also receives the target value. Exactly one of
 * the two is present; the convenience constructors cover both cases.</p>
 *
 * @param type         non-blank event type
 * @param handler      callback invoked when the event fires, or {@code null}
 * @param valueHandler callback invoked with the target value, or {@code null}
 */
public record EventBinding(String type, Runnable handler, EventValueHandler valueHandler) {

    public EventBinding {
        Objects.requireNonNull(type, "type");
        if (type.isBlank()) {
            throw new IllegalArgumentException("Event type must not be blank");
        }
        if ((handler == null) == (valueHandler == null)) {
            throw new IllegalArgumentException(
                    "An event binding requires exactly one handler");
        }
    }

    /**
     * Creates a binding for a listener that ignores the target value.
     */
    public EventBinding(String type, Runnable handler) {
        this(type, Objects.requireNonNull(handler, "handler"), null);
    }

    /**
     * Creates a binding whose listener receives the target value and checked
     * state.
     */
    public EventBinding(String type, EventValueHandler valueHandler) {
        this(type, null, Objects.requireNonNull(valueHandler, "valueHandler"));
    }

    /**
     * Returns whether this binding listens for the target value.
     */
    public boolean hasValueHandler() {
        return valueHandler != null;
    }
}
