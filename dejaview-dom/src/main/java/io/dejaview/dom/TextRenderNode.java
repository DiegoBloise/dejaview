package io.dejaview.dom;

import java.util.List;
import java.util.Objects;

/**
 * Render node for a platform text node.
 */
final class TextRenderNode implements RenderNode {

    private final DomNode node;

    TextRenderNode(DomNode node) {
        this.node = Objects.requireNonNull(node, "node");
    }

    @Override
    public List<DomNode> nodes() {
        return List.of(node);
    }

    @Override
    public void dispose() {
    }
}
