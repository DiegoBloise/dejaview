package io.dejaview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResourceRegistryTest {

    @Test
    void disposesResourcesInReverseOrder() {
        ResourceRegistry registry = new ResourceRegistry();
        List<String> events = new ArrayList<>();

        registry.register(() -> events.add("first"));
        registry.register(() -> events.add("second"));

        registry.dispose();

        assertEquals(List.of("second", "first"), events);
        assertEquals(0, registry.size());
    }

    @Test
    void disposeIsIdempotent() {
        ResourceRegistry registry = new ResourceRegistry();
        int[] disposals = {0};

        registry.register(() -> disposals[0]++);

        registry.dispose();
        registry.dispose();

        assertEquals(1, disposals[0]);
        assertTrue(registry.isDisposed());
    }

    @Test
    void continuesDisposingAfterFailureAndPreservesCause() {
        ResourceRegistry registry = new ResourceRegistry();
        int[] disposals = {0};

        registry.register(() -> disposals[0]++);
        registry.register(() -> {
            throw new IllegalStateException("boom");
        });

        IllegalStateException failure = assertThrows(IllegalStateException.class, registry::dispose);

        assertEquals(1, disposals[0]);
        assertNotNull(failure.getCause());
        assertEquals("boom", failure.getCause().getMessage());
        assertTrue(registry.isDisposed());
    }

    @Test
    void rejectsRegistrationAfterDisposal() {
        ResourceRegistry registry = new ResourceRegistry();
        registry.dispose();

        assertThrows(IllegalStateException.class, () -> registry.register(() -> {
        }));
    }

    @Test
    void registerReturnsTheResource() {
        ResourceRegistry registry = new ResourceRegistry();
        AutoCloseable resource = () -> {
        };

        assertSame(resource, registry.register(resource));
        assertEquals(1, registry.size());
    }

    @Test
    void rejectsNullResources() {
        ResourceRegistry registry = new ResourceRegistry();

        assertThrows(NullPointerException.class, () -> registry.register(null));
    }
}
