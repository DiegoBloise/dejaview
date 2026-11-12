package io.dejaview.dom;

import io.dejaview.core.ComponentView;
import io.dejaview.core.ElementView;
import io.dejaview.core.FragmentView;
import io.dejaview.core.TextView;
import io.dejaview.core.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Positional reconciler for the DOM render tree.
 *
 * <p>Nodes of the same kind are updated in place. When the kind or tag name
 * changes, the old node is replaced and disposed. Children are matched by
 * position, which keeps updates predictable without introducing keys before the
 * list-rendering milestone.</p>
 */
final class DomTreeReconciler implements TreeReconciler {

    @Override
    public RenderNode reconcile(
            RenderNode current, View next, DomDocument document, DomElement parent) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(next, "next");
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(parent, "parent");

        return reconcile(current, next, document, parent, firstNode(current));
    }

    private static RenderNode reconcile(
            RenderNode current,
            View next,
            DomDocument document,
            DomElement parent,
            DomNode reference) {
        return switch (next) {
            case TextView text -> current instanceof TextRenderNode textNode
                    ? updateText(textNode, text)
                    : replace(current, next, document, parent, reference);
            case FragmentView fragment -> current instanceof FragmentRenderNode fragmentNode
                    ? updateFragment(fragmentNode, fragment, document, parent)
                    : replace(current, next, document, parent, reference);
            case ComponentView component -> current instanceof ComponentRenderNode componentNode
                    ? updateComponent(componentNode, document, parent, reference)
                    : replace(current, next, document, parent, reference);
            case ElementView element -> current instanceof ElementRenderNode elementNode
                    && elementNode.tagName().equals(element.tagName())
                    ? updateElement(elementNode, element, document, parent)
                    : replace(current, next, document, parent, reference);
        };
    }

    private static RenderNode updateText(TextRenderNode node, TextView view) {
        node.update(view.value());
        return node;
    }

    private static RenderNode updateFragment(
            FragmentRenderNode node,
            FragmentView view,
            DomDocument document,
            DomElement parent) {
        node.replaceChildren(reconcileChildren(node.children(), view.children(), document, parent));
        return node;
    }

    private static RenderNode updateComponent(
            ComponentRenderNode node,
            DomDocument document,
            DomElement parent,
            DomNode reference) {
        RenderNode rendered = node.rendered();
        node.replaceRendered(reconcile(
                rendered,
                Objects.requireNonNull(node.component().render(), "component view"),
                document,
                parent,
                reference));
        return node;
    }

    private static RenderNode updateElement(
            ElementRenderNode node,
            ElementView view,
            DomDocument document,
            DomElement parent) {
        node.apply(view);
        node.replaceChildren(reconcileChildren(node.children(), view.children(), document, node.element()));
        return node;
    }

    private static List<RenderNode> reconcileChildren(
            List<RenderNode> current,
            List<View> next,
            DomDocument document,
            DomElement parent) {
        List<RenderNode> result = new ArrayList<>();
        int common = Math.min(current.size(), next.size());

        for (int index = 0; index < common; index++) {
            result.add(reconcile(
                    current.get(index),
                    next.get(index),
                    document,
                    parent,
                    referenceAfter(current, index)));
        }

        for (int index = common; index < next.size(); index++) {
            RenderNode added = RenderTreeBuilder.build(next.get(index), document);
            for (DomNode node : added.nodes()) {
                parent.append(node);
            }
            result.add(added);
        }

        for (int index = next.size(); index < current.size(); index++) {
            RenderNode removed = current.get(index);
            for (DomNode node : removed.nodes()) {
                parent.removeChild(node);
            }
            removed.dispose();
        }

        return result;
    }

    /**
     * Returns the first DOM node at or after {@code index}. It is used as the
     * insertion reference when a node that contributes no DOM nodes (an empty
     * fragment) is replaced.
     */
    private static DomNode referenceAfter(List<RenderNode> nodes, int index) {
        for (int current = index; current < nodes.size(); current++) {
            List<DomNode> candidates = nodes.get(current).nodes();
            if (!candidates.isEmpty()) {
                return candidates.get(0);
            }
        }
        return null;
    }

    private static DomNode firstNode(RenderNode node) {
        List<DomNode> nodes = node.nodes();
        return nodes.isEmpty() ? null : nodes.get(0);
    }

    private static RenderNode replace(
            RenderNode current,
            View next,
            DomDocument document,
            DomElement parent,
            DomNode reference) {
        RenderNode replacement = RenderTreeBuilder.build(next, document);
        for (DomNode node : replacement.nodes()) {
            parent.insertBefore(node, reference);
        }
        for (DomNode node : current.nodes()) {
            parent.removeChild(node);
        }
        current.dispose();
        return replacement;
    }
}
