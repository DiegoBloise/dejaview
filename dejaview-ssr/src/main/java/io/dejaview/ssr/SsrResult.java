package io.dejaview.ssr;

import io.dejaview.server.BoundaryError;
import io.dejaview.server.ServerResponse;

import java.util.List;
import java.util.Objects;

/**
 * The outcome of one SSR render: the HTTP status, the HTML document and the
 * report produced by the render.
 *
 * <p>A failed render never leaks the failure to the browser: the HTML is a
 * minimal generic error page and the exception is attached here for the host to
 * log. A render that contained boundary failures or warnings is still a
 * success ({@link #status()} {@code 200}); the details are in
 * {@link #boundaryErrors()} and {@link #warnings()}.</p>
 *
 * @param status         the HTTP status code the host should send
 * @param html           the complete HTML document, never {@code null}
 * @param droppedEvents  event types dropped during rendering, in first-seen
 *                       order
 * @param boundaryErrors boundary containments, in containment order
 * @param warnings       non-fatal serialization findings, in validation order
 * @param error          the failure that aborted the render, or {@code null}
 *                       when the render succeeded
 */
public record SsrResult(
        int status,
        String html,
        List<String> droppedEvents,
        List<BoundaryError> boundaryErrors,
        List<SsrFinding> warnings,
        Throwable error) {

    public SsrResult {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("status must be between 100 and 599, got " + status);
        }
        Objects.requireNonNull(html, "html");
        droppedEvents = List.copyOf(droppedEvents);
        boundaryErrors = List.copyOf(boundaryErrors);
        warnings = List.copyOf(warnings);
    }

    /**
     * Returns whether the render completed without an unhandled failure. A
     * non-200 status chosen by the host (for example a 404 page) is still a
     * successful render.
     */
    public boolean isSuccess() {
        return error == null;
    }

    /**
     * Converts this result to a transport-neutral HTML response.
     *
     * <p>Boundary errors and warnings are not headers: they are for the host's
     * logs, not for the client.</p>
     */
    public ServerResponse toResponse() {
        return ServerResponse.html(status, html);
    }
}
