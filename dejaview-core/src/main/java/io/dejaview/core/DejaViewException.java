package io.dejaview.core;

/**
 * A framework error with an optional stable code and the original cause.
 *
 * <p>The code is intended for diagnostics and tests, not for control flow.
 * Preserving the cause is required so browser and compiler errors remain
 * inspectable after the framework wraps them.</p>
 */
public class DejaViewException extends RuntimeException {

    private final String code;

    public DejaViewException(String message) {
        this(null, message, null);
    }

    public DejaViewException(String message, Throwable cause) {
        this(null, message, cause);
    }

    public DejaViewException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * Returns the stable diagnostic code, or {@code null} when none was given.
     */
    public String code() {
        return code;
    }
}
