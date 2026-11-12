package io.dejaview.core;

import java.util.Objects;

/**
 * A single DOM property.
 *
 * <p>Properties are applied through the platform API and are separate from
 * attributes because some values (for example {@code value}, {@code checked} or
 * {@code disabled}) are only meaningful as properties. The core model keeps
 * both as strings; the platform adapter decides how to apply them.</p>
 *
 * @param name  non-blank property name
 * @param value property value, never {@code null}
 */
public record Property(String name, String value) {

    public Property {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Property name must not be blank");
        }
    }
}
