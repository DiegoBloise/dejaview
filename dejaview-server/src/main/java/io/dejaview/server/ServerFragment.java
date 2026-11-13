package io.dejaview.server;

import java.util.List;
import java.util.Objects;

/**
 * A transparent group of nodes in the server render tree.
 *
 * <p>Fragments splice their children into the parent during rendering; a
 * fragment never produces a wrapper element. {@link ServerFragment} exists as a
 * node type only for the case where the root of a render produces several nodes
 * and the tree still needs a single root.</p>
 *
 * @param children child nodes in render order, never {@code null}
 */
public record ServerFragment(List<ServerNode> children) implements ServerNode {

    public ServerFragment {
        Objects.requireNonNull(children, "children");
        for (ServerNode child : children) {
            Objects.requireNonNull(child, "children must not contain null");
        }
        children = List.copyOf(children);
    }
}
