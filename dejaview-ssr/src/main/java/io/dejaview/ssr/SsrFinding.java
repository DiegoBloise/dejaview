package io.dejaview.ssr;

import java.util.Objects;

/**
 * A non-fatal observation made while validating or serializing a server tree.
 *
 * <p>Findings never change the HTTP status: a render that contains warnings is
 * still a successful render. They exist so a host can log or surface the places
 * where the HTML is safe but not a perfect representation of the view tree
 * (see {@link SsrResult#warnings()}).</p>
 *
 * @param code    a stable, machine-readable code, for example
 *                {@code "skipped-property"}
 * @param message a human-readable explanation, never blank
 */
public record SsrFinding(String code, String message) {

    public SsrFinding {
        Objects.requireNonNull(code, "code");
        if (code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        Objects.requireNonNull(message, "message");
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
