package io.dejaview.http;

/**
 * HTTP methods supported by {@link HttpClient}. Arbitrary methods are out of
 * scope for this client; extend the module rather than tunneling through a
 * raw string.
 */
public enum HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE
}
