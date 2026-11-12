package io.dejaview.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An immutable, case-insensitive header collection preserving insertion
 * order. Names are stored lower-cased; lookups are name-insensitive per the
 * HTTP specification.
 *
 * <p>A header may hold multiple values. On the wire, names with multiple
 * values are joined with {@code ", "} by browsers (and by this client when
 * writing), matching RFC 7230 §3.2.2.</p>
 */
public final class HttpHeaders {

    private static final HttpHeaders EMPTY = new HttpHeaders(Map.of());

    private final Map<String, List<String>> values;

    private HttpHeaders(Map<String, List<String>> values) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.values = Collections.unmodifiableMap(copy);
    }

    /** The empty header collection. */
    public static HttpHeaders empty() {
        return EMPTY;
    }

    /** Creates a collection from single-valued entries. */
    public static HttpHeaders of(Map<String, String> entries) {
        if (entries == null) {
            return EMPTY;
        }
        Builder builder = builder();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            builder.put(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    /** Creates a fresh builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** Creates a builder pre-filled with this collection's entries. */
    public Builder toBuilder() {
        Builder builder = new Builder();
        values.forEach((name, list) -> list.forEach(value -> builder.add(name, value)));
        return builder;
    }

    /** The first value for {@code name}, or {@code null} when absent. */
    public String get(String name) {
        List<String> list = values.get(key(name));
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    /** All values for {@code name}, in insertion order. */
    public List<String> all(String name) {
        List<String> list = values.get(key(name));
        return list == null ? List.of() : list;
    }

    /** The header names in insertion order, lower-cased. */
    public Set<String> names() {
        return values.keySet();
    }

    /** The number of header names. */
    public int size() {
        return values.size();
    }

    /** {@code true} when no headers are set. */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** The values joined with {@code ", "} for a single header line, or {@code null} when absent. */
    public String joined(String name) {
        List<String> list = values.get(key(name));
        return list == null || list.isEmpty() ? null : String.join(", ", list);
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        values.forEach((name, list) -> {
            for (String value : list) {
                if (out.length() > 0) {
                    out.append(", ");
                }
                out.append(name).append(": ").append(value);
            }
        });
        return out.toString();
    }

    private static String key(String name) {
        if (name == null) {
            throw new NullPointerException("header name must not be null");
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("header name must not be blank");
        }
        return trimmed.toLowerCase(java.util.Locale.ROOT);
    }

    /** Mutable builder for {@link HttpHeaders}. */
    public static final class Builder {
        private final Map<String, List<String>> values = new LinkedHashMap<>();

        private Builder() {
        }

        /** Replaces any existing values for {@code name}. */
        public Builder put(String name, String value) {
            requireValue(value, name);
            values.put(key(name), new ArrayList<>(List.of(value)));
            return this;
        }

        /** Appends a value to {@code name}, keeping existing values. */
        public Builder add(String name, String value) {
            requireValue(value, name);
            values.computeIfAbsent(key(name), k -> new ArrayList<>()).add(value);
            return this;
        }

        /** Removes any values for {@code name}. */
        public Builder remove(String name) {
            values.remove(key(name));
            return this;
        }

        /** Builds the immutable collection. */
        public HttpHeaders build() {
            return new HttpHeaders(values);
        }

        private static void requireValue(String value, String name) {
            if (value == null) {
                throw new NullPointerException("header value for '" + name + "' must not be null");
            }
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '\r' || c == '\n') {
                    throw new IllegalArgumentException(
                            "header value for '" + name + "' must not contain line breaks");
                }
            }
        }
    }
}
