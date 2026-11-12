package io.dejaview.http;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpHeadersTest {

    @Test
    void lookupIsCaseInsensitive() {
        HttpHeaders headers = HttpHeaders.builder().put("Content-Type", "application/json").build();

        assertEquals("application/json", headers.get("content-type"));
        assertEquals("application/json", headers.get("CONTENT-TYPE"));
        assertEquals("application/json", headers.get("Content-TYPE"));
    }

    @Test
    void putReplacesAndAddAppends() {
        HttpHeaders headers = HttpHeaders.builder()
                .add("X-Multi", "a")
                .add("X-Multi", "b")
                .put("X-Single", "1")
                .put("X-Single", "2")
                .build();

        assertEquals(List.of("a", "b"), headers.all("x-multi"));
        assertEquals(List.of("2"), headers.all("x-single"));
    }

    @Test
    void joinedJoinsMultipleValuesWithComma() {
        HttpHeaders headers = HttpHeaders.builder()
                .add("Accept", "text/html")
                .add("Accept", "application/json")
                .build();

        assertEquals("text/html, application/json", headers.joined("accept"));
    }

    @Test
    void namesPreserveInsertionOrderLowerCased() {
        HttpHeaders headers = HttpHeaders.builder()
                .put("B-One", "1")
                .put("A-Two", "2")
                .build();

        assertEquals(List.of("b-one", "a-two"), List.copyOf(headers.names()));
    }

    @Test
    void lineBreaksInValuesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpHeaders.builder().put("X", "a\r\nb"));
        assertThrows(IllegalArgumentException.class,
                () -> HttpHeaders.builder().put("X", "a\nb"));
    }

    @Test
    void blankNamesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpHeaders.builder().put("  ", "value"));
    }

    @Test
    void removeDropsValues() {
        HttpHeaders headers = HttpHeaders.builder()
                .put("X-Keep", "1")
                .put("X-Drop", "2")
                .build()
                .toBuilder()
                .remove("X-Drop")
                .build();

        assertNull(headers.get("x-drop"));
        assertEquals("1", headers.get("x-keep"));
    }

    @Test
    void emptyHasNoHeaders() {
        assertTrue(HttpHeaders.empty().isEmpty());
        assertEquals(0, HttpHeaders.empty().size());
        assertEquals(List.of(), HttpHeaders.empty().all("x"));
        assertNull(HttpHeaders.empty().joined("x"));
    }

    @Test
    void ofCreatesSingleValuedHeaders() {
        HttpHeaders headers = HttpHeaders.of(Map.of("A", "1", "B", "2"));

        assertEquals("1", headers.get("a"));
        assertEquals("2", headers.get("b"));
    }
}
