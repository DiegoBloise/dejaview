package io.dejaview.http;

import io.dejaview.core.DejaViewException;
import io.dejaview.core.internal.Preconditions;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * An immutable HTTP request. Create one with {@link #get(String)} or one of
 * the other static factories, then add detail with the fluent methods; each
 * returns a new request and leaves the receiver unchanged.
 *
 * <p>The URL may be relative to the application ({@code /api/ping}) or an
 * absolute URL ({@code https://…}). Query parameters added with
 * {@link #queryParam(String, String)} are appended to the URL's query, with
 * names and values percent-encoded ({@code '+'} for space, matching the
 * router's query parser).</p>
 *
 * <p>{@code timeoutMillis} of {@code -1} (the default) inherits the client's
 * timeout; {@code 0} disables the timeout. {@code credentials} of
 * {@code null} (the default) inherits the client's policy.</p>
 */
public final class HttpRequest {

    /** Inherit the client's timeout. */
    public static final long TIMEOUT_INHERIT = -1;

    private final String url;
    private final HttpMethod method;
    private final HttpHeaders headers;
    private final String body;
    private final long timeoutMillis;
    private final HttpCredentials credentials;
    private final List<QueryParam> queryParams;

    private HttpRequest(String url,
                        HttpMethod method,
                        HttpHeaders headers,
                        String body,
                        long timeoutMillis,
                        HttpCredentials credentials,
                        List<QueryParam> queryParams) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.timeoutMillis = timeoutMillis;
        this.credentials = credentials;
        this.queryParams = List.copyOf(queryParams);
    }

    /** A request with {@link HttpMethod#GET}. */
    public static HttpRequest get(String url) {
        return create(HttpMethod.GET, url);
    }

    /** A request with {@link HttpMethod#POST}. */
    public static HttpRequest post(String url) {
        return create(HttpMethod.POST, url);
    }

    /** A request with {@link HttpMethod#PUT}. */
    public static HttpRequest put(String url) {
        return create(HttpMethod.PUT, url);
    }

    /** A request with {@link HttpMethod#PATCH}. */
    public static HttpRequest patch(String url) {
        return create(HttpMethod.PATCH, url);
    }

    /** A request with {@link HttpMethod#DELETE}. */
    public static HttpRequest delete(String url) {
        return create(HttpMethod.DELETE, url);
    }

    private static HttpRequest create(HttpMethod method, String url) {
        return new HttpRequest(requireUrl(url), method, HttpHeaders.empty(), null,
                TIMEOUT_INHERIT, null, List.of());
    }

    /** A copy with the header added (replacing any existing values for the name). */
    public HttpRequest header(String name, String value) {
        return withHeaders(headers.toBuilder().put(name, value).build());
    }

    /** A copy with the header appended to any existing values for the name. */
    public HttpRequest addHeader(String name, String value) {
        return withHeaders(headers.toBuilder().add(name, value).build());
    }

    /** A copy with the headers replaced by {@code headers}. */
    public HttpRequest withHeaders(HttpHeaders headers) {
        return new HttpRequest(url, method, headers, body, timeoutMillis, credentials, queryParams);
    }

    /** A copy with the query parameter appended. */
    public HttpRequest queryParam(String name, String value) {
        Preconditions.requireText(name, "query parameter name");
        Preconditions.requireNonNull(value, "query parameter value");
        List<QueryParam> copy = new ArrayList<>(queryParams);
        copy.add(new QueryParam(name, value));
        return new HttpRequest(url, method, headers, body, timeoutMillis, credentials, copy);
    }

    /** A copy with the body replaced by {@code body}. Pass {@code null} to clear the body. */
    public HttpRequest body(String body) {
        return new HttpRequest(url, method, headers, body, timeoutMillis, credentials, queryParams);
    }

    /**
     * A copy with {@code body} serialized as JSON and the
     * {@code Content-Type} header set to {@code application/json} (only when
     * not already set).
     */
    public HttpRequest jsonBody(JsonValue body) {
        Preconditions.requireNonNull(body, "body");
        HttpRequest request = body(Json.write(body));
        if (request.headers.get("Content-Type") == null) {
            request = request.header("Content-Type", "application/json");
        }
        return request;
    }

    /**
     * A copy with a per-request timeout in milliseconds. {@code -1} inherits
     * the client's timeout, {@code 0} disables the timeout.
     */
    public HttpRequest timeoutMillis(long timeoutMillis) {
        if (timeoutMillis < -1) {
            throw new IllegalArgumentException("timeoutMillis must be -1 or greater");
        }
        return new HttpRequest(url, method, headers, body, timeoutMillis, credentials, queryParams);
    }

    /** A copy with an explicit credentials policy. {@code null} inherits the client's policy. */
    public HttpRequest credentials(HttpCredentials credentials) {
        return new HttpRequest(url, method, headers, body, timeoutMillis, credentials, queryParams);
    }

    /** The URL with query parameters appended (percent-encoded). */
    public String fullUrl() {
        if (queryParams.isEmpty()) {
            return url;
        }
        StringBuilder out = new StringBuilder(url);
        out.append(url.indexOf('?') >= 0 ? '&' : '?');
        boolean first = true;
        for (QueryParam param : queryParams) {
            if (!first) {
                out.append('&');
            }
            first = false;
            out.append(encode(param.name)).append('=').append(encode(param.value));
        }
        return out.toString();
    }

    /** The request URL without appended query parameters. */
    public String url() {
        return url;
    }

    /** The HTTP method. */
    public HttpMethod method() {
        return method;
    }

    /** The headers. */
    public HttpHeaders headers() {
        return headers;
    }

    /** The body, or {@code null} when absent. */
    public String body() {
        return body;
    }

    /** The per-request timeout, or {@link #TIMEOUT_INHERIT}. */
    public long timeoutMillis() {
        return timeoutMillis;
    }

    /** The per-request credentials policy, or {@code null} to inherit. */
    public HttpCredentials credentials() {
        return credentials;
    }

    /** The query parameters added via {@link #queryParam(String, String)}, in order. */
    public List<QueryParam> queryParams() {
        return queryParams;
    }

    /** The effective timeout given the client's default. */
    long effectiveTimeout(long clientTimeoutMillis) {
        return timeoutMillis == TIMEOUT_INHERIT ? clientTimeoutMillis : timeoutMillis;
    }

    /** The effective credentials policy given the client's default. */
    HttpCredentials effectiveCredentials(HttpCredentials clientCredentials) {
        return credentials != null ? credentials : clientCredentials;
    }

    @Override
    public String toString() {
        return method + " " + fullUrl();
    }

    private static String requireUrl(String url) {
        Preconditions.requireText(url, "url");
        return url;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** A query parameter added via {@link #queryParam(String, String)}. */
    public record QueryParam(String name, String value) {
        @Override
        public String toString() {
            return name + "=" + value;
        }
    }
}
