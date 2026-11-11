package io.dejaview.core;

import java.util.Objects;

/**
 * A text node.
 *
 * <p>Text is kept separate from element children so renderers can use the
 * platform's native text node API. That is both faster and safer than
 * injecting HTML strings.</p>
 *
 * @param value text content, never {@code null}
 */
public record TextView(String value) implements View {

    public TextView {
        Objects.requireNonNull(value, "value");
    }
}
