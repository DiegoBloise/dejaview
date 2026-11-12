package io.dejaview.core;

/**
 * Minimal logging contract used by framework code.
 *
 * <p>Framework modules must not depend on a concrete logging backend. This
 * interface keeps diagnostics behind a small surface that can be adapted to the
 * console, a browser logger or a host application's logging system.</p>
 */
public interface Logger {

    /**
     * Severity levels, ordered from least to most severe.
     */
    enum Level {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    /**
     * Writes a message, optionally with the failure that caused it.
     */
    void log(Level level, String message, Throwable failure);

    default void log(Level level, String message) {
        log(level, message, null);
    }

    default void debug(String message) {
        log(Level.DEBUG, message);
    }

    default void info(String message) {
        log(Level.INFO, message);
    }

    default void warn(String message) {
        log(Level.WARN, message);
    }

    default void error(String message) {
        log(Level.ERROR, message);
    }

    default void error(String message, Throwable failure) {
        log(Level.ERROR, message, failure);
    }
}
