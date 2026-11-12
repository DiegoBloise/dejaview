package io.dejaview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationContextTest {

    private final FrameworkConfig config = FrameworkConfig.defaults();
    private final Logger logger = Loggers.noop();

    @Test
    void exposesConfigurationAndLogger() {
        ApplicationContext context = ApplicationContext.create(config, logger);

        assertSame(config, context.config());
        assertSame(logger, context.logger());
        assertFalse(context.isDisposed());
        assertEquals(0, context.resources().size());
    }

    @Test
    void disposesRegisteredResourcesInReverseOrder() {
        ApplicationContext context = ApplicationContext.create(config, logger);
        List<String> events = new ArrayList<>();

        context.register(() -> events.add("first"));
        context.register(() -> events.add("second"));

        context.dispose();

        assertEquals(List.of("second", "first"), events);
        assertTrue(context.isDisposed());
    }

    @Test
    void disposeIsIdempotent() {
        ApplicationContext context = ApplicationContext.create(config, logger);
        int[] disposals = {0};

        context.register(() -> disposals[0]++);

        context.dispose();
        context.dispose();

        assertEquals(1, disposals[0]);
    }

    @Test
    void rejectsRegistrationAfterDisposal() {
        ApplicationContext context = ApplicationContext.create(config, logger);
        context.dispose();

        assertThrows(IllegalStateException.class, () -> context.register(() -> {
        }));
    }

    @Test
    void rejectsNullDependencies() {
        assertThrows(NullPointerException.class, () -> ApplicationContext.create(null));
        assertThrows(NullPointerException.class, () -> ApplicationContext.create(config, null));
    }
}
