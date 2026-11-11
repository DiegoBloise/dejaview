package io.dejaview.core;

import java.util.Objects;

/**
 * A single HTML attribute.
 *
 * <p>Attribute values are always strings. The renderer is responsible for
 * applying them through the platform API, which means values are never
 * concatenated into markup and therefore do not need manual escaping.</p>
 *
 * @param name  non-blank attribute name
 * @param value attribute value, never {@code null}
 */
public record Attribute(String name, String value) {

    public Attribute {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Attribute name must not be blank");
        }
    }
}
