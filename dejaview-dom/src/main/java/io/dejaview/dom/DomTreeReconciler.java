package io.dejaview.dom;

import io.dejaview.core.Component;
import io.dejaview.core.ComponentView;
import io.dejaview.core.DejaViewException;
import io.dejaview.core.ElementView;
import io.dejaview.core.FragmentView;
import io.dejaview.core.TextView;
import io.dejaview.core.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reconciler for the DOM render tree.
 *
 * <h2>Positional mode</h2>
 * <p>When no child in the new list carries a key, children of the same kind
 * are matched by position, which keeps updates predictable.</p>
 *
 * <h2>Keyed mode</h2>
 * <p>When at least one child in the new list carries a key, keyed children
 * are matched to current nodes by key and unkeyed children by relative order
 * between them. Keys that appear more than once in the new children list fail
 * fast with {@code DejaViewException} before any DOM node is moved. DOM order
 * is only re-anchored when the resulting order differs from the previous
 * one, so focus and selection survive updates that keep the order.</p>
 *
 * <h2>Component nodes</h2>
 * <p>Component nodes keep their instance across renders. A node replaces its
 * instance when the position is matched with a view whose component class
 * token differs from the token stored on the node. Both tokens must be
 * present for the swap to be detected; views built without a token always
 * reuse the instance.</p>
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
                    ? updateComponent(componentNode, component, document, parent, reference)
                    : replace(current, next, document, parent, reference);
            case ElementView element -> current instanceof ElementRenderNode elementNode
                    && elementNode.tagName().equals(element.tagName())
                    ? updateElement(elementNode, element, document, parent)
                    : replace(current, next, document, parent, reference);
        };
    }

    private static RenderNode updateText(TextRenderNode node, TextView view) {
        node.update(view.value());
        node.setKey(view.key());
        return node;
    }

    private static RenderNode updateFragment(
            FragmentRenderNode node,
            FragmentView view,
            DomDocument document,
            DomElement parent) {
        node.setKey(view.key());
        node.replaceChildren(reconcileChildren(node.children(), view.children(), document, parent));
        return node;
    }

    private static RenderNode updateComponent(
            ComponentRenderNode node,
            ComponentView view,
            DomDocument document,
            DomElement parent,
            DomNode reference) {
        Class<? extends Component> currentType = node.componentType();
        Class<? extends Component> nextType = view.componentType();
        if (currentType != null && nextType != null && currentType != nextType) {
            // The re-render would place a component of a different class at
            // this position. Replace the node instead of silently reusing the
            // current instance.
            return replace(node, view, document, parent, reference);
        }
        Component component = node.component();
        node.setKey(view.key());
        node.componentType(nextType);
        component.bind(view.bindings());
        RenderNode rendered = node.rendered();
        node.replaceRendered(reconcile(
                rendered,
                requireComponentView(component),
                document,
                parent,
                reference));
        component.onUpdate();
        return node;
    }

    private static RenderNode updateElement(
            ElementRenderNode node,
            ElementView view,
            DomDocument document,
            DomElement parent) {
        node.setKey(view.key());
        node.apply(view);
        node.replaceChildren(reconcileChildren(node.children(), view.children(), document, node.element()));
        return node;
    }

    private static View requireComponentView(Component component) {
        return Objects.requireNonNull(component.render(), "component view");
    }

    private static List<RenderNode> reconcileChildren(
            List<RenderNode> current,
            List<View> next,
            DomDocument document,
            DomElement parent) {
        for (View view : next) {
            if (view.key() != null) {
                return reconcileKeyedChildren(current, next, document, parent);
            }
        }
        return reconcilePositionalChildren(current, next, document, parent);
    }

    private static List<RenderNode> reconcilePositionalChildren(
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

    private static List<RenderNode> reconcileKeyedChildren(
            List<RenderNode> current,
            List<View> next,
            DomDocument document,
            DomElement parent) {
        rejectDuplicateKeys(next);

        Map<String, Integer> keyedIndex = new HashMap<>();
        List<Integer> unkeyed = new ArrayList<>();
        for (int index = 0; index < current.size(); index++) {
            String key = current.get(index).key();
            if (key == null) {
                unkeyed.add(index);
            } else {
                keyedIndex.putIfAbsent(key, index);
            }
        }

        boolean[] matched = new boolean[current.size()];
        boolean[] matchedNew = new boolean[next.size()];
        List<RenderNode> result = new ArrayList<>(next.size());
        int unkeyedCursor = 0;
        int previousMatched = -1;
        boolean increasing = true;
        boolean replacedNode = false;
        boolean appendedNew = false;

        // Pass 1: match and reconcile or build, in new order. New children
        // are appended at the end (or in place when anchored by the final
        // pass); the DOM order is corrected by pass 3 when it differs.
        for (int index = 0; index < next.size(); index++) {
            View view = next.get(index);
            int match = -1;
            if (view.key() != null) {
                Integer candidate = keyedIndex.remove(view.key());
                if (candidate != null) {
                    match = candidate;
                }
            } else {
                while (unkeyedCursor < unkeyed.size()) {
                    int candidate = unkeyed.get(unkeyedCursor);
                    unkeyedCursor++;
                    if (!matched[candidate]) {
                        match = candidate;
                        break;
                    }
                }
            }

            if (match >= 0) {
                matched[match] = true;
                matchedNew[index] = true;
                RenderNode updated = reconcile(
                        current.get(match), view, document, parent, null);
                if (updated != current.get(match)) {
                    replacedNode = true;
                }
                if (match <= previousMatched) {
                    increasing = false;
                }
                previousMatched = match;
                result.add(updated);
            } else {
                appendedNew = true;
                RenderNode added = RenderTreeBuilder.build(view, document);
                for (DomNode node : added.nodes()) {
                    parent.append(node);
                }
                result.add(added);
            }
        }

        // Pass 2: dispose what the new list no longer references.
        for (int index = 0; index < current.size(); index++) {
            if (matched[index]) {
                continue;
            }
            RenderNode removed = current.get(index);
            for (DomNode node : removed.nodes()) {
                parent.removeChild(node);
            }
            removed.dispose();
        }

        // Pass 3: re-anchor only when the DOM order cannot already match the
        // new order. Skipping this keeps focus and selection alive when only
        // attribute or text updates happened.
        int lastMatchedNewIndex = -1;
        for (int index = 0; index < next.size(); index++) {
            if (matchedNew[index]) {
                lastMatchedNewIndex = index;
            }
        }
        boolean allNewAtEnd = true;
        if (appendedNew) {
            for (int index = 0; index < next.size(); index++) {
                if (!matchedNew[index] && index < lastMatchedNewIndex) {
                    allNewAtEnd = false;
                    break;
                }
            }
        }
        boolean needsReanchor = !increasing || replacedNode || !allNewAtEnd;
        if (needsReanchor) {
            DomNode anchor = null;
            for (int index = result.size() - 1; index >= 0; index--) {
                List<DomNode> nodes = result.get(index).nodes();
                for (int nodeIndex = nodes.size() - 1; nodeIndex >= 0; nodeIndex--) {
                    parent.insertBefore(nodes.get(nodeIndex), anchor);
                    anchor = nodes.get(nodeIndex);
                }
            }
        }

        return result;
    }

    private static void rejectDuplicateKeys(List<View> next) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (View view : next) {
            String key = view.key();
            if (key == null) {
                continue;
            }
            if (!seen.add(key)) {
                throw new DejaViewException(
                        "duplicate-key",
                        "Duplicate key '" + key + "' in children list; keys must be unique among siblings",
                        null);
            }
        }
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
