package io.dejaview.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * Render node that flattens several child nodes into its parent.
 */
final class FragmentRenderNode implements RenderNode {

    private List<RenderNode> children;
    private String key;

    private boolean disposed;

    FragmentRenderNode(List<RenderNode> children) {
        this.children = List.copyOf(children);
    }

    List<RenderNode> children() {
        return children;
    }

    void replaceChildren(List<RenderNode> next) {
        children = List.copyOf(next);
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
    public String key() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
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
