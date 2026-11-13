package io.dejaview.core;

/**
 * Handles an event together with the current value of its target control.
 *
 * <p>Form bindings need the value typed into an {@code input}, the checked
 * state of a {@code checkbox} or the selected value of a {@code select}. The
 * core module cannot depend on a browser event model, so the platform adapter
 * extracts those two primitive facts and passes them here.</p>
 *
 * <p>{@code value} is never {@code null}; it is an empty string when the target
 * has no value. {@code checked} is only meaningful for checkable controls.</p>
 */
@FunctionalInterface
public interface EventValueHandler {

    /**
     * Receives the event target value.
     *
     * @param value   current target value, never {@code null}
     * @param checked current checked state; {@code false} for non-checkable
     *                targets
     */
    void handle(String value, boolean checked);
}
