package io.dejaview.dom;

import java.util.List;
import java.util.Objects;

/**
 * Render node for an element, its listeners and its children.
 */
final class ElementRenderNode implements RenderNode {

    private final DomElement element;
    private final List<RenderNode> children;
    private final List<DomEventRegistration> events;

    private boolean disposed;

    ElementRenderNode(
            DomElement element, List<RenderNode> children, List<DomEventRegistration> events) {
        this.element = Objects.requireNonNull(element, "element");
        this.children = List.copyOf(children);
        this.events = List.copyOf(events);
    }

    @Override
    public List<DomNode> nodes() {
        return List.of(element);
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;

        for (int index = events.size() - 1; index >= 0; index--) {
            events.get(index).remove();
        }
        for (int index = children.size() - 1; index >= 0; index--) {
            children.get(index).dispose();
        }
    }
}
