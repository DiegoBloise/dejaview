package io.dejaview.server;

import io.dejaview.core.internal.Preconditions;
import io.dejaview.http.HttpHeaders;

import java.util.Objects;

/**
 * An immutable server request handed to the rendering pipeline.
 *
 * <p>This is deliberately transport-neutral: it carries what the renderer needs
 * and nothing about sockets, servlets or HTTP frameworks. A host adapter
 * (Jetty, embedded HTTP server, servlet) builds one of these per incoming
 * request.</p>
 *
 * @param method  the HTTP method, e.g. {@code GET}; never blank
 * @param path    the request target, e.g. {@code /products?page=2}; never blank.
 *                The query string, when present, is part of the path — the
 *                server platform does not parse it
 * @param headers the request headers; never {@code null}, use
 *                {@link HttpHeaders#empty()} when empty
 * @param body    the request body as text; never {@code null}, empty when absent
 */
public record ServerRequest(String method, String path, HttpHeaders headers, String body) {

    public ServerRequest {
        method = Preconditions.requireText(method, "method").trim();
        path = Preconditions.requireText(path, "path").trim();
        headers = headers == null ? HttpHeaders.empty() : headers;
        body = body == null ? "" : body;
    }

    /**
     * Creates a request without headers or body.
     */
    public static ServerRequest of(String method, String path) {
        return new ServerRequest(method, path, HttpHeaders.empty(), "");
    }

    /**
     * Returns a copy of this request with the given headers.
     */
    public ServerRequest withHeaders(HttpHeaders headers) {
        return new ServerRequest(method, path, Objects.requireNonNull(headers, "headers"), body);
    }

    /**
     * Returns a copy of this request with the given body.
     */
    public ServerRequest withBody(String body) {
        return new ServerRequest(method, path, headers, body == null ? "" : body);
    }
}
