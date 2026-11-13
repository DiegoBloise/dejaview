package io.dejaview.server;

import io.dejaview.http.HttpHeaders;

import java.util.Map;
import java.util.Objects;

/**
 * An immutable server response produced by the rendering pipeline.
 *
 * <p>Like {@link ServerRequest}, this is transport-neutral: a host adapter
 * translates it to whatever the underlying HTTP implementation speaks.</p>
 *
 * @param status  the HTTP status code, 100–599
 * @param headers the response headers; never {@code null}
 * @param body    the response body as text; never {@code null}, empty when absent
 */
public record ServerResponse(int status, HttpHeaders headers, String body) {

    public static final String TEXT_HTML = "text/html; charset=utf-8";

    public ServerResponse {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("status must be between 100 and 599, got " + status);
        }
        headers = headers == null ? HttpHeaders.empty() : headers;
        body = body == null ? "" : body;
    }

    /**
     * Creates a 200 response with {@code text/html; charset=utf-8}.
     */
    public static ServerResponse html(String body) {
        return new ServerResponse(200, HttpHeaders.of(Map.of("Content-Type", TEXT_HTML)), body);
    }

    /**
     * Creates a response with the given status and an HTML content type.
     */
    public static ServerResponse html(int status, String body) {
        return new ServerResponse(status, HttpHeaders.of(Map.of("Content-Type", TEXT_HTML)), body);
    }

    /**
     * Creates a response with the given status, headers and body.
     */
    public static ServerResponse of(int status, HttpHeaders headers, String body) {
        return new ServerResponse(status, headers, body);
    }

    /**
     * Returns a copy of this response with the given headers.
     */
    public ServerResponse withHeaders(HttpHeaders headers) {
        return new ServerResponse(status, Objects.requireNonNull(headers, "headers"), body);
    }
}
