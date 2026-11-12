package io.dejaview.http;

import io.dejaview.core.DejaViewException;
import io.dejaview.core.internal.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * The DejaView HTTP client: a consistent Java API over the platform HTTP
 * transport.
 *
 * <p>Design notes:</p>
 * <ul>
 *   <li>Delivery is always asynchronous; the consumer is invoked exactly
 *       once. With a synchronous transport (or on the failure paths listed
 *       below) the consumer may be invoked before {@code send} returns.</li>
 *   <li>HTTP error statuses are reported as {@link HttpResult.Success} with
 *       the failing status; only transport-level failures are
 *       {@link HttpResult.Failure}. Use {@link HttpResponse#ok()}.</li>
 *   <li>Request interceptors run in registration order; response
 *       interceptors run in registration order and only observe
 *       {@link HttpResult.Success}. An interceptor that throws produces
 *       {@code interceptor-error}.</li>
 *   <li>Per-request timeouts and credentials inherit the client's defaults
 *       when the request does not override them. A client timeout of
 *       {@code -1} (the default) means "no explicit timeout"; the browser
 *       transport then disables the timeout.</li>
 *   <li>Consumer exceptions are not caught; they propagate on the calling
 *       thread (synchronous transports) or into the event dispatch stack
 *       (browser transport). Consumers should not throw.</li>
 *   <li>When no transport is configured, a browser transport is created on
 *       first use. When it is unavailable (e.g. on the JVM), {@code send}
 *       fails with {@code missing-transport} instead of throwing from
 *       static initializers.</li>
 * </ul>
 */
public final class HttpClient {

    private final HttpTransport configuredTransport;
    private final long defaultTimeoutMillis;
    private final HttpCredentials defaultCredentials;
    private final List<HttpRequestInterceptor> requestInterceptors;
    private final List<HttpResponseInterceptor> responseInterceptors;

    private final Object transportMonitor = new Object();
    private volatile HttpTransport lazyTransport;

    private HttpClient(Builder builder) {
        this.configuredTransport = builder.transport;
        this.defaultTimeoutMillis = builder.defaultTimeoutMillis;
        this.defaultCredentials = builder.defaultCredentials;
        this.requestInterceptors = List.copyOf(builder.requestInterceptors);
        this.responseInterceptors = List.copyOf(builder.responseInterceptors);
    }

    /** A fresh builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** Sends {@code GET url} with no extra configuration. */
    public HttpCall get(String url, Consumer<HttpResult> consumer) {
        return send(HttpRequest.get(url), consumer);
    }

    /** Sends {@code POST url} with no body. */
    public HttpCall post(String url, Consumer<HttpResult> consumer) {
        return send(HttpRequest.post(url), consumer);
    }

    /** Sends {@code PUT url} with no body. */
    public HttpCall put(String url, Consumer<HttpResult> consumer) {
        return send(HttpRequest.put(url), consumer);
    }

    /** Sends {@code PATCH url} with no body. */
    public HttpCall patch(String url, Consumer<HttpResult> consumer) {
        return send(HttpRequest.patch(url), consumer);
    }

    /** Sends {@code DELETE url}. */
    public HttpCall delete(String url, Consumer<HttpResult> consumer) {
        return send(HttpRequest.delete(url), consumer);
    }

    /**
     * Sends {@code request} and reports the outcome to {@code consumer}
     * exactly once. Interceptor failures are reported to the consumer as
     * {@code interceptor-error}; no exception is thrown for them.
     */
    public HttpCall send(HttpRequest request, Consumer<HttpResult> consumer) {
        Preconditions.requireNonNull(request, "request");
        Preconditions.requireNonNull(consumer, "consumer");

        AtomicBoolean done = new AtomicBoolean();
        Consumer<HttpResult> guarded = result -> {
            if (done.compareAndSet(false, true)) {
                consumer.accept(result);
            }
        };

        HttpRequest effective = request;
        try {
            for (HttpRequestInterceptor interceptor : requestInterceptors) {
                effective = interceptor.intercept(effective);
                if (effective == null) {
                    throw new DejaViewException("interceptor-error",
                            "request interceptor returned null", null);
                }
            }
            effective = effective
                    .timeoutMillis(effective.effectiveTimeout(defaultTimeoutMillis))
                    .credentials(effective.effectiveCredentials(defaultCredentials));
        } catch (Throwable t) {
            guarded.accept(interceptorFailure(t));
            return new CompletedCall(done);
        }

        HttpTransport transport;
        try {
            transport = resolveTransport();
        } catch (Throwable t) {
            guarded.accept(new HttpResult.Failure(new HttpFailure(
                    "missing-transport",
                    t.getMessage() != null ? t.getMessage() : "browser transport is not available",
                    t instanceof DejaViewException d ? d : t)));
            return new CompletedCall(done);
        }

        HttpCall inner = transport.execute(effective, result -> guarded.accept(applyResponseInterceptors(result)));
        return new ClientCall(inner, done);
    }

    private HttpResult applyResponseInterceptors(HttpResult result) {
        if (!(result instanceof HttpResult.Success success)) {
            return result;
        }
        HttpResponse response = success.response();
        try {
            for (HttpResponseInterceptor interceptor : responseInterceptors) {
                response = interceptor.intercept(response);
                if (response == null) {
                    throw new DejaViewException("interceptor-error",
                            "response interceptor returned null", null);
                }
            }
            return new HttpResult.Success(response);
        } catch (Throwable t) {
            return interceptorFailure(t);
        }
    }

    private static HttpResult.Failure interceptorFailure(Throwable t) {
        if (t instanceof DejaViewException d && "interceptor-error".equals(d.code())) {
            return new HttpResult.Failure(new HttpFailure("interceptor-error", d.getMessage(), d.getCause()));
        }
        return new HttpResult.Failure(new HttpFailure(
                "interceptor-error",
                t.getMessage() != null ? t.getMessage() : t.toString(),
                t));
    }

    private HttpTransport resolveTransport() {
        if (configuredTransport != null) {
            return configuredTransport;
        }
        HttpTransport cached = lazyTransport;
        if (cached != null) {
            return cached;
        }
        synchronized (transportMonitor) {
            if (lazyTransport == null) {
                try {
                    lazyTransport = new io.dejaview.http.internal.BrowserHttpTransport();
                } catch (Throwable t) {
                    throw new DejaViewException("missing-transport",
                            "the browser transport is not available: " + t, t);
                }
            }
            return lazyTransport;
        }
    }

    /** Wraps the transport's call so {@code isDone()} reflects delivery to the consumer. */
    private static final class ClientCall implements HttpCall {
        private final HttpCall inner;
        private final AtomicBoolean done;

        private ClientCall(HttpCall inner, AtomicBoolean done) {
            this.inner = inner;
            this.done = done;
        }

        @Override
        public void cancel() {
            if (inner != null) {
                inner.cancel();
            }
        }

        @Override
        public boolean isDone() {
            return done.get();
        }
    }

    /** A call to a request that never reached the transport; cancellation is a no-op. */
    private static final class CompletedCall implements HttpCall {
        private final AtomicBoolean done;

        private CompletedCall(AtomicBoolean done) {
            this.done = done;
        }

        @Override
        public void cancel() {
            // already settled
        }

        @Override
        public boolean isDone() {
            return done.get();
        }
    }

    /** Builder for {@link HttpClient}. */
    public static final class Builder {
        private HttpTransport transport;
        private long defaultTimeoutMillis = -1;
        private HttpCredentials defaultCredentials = HttpCredentials.SAME_ORIGIN;
        private final List<HttpRequestInterceptor> requestInterceptors = new ArrayList<>();
        private final List<HttpResponseInterceptor> responseInterceptors = new ArrayList<>();

        private Builder() {
        }

        /** Sets the transport. Defaults to the browser transport created on first use. */
        public Builder transport(HttpTransport transport) {
            this.transport = transport;
            return this;
        }

        /**
         * Sets the default timeout in milliseconds for requests that do not
         * specify one. {@code -1} (the default) disables the timeout;
         * {@code 0} also disables it explicitly. Values below {@code -1} are
         * rejected.
         */
        public Builder defaultTimeoutMillis(long defaultTimeoutMillis) {
            if (defaultTimeoutMillis < -1) {
                throw new IllegalArgumentException("defaultTimeoutMillis must be -1 or greater");
            }
            this.defaultTimeoutMillis = defaultTimeoutMillis;
            return this;
        }

        /** Sets the default credentials policy. Defaults to {@link HttpCredentials#SAME_ORIGIN}. */
        public Builder defaultCredentials(HttpCredentials defaultCredentials) {
            this.defaultCredentials = Preconditions.requireNonNull(defaultCredentials, "defaultCredentials");
            return this;
        }

        /** Registers a request interceptor; they run in registration order. */
        public Builder addRequestInterceptor(HttpRequestInterceptor interceptor) {
            this.requestInterceptors.add(Preconditions.requireNonNull(interceptor, "interceptor"));
            return this;
        }

        /** Registers a response interceptor; they run in registration order. */
        public Builder addResponseInterceptor(HttpResponseInterceptor interceptor) {
            this.responseInterceptors.add(Preconditions.requireNonNull(interceptor, "interceptor"));
            return this;
        }

        /** Builds the client. */
        public HttpClient build() {
            return new HttpClient(this);
        }
    }
}
