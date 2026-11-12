package io.dejaview.dom;

import io.dejaview.core.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Render node for a mounted component.
 *
 * <p>The node owns one component instance for its whole lifetime. Re-renders
 * call {@link Component#render()} on that instance, so component state and
 * component-owned resources survive reactive updates. Disposal runs the
 * destruction lifecycle before releasing the rendered subtree.</p>
 *
 * <p>The factory that produced the instance is kept for diagnostics, and the
 * component class token of the view is kept so the reconciler can detect
 * component-type swaps at the same template position without instantiating a
 * probe. A node replaces its instance when both tokens are present and
 * differ, or when the view's key is present on both sides and differs;
 * otherwise the instance is reused.</p>
 */
final class ComponentRenderNode implements RenderNode {

    private final Component component;
    private final Supplier<? extends Component> factory;
    private Class<? extends Component> componentType;
    private RenderNode rendered;

    private String key;
    private boolean disposed;

    ComponentRenderNode(
            Component component,
            RenderNode rendered,
            Supplier<? extends Component> factory,
            Class<? extends Component> componentType) {
        this.component = Objects.requireNonNull(component, "component");
        this.rendered = Objects.requireNonNull(rendered, "rendered");
        this.factory = Objects.requireNonNull(factory, "factory");
        this.componentType = componentType;
    }

    Component component() {
        return component;
    }

    Supplier<? extends Component> factory() {
        return factory;
    }

    Class<? extends Component> componentType() {
        return componentType;
    }

    void componentType(Class<? extends Component> next) {
        if (next != null) {
            componentType = next;
        }
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

        try {
            component.onDestroy();
        } finally {
            try {
                component.close();
            } finally {
                rendered.dispose();
            }
        }
    }
}
