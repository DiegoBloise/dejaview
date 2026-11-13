package io.dejaview.state;

import java.util.Objects;

/**
 * A structured, language-neutral async failure. {@code code} is a stable,
 * machine-readable identifier (for example {@code "empty-cart"} or
 * {@code "network-error"}); {@code message} is diagnostic detail for logs and
 * developers, not user-facing copy. Applications map codes to localized
 * messages, exactly like reactive form error keys.
 *
 * @param code stable identifier, never blank
 * @param message diagnostic detail; never null
 */
public record AsyncError(String code, String message) {

    /**
     * Creates an async error.
     *
     * @throws IllegalArgumentException if {@code code} is blank
     * @throws NullPointerException if {@code message} is null
     */
    public AsyncError {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        if (code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
    }

    /**
     * Convenience factory.
     */
    public static AsyncError of(String code, String message) {
        return new AsyncError(code, message);
    }
}
