package io.dejaview.server;

import io.dejaview.http.HttpHeaders;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerRequestTest {

    @Test
    void createsRequestWithDefaults() {
        ServerRequest request = ServerRequest.of("GET", "/products");

        assertEquals("GET", request.method());
        assertEquals("/products", request.path());
        assertEquals(HttpHeaders.empty(), request.headers());
        assertEquals("", request.body());
    }

    @Test
    void rejectsBlankMethod() {
        assertThrows(IllegalArgumentException.class, () -> ServerRequest.of(" ", "/"));
        assertThrows(NullPointerException.class, () -> new ServerRequest(null, "/", null, null));
    }

    @Test
    void rejectsBlankPath() {
        assertThrows(IllegalArgumentException.class, () -> ServerRequest.of("GET", ""));
        assertThrows(IllegalArgumentException.class, () -> new ServerRequest("GET", "  ", null, null));
    }

    @Test
    void nullHeadersAndBodyNormalize() {
        ServerRequest request = new ServerRequest("POST", "/submit", null, null);

        assertEquals(HttpHeaders.empty(), request.headers());
        assertEquals("", request.body());
    }

    @Test
    void withHeadersAndBodyReturnCopies() {
        ServerRequest original = ServerRequest.of("GET", "/");
        HttpHeaders headers = HttpHeaders.of(Map.of("Accept", "text/html"));

        ServerRequest decorated = original.withHeaders(headers).withBody("payload");

        assertEquals("payload", decorated.body());
        assertEquals("text/html", decorated.headers().get("Accept"));
        assertEquals("", original.body());
        assertEquals(HttpHeaders.empty(), original.headers());
    }
}
