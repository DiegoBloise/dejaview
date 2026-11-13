package io.dejaview.forms;

/**
 * Synchronous validation rule for a control value.
 *
 * <p>A validator returns {@link ValidationErrors#none()} when the value is
 * valid. Keys are stable error codes; applications map them to localized
 * messages. Built-in validators use the keys {@code required},
 * {@code minLength}, {@code maxLength}, {@code email}, {@code pattern},
 * {@code min} and {@code max}.</p>
 *
 * @param <V> value type
 */
@FunctionalInterface
public interface Validator<V> {

    /**
     * Validates a non-{@code null} value.
     *
     * @param value value to validate
     * @return errors, never {@code null}
     */
    ValidationErrors validate(V value);
}
