package io.dejaview.dom;

import java.util.List;
import java.util.Objects;

/**
 * Render node for a resolved component view.
 */
final class ComponentRenderNode implements RenderNode {

    private RenderNode rendered;

    private boolean disposed;

    ComponentRenderNode(RenderNode rendered) {
        this.rendered = Objects.requireNonNull(rendered, "rendered");
    }

    RenderNode rendered() {
        return rendered;
    }

    void replaceRendered(RenderNode next) {
        rendered = Objects.requireNonNull(next, "next");
    }

    @Override
    public List<DomNode> nodes() {
        return rendered.nodes();
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        rendered.dispose();
    }
}
