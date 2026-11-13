package io.dejaview.forms;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A snapshot produced by {@link AbstractControl#submit()}.
 *
 * @param valid   whether the control tree is valid
 * @param value   submitted value, never {@code null}
 * @param errors  aggregated errors, never {@code null}
 * @param <V>     value type
 */
public record FormSubmission<V>(boolean valid, V value, ValidationErrors errors) {

    public FormSubmission {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(errors, "errors");
    }

    /**
     * Invokes {@code consumer} with the value only when the submission is
     * valid. Returns whether the consumer ran.
     */
    public boolean ifValid(Consumer<V> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (!valid) {
            return false;
        }
        consumer.accept(value);
        return true;
    }
}
