package io.dejaview.http;

import io.dejaview.core.DejaViewException;

/**
 * An HTTP response. {@code status} is the raw HTTP status code;
 * {@link #ok()} is {@code true} for the 2xx range. {@code body} is the raw
 * response body text as received from the transport.
 */
public record HttpResponse(int status,
                           String statusText,
                           HttpHeaders headers,
                           String body) {

    public HttpResponse {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("status must be a valid HTTP status code, got " + status);
        }
        if (headers == null) {
            headers = HttpHeaders.empty();
        }
        if (statusText == null) {
            statusText = "";
        }
        if (body == null) {
            body = "";
        }
    }

    /** {@code true} for status codes 200–299. */
    public boolean ok() {
        return status >= 200 && status < 300;
    }

    /** Parses the body as JSON. Throws {@link DejaViewException} with code {@code json-error} on malformed input. */
    public JsonValue json() {
        return Json.parse(body);
    }

    /** The body parsed as JSON and coerced to the object members; fails on type mismatch. */
    public JsonValue.JsonObject jsonObject() {
        JsonValue value = json();
        if (!(value instanceof JsonValue.JsonObject object)) {
            throw new DejaViewException("json-error", "response body must be a JSON object", null);
        }
        return object;
    }
}
