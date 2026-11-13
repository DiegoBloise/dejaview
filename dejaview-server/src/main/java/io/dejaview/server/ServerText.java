package io.dejaview.server;

import java.util.Objects;

/**
 * A text node in the server render tree.
 *
 * @param value the text content, never {@code null}
 */
public record ServerText(String value) implements ServerNode {

    public ServerText {
        Objects.requireNonNull(value, "value");
    }
}
