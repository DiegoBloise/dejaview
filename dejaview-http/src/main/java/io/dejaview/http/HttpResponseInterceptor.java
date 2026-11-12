package io.dejaview.http;

/**
 * Intercepts responses after the transport completes. Response
 * interceptors run in registration order and only observe the result when
 * the round-trip completed — i.e. for {@link HttpResult.Success}, including
 * HTTP error statuses. Transport-level failures (network error, timeout,
 * cancellation) bypass response interceptors.
 *
 * <p>Throwing aborts the call with {@code interceptor-error}.</p>
 */
@FunctionalInterface
public interface HttpResponseInterceptor {

    /** Returns the response to deliver to the caller — typically derived from {@code response}. */
    HttpResponse intercept(HttpResponse response);
}
