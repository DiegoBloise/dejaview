package io.dejaview.server;

/**
 * A node in the server render tree.
 *
 * <p>The server tree is the platform-neutral output of a server-side render. It
 * deliberately contains no browser references and no event handlers: events are
 * dropped during rendering (see {@link ServerTree#droppedEvents()}). A later
 * milestone serializes the tree to HTML for SSR and initial-state injection.</p>
 *
 * <p>Like {@code View}, server nodes are values: builders return new instances
 * instead of mutating receivers.</p>
 */
public sealed interface ServerNode permits ServerElement, ServerText, ServerFragment {
}
