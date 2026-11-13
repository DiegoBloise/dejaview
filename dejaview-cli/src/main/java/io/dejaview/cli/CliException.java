package io.dejaview.cli;

/**
 * User-facing failure: the message is printed as-is and the process exits with
 * {@link #exitCode()}. Stack traces are never shown for these.
 */
public final class CliException extends Exception {

    private final int exitCode;

    public CliException(String message) {
        this(message, 1);
    }

    public CliException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }
}
