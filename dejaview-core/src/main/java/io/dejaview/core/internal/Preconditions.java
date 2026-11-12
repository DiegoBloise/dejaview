package io.dejaview.core.internal;

/**
 * Internal argument checks shared by core abstractions.
 *
 * <p>This is deliberately small. It exists so configuration and lifecycle
 * types validate their inputs consistently without repeating the same
 * conditions.</p>
 */
public final class Preconditions {

    private Preconditions() {
    }

    /**
     * Returns {@code value} or throws when it is {@code null}.
     */
    public static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new NullPointerException(name + " must not be null");
        }
        return value;
    }

    /**
     * Returns {@code value} or throws when it is {@code null} or blank.
     */
    public static String requireText(String value, String name) {
        requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
