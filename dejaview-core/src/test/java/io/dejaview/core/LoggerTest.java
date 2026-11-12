package io.dejaview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoggerTest {

    @Test
    void defaultMethodsDispatchToLog() {
        RecordingLogger logger = new RecordingLogger();

        logger.debug("debug");
        logger.info("info");
        logger.warn("warn");
        logger.error("error");

        assertEquals(
                List.of("DEBUG debug", "INFO info", "WARN warn", "ERROR error"),
                logger.messages);
    }

    @Test
    void errorCarriesTheFailure() {
        RecordingLogger logger = new RecordingLogger();
        IllegalStateException failure = new IllegalStateException("boom");

        logger.error("failed", failure);

        assertEquals("ERROR failed", logger.messages.get(0));
        assertSame(failure, logger.failures.get(0));
    }

    @Test
    void noopLoggerDiscardsMessages() {
        Loggers.noop().info("ignored");
    }

    private static final class RecordingLogger implements Logger {

        private final List<String> messages = new ArrayList<>();
        private final List<Throwable> failures = new ArrayList<>();

        @Override
        public void log(Level level, String message, Throwable failure) {
            messages.add(level + " " + message);
            failures.add(failure);
        }
    }
}
