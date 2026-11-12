package io.dejaview.http;

/**
 * Service-provider interface between {@link HttpClient} and a concrete HTTP
 * transport (browser XHR, a JVM stub for tests, …). Implementations must
 * report the outcome exactly once, even when the call is cancelled.
 *
 * <p>The request handed to {@code execute} already carries effective
 * timeout and credentials (the client resolves client-level defaults before
 * dispatch), so transports only need to read {@link HttpRequest#timeoutMillis()}
 * and {@link HttpRequest#credentials()}.</p>
 */
public interface HttpTransport {

    /** Executes {@code request} and reports the outcome to {@code handler} exactly once. */
    HttpCall execute(HttpRequest request, java.util.function.Consumer<HttpResult> handler);
}
