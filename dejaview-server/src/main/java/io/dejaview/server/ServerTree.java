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
 */
public record ServerTree(ServerNode root, List<String> droppedEvents) {

    public ServerTree {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(droppedEvents, "droppedEvents");
        droppedEvents = List.copyOf(droppedEvents);
    }

    /**
     * Returns whether nothing was dropped during this render.
     */
    public boolean isComplete() {
        return droppedEvents.isEmpty();
    }
}
