package io.dejaview.core.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PreconditionsTest {

    @Test
    void returnsValuesThatPassTheCheck() {
        Object value = new Object();

        assertSame(value, Preconditions.requireNonNull(value, "value"));
        assertEquals("text", Preconditions.requireText("text", "name"));
    }

    @Test
    void rejectsNullsAndBlankText() {
        assertThrows(NullPointerException.class, () -> Preconditions.requireNonNull(null, "value"));
        assertThrows(NullPointerException.class, () -> Preconditions.requireText(null, "name"));
        assertThrows(IllegalArgumentException.class, () -> Preconditions.requireText(" ", "name"));
    }
}
