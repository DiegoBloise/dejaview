package io.dejaview.dom;

import java.util.List;
import java.util.Objects;

/**
 * Render node for a resolved component view.
 */
final class ComponentRenderNode implements RenderNode {

    private final RenderNode rendered;

    ComponentRenderNode(RenderNode rendered) {
        this.rendered = Objects.requireNonNull(rendered, "rendered");
    }

    @Override
    public List<DomNode> nodes() {
        return rendered.nodes();
    }

    @Override
    public void dispose() {
        rendered.dispose();
    }
}
