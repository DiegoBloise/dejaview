package io.dejaview.dom;

/**
 * Receives a DOM event together with the target value and checked state.
 *
 * <p>Platform adapters extract {@code event.target.value} and
 * {@code event.target.checked} and call this listener. Keeping the extraction
 * in the adapter is what lets {@code dejaview-core} stay independent of browser
 * types.</p>
 */
@FunctionalInterface
public interface DomValueListener {

    /**
     * Handles an event.
     *
     * @param value   target value, never {@code null}
     * @param checked target checked state; {@code false} when not applicable
     */
    void handle(String value, boolean checked);
}
