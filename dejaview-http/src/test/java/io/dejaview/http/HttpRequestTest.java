package io.dejaview.http;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRequestTest {

    @Test
    void factoriesSetMethod() {
        assertEquals(HttpMethod.GET, HttpRequest.get("/a").method());
        assertEquals(HttpMethod.POST, HttpRequest.post("/a").method());
        assertEquals(HttpMethod.PUT, HttpRequest.put("/a").method());
        assertEquals(HttpMethod.PATCH, HttpRequest.patch("/a").method());
        assertEquals(HttpMethod.DELETE, HttpRequest.delete("/a").method());
    }

    @Test
    void fluentMethodsReturnNewInstances() {
        HttpRequest base = HttpRequest.get("/api");
        HttpRequest updated = base.header("X-A", "1").timeoutMillis(100).body("b");

        assertEquals(HttpHeaders.empty(), base.headers());
        assertEquals(HttpRequest.TIMEOUT_INHERIT, base.timeoutMillis());
        assertNull(base.body());

        assertEquals("1", updated.headers().get("x-a"));
        assertEquals(100, updated.timeoutMillis());
        assertEquals("b", updated.body());
    }

    @Test
    void fullUrlAppendsEncodedQueryParams() {
        HttpRequest request = HttpRequest.get("/search")
                .queryParam("q", "crème brûlée")
                .queryParam("safe", "a/b");

        assertEquals("/search?q=cr%C3%A8me+br%C3%BBl%C3%A9e&safe=a%2Fb", request.fullUrl());
    }

    @Test
    void fullUrlAppendsWithAmpersandWhenQueryExists() {
        HttpRequest request = HttpRequest.get("/search?keep=1").queryParam("added", "2");

        assertEquals("/search?keep=1&added=2", request.fullUrl());
    }

    @Test
    void jsonBodySerializesAndSetsContentType() {
        HttpRequest request = HttpRequest.post("/api")
                .jsonBody(Json.parse("{\"greeting\":\"hello\"}"));

        assertEquals("{\"greeting\":\"hello\"}", request.body());
        assertEquals("application/json", request.headers().get("content-type"));
    }

    @Test
    void jsonBodyKeepsExplicitContentType() {
        HttpRequest request = HttpRequest.post("/api")
                .header("Content-Type", "application/vnd.thing+json")
                .jsonBody(Json.parse("{\"a\":1}"));

        assertEquals("application/vnd.thing+json", request.headers().get("content-type"));
    }

    @Test
    void blankUrlIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.get("  "));
        assertThrows(NullPointerException.class, () -> HttpRequest.get(null));
    }

    @Test
    void queryParamValidatesInputs() {
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.get("/a").queryParam(" ", "v"));
        assertThrows(NullPointerException.class, () -> HttpRequest.get("/a").queryParam("k", null));
    }

    @Test
    void credentialsAndTimeoutInheritByDefault() {
        HttpRequest request = HttpRequest.get("/a");

        assertEquals(HttpRequest.TIMEOUT_INHERIT, request.timeoutMillis());
        assertNull(request.credentials());
    }

    @Test
    void queryParamsAreReturnedInOrder() {
        HttpRequest request = HttpRequest.get("/a")
                .queryParam("b", "2")
                .queryParam("a", "1");

        assertEquals(List.of(new HttpRequest.QueryParam("b", "2"), new HttpRequest.QueryParam("a", "1")),
                request.queryParams());
    }
}
