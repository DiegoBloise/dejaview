package io.dejaview.http;

/**
 * A failed HTTP call. {@code code} is a stable diagnostic string:
 *
 * <ul>
 *   <li>{@code network-error} — the transport failed (DNS, connection refused, CORS, status 0);</li>
 *   <li>{@code timeout} — the request exceeded its effective timeout;</li>
 *   <li>{@code cancelled} — the call was cancelled via {@link HttpCall#cancel()};</li>
 *   <li>{@code interceptor-error} — a request or response interceptor threw, or returned {@code null};</li>
 *   <li>{@code missing-transport} — no transport was configured and the browser transport is unavailable;</li>
 *   <li>{@code json-error} — JSON (de)serialization failed (see {@link HttpResponse#json()}).</li>
 * </ul>
 *
 * <p>{@code cause} carries the original exception when one exists; it may be
 * {@code null}.</p>
 */
public record HttpFailure(String code, String message, Throwable cause) {

    public HttpFailure {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("failure code must not be blank");
        }
        if (message == null) {
            message = code;
        }
    }

    @Override
    public String toString() {
        return code + ": " + message;
    }
}
