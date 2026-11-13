package io.dejaview.forms;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Ready-to-use synchronous validators.
 *
 * <p>String validators ignore empty values so a field is only marked invalid
 * for a missing value when {@link #required()} is also present. This mirrors
 * the common form rule that "empty" and "malformed" are separate decisions.</p>
 *
 * <p>Every built-in validator returns a stable error key and no message. Add a
 * message with {@link ValidationErrors#of(String, String)} in a custom
 * validator, or map keys to localized copy in the application.</p>
 */
public final class Validators {

    private Validators() {
    }

    /**
     * Requires a non-blank string.
     */
    public static Validator<String> required() {
        return value -> value.isBlank()
                ? ValidationErrors.of("required")
                : ValidationErrors.none();
    }

    /**
     * Requires at least {@code min} characters. Empty values are accepted.
     */
    public static Validator<String> minLength(int min) {
        if (min < 0) {
            throw new IllegalArgumentException("min must not be negative");
        }
        return value -> value.isEmpty() || value.length() >= min
                ? ValidationErrors.none()
                : ValidationErrors.of("minLength");
    }

    /**
     * Requires at most {@code max} characters. Empty values are accepted.
     */
    public static Validator<String> maxLength(int max) {
        if (max < 0) {
            throw new IllegalArgumentException("max must not be negative");
        }
        return value -> value.length() <= max
                ? ValidationErrors.none()
                : ValidationErrors.of("maxLength");
    }

    /**
     * Requires a simple {@code local@domain.tld} address. Empty values are
     * accepted.
     */
    public static Validator<String> email() {
        return value -> value.isEmpty() || isEmail(value)
                ? ValidationErrors.none()
                : ValidationErrors.of("email");
    }

    /**
     * Requires a string that matches {@code pattern}. Empty values are
     * accepted. The reported key is {@code "pattern"}.
     */
    public static Validator<String> pattern(Pattern pattern) {
        return pattern(pattern, "pattern");
    }

    /**
     * Requires a string that matches {@code pattern}. Empty values are
     * accepted. The reported key is {@code key}.
     */
    public static Validator<String> pattern(Pattern pattern, String key) {
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(key, "key");
        return value -> value.isEmpty() || pattern.matcher(value).matches()
                ? ValidationErrors.none()
                : ValidationErrors.of(key);
    }

    /**
     * Requires a number greater than or equal to {@code min}.
     */
    public static <N extends Number> Validator<N> min(double min) {
        return value -> value.doubleValue() >= min
                ? ValidationErrors.none()
                : ValidationErrors.of("min");
    }

    /**
     * Requires a number less than or equal to {@code max}.
     */
    public static <N extends Number> Validator<N> max(double max) {
        return value -> value.doubleValue() <= max
                ? ValidationErrors.none()
                : ValidationErrors.of("max");
    }

    /**
     * Creates a validator from a predicate. The predicate returns {@code true}
     * when the value is valid.
     */
    public static <V> Validator<V> custom(String key, Predicate<V> isValid) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(isValid, "isValid");
        return value -> isValid.test(value)
                ? ValidationErrors.none()
                : ValidationErrors.of(key);
    }

    private static boolean isEmail(String value) {
        int at = value.indexOf('@');
        if (at <= 0 || at != value.lastIndexOf('@') || at == value.length() - 1) {
            return false;
        }
        String domain = value.substring(at + 1);
        int dot = domain.indexOf('.');
        return dot > 0 && dot < domain.length() - 1 && !containsWhitespace(value);
    }

    private static boolean containsWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }
}
