package io.dejaview.dom;

import io.dejaview.core.Component;

import java.util.List;
import java.util.Objects;

/**
 * Render node for a mounted component.
 *
 * <p>The node owns one component instance for its whole lifetime. Re-renders
 * call {@link Component#render()} on that instance, so component state and
 * component-owned resources survive reactive updates. Disposal closes the
 * component before releasing the rendered subtree.</p>
 */
final class ComponentRenderNode implements RenderNode {

    private final Component component;
    private RenderNode rendered;

    private boolean disposed;

    ComponentRenderNode(Component component, RenderNode rendered) {
        this.component = Objects.requireNonNull(component, "component");
        this.rendered = Objects.requireNonNull(rendered, "rendered");
    }

    Component component() {
        return component;
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

        try {
            component.close();
        } finally {
            rendered.dispose();
        }
    }
}
