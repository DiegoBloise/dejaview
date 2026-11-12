package io.dejaview.core;

import java.util.Objects;

/**
 * Factories for the built-in {@link Logger} implementations.
 */
public final class Loggers {

    private Loggers() {
    }

    /**
     * Returns a logger that discards every message.
     */
    public static Logger noop() {
        return NoopLogger.INSTANCE;
    }

    /**
     * Returns a logger that writes to standard output and standard error.
     */
    public static Logger system() {
        return SystemLogger.INSTANCE;
    }

    private enum NoopLogger implements Logger {
        INSTANCE;

        @Override
        public void log(Level level, String message, Throwable failure) {
        }
    }

    private enum SystemLogger implements Logger {
        INSTANCE;

        @Override
        public void log(Level level, String message, Throwable failure) {
            Objects.requireNonNull(level, "level");
            Objects.requireNonNull(message, "message");

            String line = "[" + level + "] " + message;
            if (level == Level.WARN || level == Level.ERROR) {
                System.err.println(line);
                if (failure != null) {
                    failure.printStackTrace(System.err);
                }
            } else {
                System.out.println(line);
            }
        }
    }
}
