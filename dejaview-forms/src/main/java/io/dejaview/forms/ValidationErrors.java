package io.dejaview.forms;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An immutable, ordered set of validation errors.
 *
 * <p>Errors are keyed by a stable code such as {@code "required"} or
 * {@code "email"}. A key may carry an optional message supplied by a custom
 * validator. Built-in validators only use keys, which keeps the framework
 * language-neutral: the application decides how each code is presented.</p>
 *
 * <p>Group and array errors are prefixed with the child name or index, for
 * example {@code "email.required"} or {@code "items.0.required"}.</p>
 */
public final class ValidationErrors {

    private static final ValidationErrors NONE = new ValidationErrors(Map.of());

    private final Map<String, String> entries;

    private ValidationErrors(Map<String, String> entries) {
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    /**
     * Returns the empty error set.
     */
    public static ValidationErrors none() {
        return NONE;
    }

    /**
     * Creates an error set with one key and no message.
     *
     * @throws IllegalArgumentException if {@code key} is blank
     */
    public static ValidationErrors of(String key) {
        return of(key, "");
    }

    /**
     * Creates an error set with one key and a message.
     *
     * @throws IllegalArgumentException if {@code key} is blank
     * @throws NullPointerException     if {@code message} is {@code null}
     */
    public static ValidationErrors of(String key, String message) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(message, "message");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Validation error key must not be blank");
        }
        return new ValidationErrors(Map.of(key, message));
    }

    /**
     * Creates an error set from an ordered map of key to message.
     */
    public static ValidationErrors of(Map<String, String> entries) {
        Objects.requireNonNull(entries, "entries");
        if (entries.isEmpty()) {
            return NONE;
        }
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "key");
            Objects.requireNonNull(entry.getValue(), "message");
            if (entry.getKey().isBlank()) {
                throw new IllegalArgumentException("Validation error key must not be blank");
            }
        }
        return new ValidationErrors(entries);
    }

    /**
     * Returns whether there are no errors.
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Returns whether {@code key} is present.
     */
    public boolean has(String key) {
        return entries.containsKey(key);
    }

    /**
     * Returns the message for {@code key}, or an empty string when absent.
     */
    public String message(String key) {
        return entries.getOrDefault(key, "");
    }

    /**
     * Returns the error keys in insertion order.
     */
    public Set<String> keys() {
        return entries.keySet();
    }

    /**
     * Returns an immutable copy of the key-to-message map.
     */
    public Map<String, String> asMap() {
        return entries;
    }

    /**
     * Returns a new set with this set's errors followed by {@code other}'s.
     * This set wins when both contain the same key.
     */
    public ValidationErrors merge(ValidationErrors other) {
        Objects.requireNonNull(other, "other");
        if (other.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return other;
        }
        Map<String, String> merged = new LinkedHashMap<>(entries);
        for (Map.Entry<String, String> entry : other.entries.entrySet()) {
            merged.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return new ValidationErrors(merged);
    }

    /**
     * Returns a new set whose keys are prefixed with {@code prefix + "."}.
     */
    public ValidationErrors prefix(String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        if (isEmpty()) {
            return this;
        }
        Map<String, String> prefixed = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            prefixed.put(prefix + "." + entry.getKey(), entry.getValue());
        }
        return new ValidationErrors(prefixed);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ValidationErrors errors && entries.equals(errors.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    @Override
    public String toString() {
        return "ValidationErrors" + entries;
    }
}
