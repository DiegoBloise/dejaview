package io.dejaview.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * Render node that flattens several child nodes into its parent.
 */
final class FragmentRenderNode implements RenderNode {

    private final List<RenderNode> children;

    private boolean disposed;

    FragmentRenderNode(List<RenderNode> children) {
        this.children = List.copyOf(children);
    }

    @Override
    public List<DomNode> nodes() {
        List<DomNode> nodes = new ArrayList<>();
        for (RenderNode child : children) {
            nodes.addAll(child.nodes());
        }
        return List.copyOf(nodes);
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;

        for (int index = children.size() - 1; index >= 0; index--) {
            children.get(index).dispose();
        }
    }
}
