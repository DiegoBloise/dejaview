package io.dejaview.server;

import java.util.Objects;

/**
 * One boundary containment reported by a server render: a
 * {@link ServerErrorBoundary} that caught a failure of the subtree it was
 * protecting.
 *
 * @param boundaryType the fully-qualified class name of the boundary component
 * @param errorType    the fully-qualified class name of the caught exception
 * @param message      the exception message, or an empty string when the
 *                     exception carries none
 */
public record BoundaryError(String boundaryType, String errorType, String message) {

    public BoundaryError {
        boundaryType = Objects.requireNonNull(boundaryType, "boundaryType");
        errorType = Objects.requireNonNull(errorType, "errorType");
        message = message == null ? "" : message;
    }
}
