package io.dejaview.server;

import io.dejaview.http.HttpHeaders;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerResponseTest {

    @Test
    void htmlFactoryProducesUtf8Html() {
        ServerResponse response = ServerResponse.html("<h1>Hello</h1>");

        assertEquals(200, response.status());
        assertEquals("<h1>Hello</h1>", response.body());
        assertEquals(ServerResponse.TEXT_HTML, response.headers().get("Content-Type"));
    }

    @Test
    void htmlStatusFactoryKeepsExplicitStatus() {
        ServerResponse response = ServerResponse.html(404, "<h1>Not found</h1>");

        assertEquals(404, response.status());
        assertEquals(ServerResponse.TEXT_HTML, response.headers().get("Content-Type"));
    }

    @Test
    void rejectsOutOfRangeStatus() {
        assertThrows(IllegalArgumentException.class, () -> new ServerResponse(99, null, null));
        assertThrows(IllegalArgumentException.class, () -> new ServerResponse(600, null, null));
    }

    @Test
    void nullHeadersAndBodyNormalize() {
        ServerResponse response = new ServerResponse(204, null, null);

        assertEquals(HttpHeaders.empty(), response.headers());
        assertEquals("", response.body());
    }

    @Test
    void withHeadersReturnsCopy() {
        ServerResponse original = ServerResponse.html("body");
        HttpHeaders headers = HttpHeaders.of(Map.of("Cache-Control", "no-store"));

        ServerResponse decorated = original.withHeaders(headers);

        assertEquals("no-store", decorated.headers().get("Cache-Control"));
        assertNull(decorated.headers().get("Content-Type"));
        assertEquals(ServerResponse.TEXT_HTML, original.headers().get("Content-Type"));
    }
}
