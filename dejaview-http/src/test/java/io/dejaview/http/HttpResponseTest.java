package io.dejaview.http;

import io.dejaview.core.DejaViewException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpResponseTest {

    @Test
    void okIsTrueForTwoHundredsAndFalseOtherwise() {
        assertTrue(response(199 + 1).ok());
        assertTrue(response(204).ok());
        assertTrue(response(299).ok());
        assertFalse(response(199).ok());
        assertFalse(response(300).ok());
        assertFalse(response(404).ok());
        assertFalse(response(500).ok());
    }

    @Test
    void statusOutOfRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> response(99));
        assertThrows(IllegalArgumentException.class, () -> response(600));
    }

    @Test
    void nullBodyAndHeadersAndStatusTextAreNormalized() {
        HttpResponse response = new HttpResponse(200, null, null, null);

        assertEquals("", response.statusText());
        assertEquals("", response.body());
        assertTrue(response.headers().isEmpty());
    }

    @Test
    void jsonObjectRejectsNonObjectDocuments() {
        HttpResponse response = new HttpResponse(200, "OK", HttpHeaders.empty(), "[1,2]");

        DejaViewException error = assertThrows(DejaViewException.class, response::jsonObject);
        assertEquals("json-error", error.code());
    }

    private static HttpResponse response(int status) {
        return new HttpResponse(status, "", HttpHeaders.empty(), "");
    }
}
