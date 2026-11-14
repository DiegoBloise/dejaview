package io.dejaview.ssg;

import io.dejaview.core.DejaViewException;

/**
 * A failure that aborted a static site generation.
 *
 * <p>Build output is intentionally explicit: the message names the route or
 * asset that failed, so a CI log points at the offending page instead of a
 * generic stack trace. The original cause is preserved.</p>
 */
public final class SsgException extends DejaViewException {

    /** Stable diagnostic code used by every generation failure. */
    public static final String CODE = "ssg-generation-failed";

    public SsgException(String message) {
        super(CODE, message, null);
    }

    public SsgException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
