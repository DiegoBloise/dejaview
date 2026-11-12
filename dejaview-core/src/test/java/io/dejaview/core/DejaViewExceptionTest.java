package io.dejaview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class DejaViewExceptionTest {

    @Test
    void preservesMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("original");
        DejaViewException exception = new DejaViewException("wrapped", cause);

        assertEquals("wrapped", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertNull(exception.code());
    }

    @Test
    void preservesDiagnosticCode() {
        DejaViewException exception =
                new DejaViewException("DV-001", "diagnostic", new RuntimeException("cause"));

        assertEquals("DV-001", exception.code());
        assertEquals("diagnostic", exception.getMessage());
    }
}
