package io.dejaview.http;

import io.dejaview.core.DejaViewException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpClientTest {

    private static final HttpResponse PONG =
            new HttpResponse(200, "OK", HttpHeaders.empty(), "pong");

    private static HttpResponse jsonResponse() {
        return new HttpResponse(200, "OK", HttpHeaders.empty(), "{\"ok\":true}");
    }

    private static AtomicReference<HttpResult> send(HttpClient client, HttpRequest request) {
        AtomicReference<HttpResult> delivered = new AtomicReference<>();
        client.send(request, delivered::set);
        return delivered;
    }

    @Test
    void sendsGetWithMethodAndUrl() {
        FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(PONG));
        HttpClient client = HttpClient.builder().transport(transport).build();

        AtomicReference<HttpResult> delivered = send(client, HttpRequest.get("/api/ping"));

        assertEquals(HttpMethod.GET, transport.request.method());
        assertEquals("/api/ping", transport.request.fullUrl());
        assertTrue(delivered.get().isSuccess());
    }

    @Test
    void allMethodFactoriesSendMethod() {
        for (HttpMethod method : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)) {
            FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(PONG));
            HttpClient client = HttpClient.builder().transport(transport).build();

            HttpRequest request = switch (method) {
                case POST -> HttpRequest.post("/x");
                case PUT -> HttpRequest.put("/x");
                case PATCH -> HttpRequest.patch("/x");
                case DELETE -> HttpRequest.delete("/x");
                case GET -> throw new IllegalStateException();
            };
            send(client, request);
            assertEquals(method, transport.request.method());
        }
    }

    @Test
    void convenienceMethodsSendExpectedMethods() {
        FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(PONG));
        HttpClient client = HttpClient.builder().transport(transport).build();

        client.get("/a", r -> { });
        assertEquals(HttpMethod.GET, transport.request.method());
        client.post("/a", r -> { });
        assertEquals(HttpMethod.POST, transport.request.method());
        client.put("/a", r -> { });
        assertEquals(HttpMethod.PUT, transport.request.method());
        client.patch("/a", r -> { });
        assertEquals(HttpMethod.PATCH, transport.request.method());
        client.delete("/a", r -> { });
        assertEquals(HttpMethod.DELETE, transport.request.method());
    }

    @Test
    void headersQueryParamsAndBodyReachTheTransport() {
        FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(PONG));
        HttpClient client = HttpClient.builder().transport(transport).build();

        HttpRequest request = HttpRequest.post("/api/echo")
                .header("X-Token", "abc")
                .queryParam("q", "hello world")
                .queryParam("lang", "pt/BR")
                .body("payload");
        send(client, request);

        assertEquals("abc", transport.request.headers().get("x-token"));
        assertEquals("payload", transport.request.body());
        assertEquals("/api/echo?q=hello+world&lang=pt%2FBR", transport.request.fullUrl());
    }

    @Test
    void queryParamsEncodeReservedCharacters() {
        FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(PONG));
        HttpClient client = HttpClient.builder().transport(transport).build();

        send(client, HttpRequest.get("/s").queryParam("a", "1 2").queryParam("b", "x&y=z"));

        assertEquals("/s?a=1+2&b=x%26y%3Dz", transport.request.fullUrl());
    }

    @Test
    void requestInterceptorsRunInOrderAndSeePreviousChanges() {
        FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(PONG));
        HttpClient client = HttpClient.builder()
                .transport(transport)
                .addRequestInterceptor(req -> req.header("X-Order", "1"))
                .addRequestInterceptor(req -> req.header("X-Order",
                        req.headers().get("X-Order") + ",2"))
                .build();

        send(client, HttpRequest.get("/api"));

        assertEquals("1,2", transport.request.headers().get("X-Order"));
    }

    @Test
    void throwingRequestInterceptorProducesInterceptorErrorAndSkipsTransport() {
        FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(PONG));
        HttpClient client = HttpClient.builder()
                .transport(transport)
                .addRequestInterceptor(req -> {
                    throw new IllegalStateException("boom");
                })
                .build();

        AtomicReference<HttpResult> delivered = send(client, HttpRequest.get("/api"));

        assertTrue(delivered.get() instanceof HttpResult.Failure);
        assertEquals("interceptor-error", delivered.get().failure().code());
        assertEquals("boom", delivered.get().failure().message());
        assertEquals(null, transport.request); // never dispatched
    }

    @Test
    void nullReturningRequestInterceptorProducesInterceptorError() {
        FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(PONG));
        HttpClient client = HttpClient.builder()
                .transport(transport)
                .addRequestInterceptor(req -> null)
                .build();

        AtomicReference<HttpResult> delivered = send(client, HttpRequest.get("/api"));

        assertEquals("interceptor-error", delivered.get().failure().code());
    }

    @Test
    void responseInterceptorsRunInOrderOnSuccess() {
        FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(PONG));
        HttpClient client = HttpClient.builder()
                .transport(transport)
                .addResponseInterceptor(res -> new HttpResponse(res.status(), res.statusText(),
                        res.headers().toBuilder().put("X-First", "1").build(), res.body() + "-1"))
                .addResponseInterceptor(res -> new HttpResponse(res.status(), res.statusText(),
                        res.headers().toBuilder().put("X-Second", "2").build(), res.body() + "-2"))
                .build();

        AtomicReference<HttpResult> delivered = send(client, HttpRequest.get("/api"));

        HttpResponse response = delivered.get().response();
        assertEquals("pong-1-2", response.body());
        assertEquals("1", response.headers().get("x-first"));
        assertEquals("2", response.headers().get("x-second"));
    }

    @Test
    void throwingResponseInterceptorProducesInterceptorError() {
        FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(PONG));
        HttpClient client = HttpClient.builder()
                .transport(transport)
                .addResponseInterceptor(res -> {
                    throw new IllegalStateException("bad response");
                })
                .build();

        AtomicReference<HttpResult> delivered = send(client, HttpRequest.get("/api"));

        assertEquals("interceptor-error", delivered.get().failure().code());
        assertEquals("bad response", delivered.get().failure().message());
    }

    @Test
    void responseInterceptorsAreSkippedOnTransportFailure() {
        FakeTransport transport = new FakeTransport().respondingWith(
                HttpResult.Failure.of("network-error", "down", null));
        boolean[] called = {false};
        HttpClient client = HttpClient.builder()
                .transport(transport)
                .addResponseInterceptor(res -> {
                    called[0] = true;
                    return res;
                })
                .build();

        AtomicReference<HttpResult> delivered = send(client, HttpRequest.get("/api"));

        assertFalse(called[0]);
        assertEquals("network-error", delivered.get().failure().code());
    }

    @Test
    void responseInterceptorsObserveErrorStatuses() {
        FakeTransport transport = new FakeTransport().respondingWith(
                new HttpResult.Success(new HttpResponse(500, "Server Error", HttpHeaders.empty(), "oops")));
        HttpClient client = HttpClient.builder()
                .transport(transport)
                .addResponseInterceptor(res -> new HttpResponse(res.status(), res.statusText(),
                        res.headers(), res.body() + "-intercepted"))
                .build();

        AtomicReference<HttpResult> delivered = send(client, HttpRequest.get("/api"));

        assertEquals("oops-intercepted", delivered.get().response().body());
        assertFalse(delivered.get().response().ok());
    }

    @Test
    void networkErrorIsReportedAsFailure() {
        FakeTransport transport = new FakeTransport().respondingWith(
                HttpResult.Failure.of("network-error", "unreachable", new RuntimeException("refused")));
        HttpClient client = HttpClient.builder().transport(transport).build();

        AtomicReference<HttpResult> delivered = send(client, HttpRequest.get("/api"));

        assertFalse(delivered.get().isSuccess());
        assertEquals("network-error", delivered.get().failure().code());
        assertSame(RuntimeException.class, delivered.get().failure().cause().getClass());
    }

    @Test
    void requestTimeoutWinsOverClientDefault() {
        FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(PONG));
        HttpClient client = HttpClient.builder()
                .transport(transport)
                .defaultTimeoutMillis(5_000)
                .build();

        send(client, HttpRequest.get("/api").timeoutMillis(250));

        assertEquals(250, transport.request.timeoutMillis());
    }

    @Test
    void clientTimeoutIsUsedWhenRequestDoesNotOverride() {
        FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(PONG));
        HttpClient client = HttpClient.builder()
                .transport(transport)
                .defaultTimeoutMillis(5_000)
                .build();

        send(client, HttpRequest.get("/api"));

        assertEquals(5_000, transport.request.timeoutMillis());
    }

    @Test
    void clientTimeoutDefaultsToDisabled() {
        FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(PONG));
        HttpClient client = HttpClient.builder().transport(transport).build();

        send(client, HttpRequest.get("/api"));

        assertEquals(HttpRequest.TIMEOUT_INHERIT, transport.request.timeoutMillis());
    }

    @Test
    void requestCredentialsWinOverClientDefault() {
        FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(PONG));
        HttpClient client = HttpClient.builder()
                .transport(transport)
                .defaultCredentials(HttpCredentials.INCLUDE)
                .build();

        send(client, HttpRequest.get("/api").credentials(HttpCredentials.OMIT));

        assertEquals(HttpCredentials.OMIT, transport.request.credentials());
    }

    @Test
    void clientCredentialsDefaultAppliesAndDefaultsToSameOrigin() {
        FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(PONG));
        HttpClient client = HttpClient.builder().transport(transport).build();

        send(client, HttpRequest.get("/api"));

        assertEquals(HttpCredentials.SAME_ORIGIN, transport.request.credentials());
    }

    @Test
    void consumerIsInvokedExactlyOnceWhenTransportDoubleFires() {
        FakeTransport transport = new FakeTransport() {
            @Override
            public HttpCall execute(HttpRequest request, java.util.function.Consumer<HttpResult> handler) {
                this.request = request;
                handler.accept(new HttpResult.Success(PONG));
                handler.accept(new HttpResult.Success(PONG));
                return call;
            }
        };
        HttpClient client = HttpClient.builder().transport(transport).build();

        int[] count = {0};
        client.send(HttpRequest.get("/api"), r -> count[0]++);

        assertEquals(1, count[0]);
    }

    @Test
    void cancellationReachesConsumerAsCancelled() {
        FakeTransport transport = new FakeTransport().manual();
        HttpClient client = HttpClient.builder().transport(transport).build();

        AtomicReference<HttpResult> delivered = new AtomicReference<>();
        HttpCall call = client.send(HttpRequest.get("/api"), delivered::set);

        assertFalse(transport.call.cancelled);
        assertFalse(transport.call.isDone());

        call.cancel();
        assertTrue(transport.call.cancelled);

        transport.deliver(HttpResult.Failure.of("cancelled", "the request was cancelled", null));
        assertEquals("cancelled", delivered.get().failure().code());
        assertTrue(call.isDone());
    }

    @Test
    void missingBrowserTransportFailsWithStableCodeOnJvm() {
        HttpClient client = HttpClient.builder().build();

        AtomicReference<HttpResult> delivered = send(client, HttpRequest.get("/api"));

        assertTrue(delivered.get() instanceof HttpResult.Failure);
        assertEquals("missing-transport", delivered.get().failure().code());
    }

    @Test
    void jsonResponseCanBeReadAsTree() {
        FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(jsonResponse()));
        HttpClient client = HttpClient.builder().transport(transport).build();

        AtomicReference<HttpResult> delivered = send(client, HttpRequest.get("/api"));

        JsonValue.JsonObject body = delivered.get().response().jsonObject();
        assertEquals(Boolean.TRUE, body.getBoolean("ok"));
    }

    @Test
    void jsonBodySetsContentTypeAndSerializes() {
        FakeTransport transport = new FakeTransport().respondingWith(new HttpResult.Success(PONG));
        HttpClient client = HttpClient.builder().transport(transport).build();

        send(client, HttpRequest.post("/api")
                .jsonBody(Json.parse("{\"name\":\"Ada\"}")));

        assertEquals("{\"name\":\"Ada\"}", transport.request.body());
        assertEquals("application/json", transport.request.headers().get("content-type"));
    }

    @Test
    void malformedJsonThrowsJsonErrorWithCode() {
        FakeTransport transport = new FakeTransport().respondingWith(
                new HttpResult.Success(new HttpResponse(200, "OK", HttpHeaders.empty(), "{nope")));
        HttpClient client = HttpClient.builder().transport(transport).build();

        AtomicReference<HttpResult> delivered = send(client, HttpRequest.get("/api"));

        DejaViewException error =
                assertThrows(DejaViewException.class, () -> delivered.get().response().json());
        assertEquals("json-error", error.code());
    }

    @Test
    void invalidDefaultTimeoutIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpClient.builder().defaultTimeoutMillis(-2));
    }

    @Test
    void negativeRequestTimeoutIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpRequest.get("/api").timeoutMillis(-5));
    }

    @Test
    void nullRequestThrows() {
        HttpClient client = HttpClient.builder().build();
        assertThrows(NullPointerException.class, () -> client.send(null, r -> { }));
    }
}
