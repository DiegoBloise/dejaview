package io.dejaview.core;

import io.dejaview.core.internal.Preconditions;

import java.util.Objects;

/**
 * A text node.
 *
 * <p>Text is kept separate from element children so renderers can use the
 * platform's native text node API. That is both faster and safer than
 * injecting HTML strings.</p>
 *
 * @param value text content, never {@code null}
 * @param key   optional reconciliation key, or {@code null} when unkeyed
 */
public record TextView(String value, String key) implements View {

    public TextView {
        Objects.requireNonNull(value, "value");
        Preconditions.requireKey(key, "key");
    }

    /**
     * Creates an unkeyed text view.
     */
    public TextView(String value) {
        this(value, null);
    }

    /**
     * Returns a copy of this view with the given reconciliation key.
     */
    public TextView key(String key) {
        return new TextView(value, key);
    }
}
