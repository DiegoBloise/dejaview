package io.dejaview.server;

import java.util.List;
import java.util.Objects;

/**
 * The result of a server-side render: the produced tree plus a report of what
 * could not be carried across the boundary.
 *
 * @param root          the root node; a single node, or {@link ServerFragment}
 *                      wrapping the top-level nodes when the render produced
 *                      several. Never {@code null}
 * @param droppedEvents the de-duplicated, first-seen-order event type names
 *                      whose handlers were dropped during rendering (for example
 *                      {@code "click"}, {@code "input"}). Empty when the view had
 *                      no events. Never {@code null}
 * @param boundaryErrors one report per {@link ServerErrorBoundary} containment,
 *                      in containment order. Empty when no boundary caught an
 *                      error. Never {@code null}
 */
public record ServerTree(ServerNode root, List<String> droppedEvents, List<BoundaryError> boundaryErrors) {

    public ServerTree {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(droppedEvents, "droppedEvents");
        Objects.requireNonNull(boundaryErrors, "boundaryErrors");
        droppedEvents = List.copyOf(droppedEvents);
        boundaryErrors = List.copyOf(boundaryErrors);
    }

    /**
     * Creates a tree without boundary reports.
     */
    public ServerTree(ServerNode root, List<String> droppedEvents) {
        this(root, droppedEvents, List.of());
    }

    /**
     * Returns whether nothing was dropped or contained during this render.
     */
    public boolean isComplete() {
        return droppedEvents.isEmpty() && boundaryErrors.isEmpty();
    }
}
