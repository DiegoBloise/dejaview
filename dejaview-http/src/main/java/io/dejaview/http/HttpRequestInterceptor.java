package io.dejaview.http;

/**
 * Intercepts requests before they reach the transport. Request interceptors
 * run in registration order; each receives the request produced by the
 * previous interceptor. Throwing aborts the call with
 * {@code interceptor-error}; the transport is not invoked.
 */
@FunctionalInterface
public interface HttpRequestInterceptor {

    /** Returns the request to send — typically derived from {@code request}. */
    HttpRequest intercept(HttpRequest request);
}
