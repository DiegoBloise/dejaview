package io.dejaview.http.internal;

import io.dejaview.http.HttpCall;
import io.dejaview.http.HttpCredentials;
import io.dejaview.http.HttpFailure;
import io.dejaview.http.HttpHeaders;
import io.dejaview.http.HttpRequest;
import io.dejaview.http.HttpResponse;
import io.dejaview.http.HttpResult;
import io.dejaview.http.HttpTransport;
import org.teavm.jso.JSBody;
import org.teavm.jso.ajax.ProgressEvent;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.dom.events.EventListener;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Browser {@link HttpTransport} backed by {@code XMLHttpRequest}.
 *
 * <p>{@code teavm-jso-apis} 0.16.0 ships typed XHR bindings but no Fetch
 * API, so this transport uses XHR. Notes on mapping:</p>
 * <ul>
 *   <li>{@code responseType} is left as text; the body is read as
 *       {@code responseText}.</li>
 *   <li>Multi-valued headers are joined with {@code ", "} (XHR's
 *       {@code setRequestHeader} appends otherwise).</li>
 *   <li>{@code withCredentials} is only settable as a whole:
 *       {@link HttpCredentials#INCLUDE} → {@code true};
 *       {@code OMIT}/{@code SAME_ORIGIN} → {@code false}. XHR cannot omit
 *       same-origin cookies; see ADR 0018 for the documented limitation.</li>
 *   <li>The XHR {@code timeout} property is not in the typed bindings and is
 *       set through a one-line JS interop bridge. A negative effective
 *       timeout becomes {@code 0} (disabled).</li>
 *   <li>{@code onLoad} with status 0 is treated as a network failure —
 *       it indicates the round-trip did not complete (including opaque
 *       cross-origin responses).</li>
 *   <li>Cancellation maps to {@code abort()}; the {@code abort} event is
 *       delivered as {@code cancelled}.</li>
 *   <li>Response headers are parsed from the raw
 *       {@code getAllResponseHeaders()} block; header names are
 *       lower-cased, {@code Set-Cookie} values are kept separately per
 *       line.</li>
 * </ul>
 *
 * <p>This class only works under TeaVM on the browser. The JVM test suite
 * uses fake transports; never reference this class from platform-neutral
 * code paths that must run on the JVM without TeaVM.</p>
 */
public final class BrowserHttpTransport implements HttpTransport {

    @Override
    public HttpCall execute(HttpRequest request, Consumer<HttpResult> handler) {
        Call call = new Call(request, handler);
        try {
            call.start();
        } catch (Throwable t) {
            call.fail("missing-transport",
                    "the browser transport failed to initialize: " + t, t);
        }
        return call;
    }

    @JSBody(params = {"xhr", "ms"}, script = "xhr.timeout = ms;")
    private static native void setTimeout(XMLHttpRequest xhr, int ms);

    @JSBody(params = {"xhr", "value"}, script = "xhr.withCredentials = value;")
    private static native void setWithCredentials(XMLHttpRequest xhr, boolean value);

    private static HttpHeaders parseResponseHeaders(String raw) {
        HttpHeaders.Builder builder = HttpHeaders.builder();
        if (raw == null || raw.isEmpty()) {
            return builder.build();
        }
        String[] lines = raw.split("\r\n|\n");
        for (String line : lines) {
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();
            if (!name.isEmpty()) {
                builder.add(name, value);
            }
        }
        return builder.build();
    }

    private static final class Call implements HttpCall {
        private final HttpRequest request;
        private final Consumer<HttpResult> handler;
        private final java.util.concurrent.atomic.AtomicBoolean settled =
                new java.util.concurrent.atomic.AtomicBoolean();

        private XMLHttpRequest xhr;

        private Call(HttpRequest request, Consumer<HttpResult> handler) {
            this.request = request;
            this.handler = handler;
        }

        private void start() {
            XMLHttpRequest xhr = XMLHttpRequest.create();
            this.xhr = xhr;

            xhr.open(request.method().name(), request.fullUrl(), true);

            long effectiveTimeout = request.timeoutMillis();
            int xhrTimeout = effectiveTimeout <= 0 ? 0 : (int) Math.min(effectiveTimeout, Integer.MAX_VALUE);
            setTimeout(xhr, xhrTimeout);

            setWithCredentials(xhr, request.credentials() == HttpCredentials.INCLUDE);

            request.headers().names().forEach(name -> {
                String joined = request.headers().joined(name);
                if (joined != null) {
                    xhr.setRequestHeader(name, joined);
                }
            });

            xhr.onLoad(new EventListener<ProgressEvent>() {
                @Override
                public void handleEvent(ProgressEvent event) {
                    int status = xhr.getStatus();
                    if (status == 0) {
                        fail("network-error", "request completed without a response (status 0)", null);
                        return;
                    }
                    succeed(new HttpResponse(
                            status,
                            xhr.getStatusText(),
                            parseResponseHeaders(xhr.getAllResponseHeaders()),
                            xhr.getResponseText()));
                }
            });
            xhr.onError(new EventListener<ProgressEvent>() {
                @Override
                public void handleEvent(ProgressEvent event) {
                    fail("network-error", "the request failed (network error, CORS, or DNS)", null);
                }
            });
            xhr.onTimeout(new EventListener<ProgressEvent>() {
                @Override
                public void handleEvent(ProgressEvent event) {
                    fail("timeout", "the request exceeded its timeout of " + xhrTimeout + " ms", null);
                }
            });
            xhr.onAbort(new EventListener<ProgressEvent>() {
                @Override
                public void handleEvent(ProgressEvent event) {
                    fail("cancelled", "the request was cancelled", null);
                }
            });

            String body = request.body();
            if (body != null) {
                xhr.send(body);
            } else {
                xhr.send();
            }
        }

        private void succeed(HttpResponse response) {
            if (settled.compareAndSet(false, true)) {
                handler.accept(new HttpResult.Success(response));
            }
        }

        private void fail(String code, String message, Throwable cause) {
            if (settled.compareAndSet(false, true)) {
                handler.accept(new HttpResult.Failure(new HttpFailure(code, message, cause)));
            }
        }

        @Override
        public void cancel() {
            XMLHttpRequest current = xhr;
            if (current == null) {
                fail("cancelled", "the request was cancelled before it started", null);
                return;
            }
            if (settled.get()) {
                return;
            }
            try {
                current.abort();
            } catch (Throwable t) {
                fail("cancelled", "the request was cancelled", t);
            }
            // abort() triggers onAbort, which settles with "cancelled";
            // guarantee settlement even if the event never fires.
            fail("cancelled", "the request was cancelled", null);
        }

        @Override
        public boolean isDone() {
            return settled.get();
        }
    }
}
