package io.dejaview.dom;

import io.dejaview.core.Attribute;
import io.dejaview.core.ComponentView;
import io.dejaview.core.ElementView;
import io.dejaview.core.EventBinding;
import io.dejaview.core.FragmentView;
import io.dejaview.core.TextView;
import io.dejaview.core.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Renders an immutable {@link View} tree into a {@link DomDocument}.
 *
 * <p>The renderer contains no browser API calls. It depends only on the small
 * {@code Dom*} interfaces, which keeps the traversal logic testable on the JVM
 * and leaves the platform-specific code in a thin adapter.</p>
 *
 * <p>Rendering happens in two phases: the renderer first builds a
 * {@link RenderNode} tree that owns the created DOM nodes, listeners and child
 * nodes, then appends the resulting roots to the container. The returned
 * {@link MountedDom} handle can dispose the tree without re-walking the original
 * view.</p>
 *
 * <p>This is a first-mount renderer: diffing, keyed updates and hydration are
 * later milestones. The update contract is {@link TreeReconciler}.</p>
 */
public final class DomRenderer implements Renderer {

    /**
     * Appends {@code view} to {@code container}.
     *
     * @return a handle that detaches the mounted nodes and listeners
     * @throws NullPointerException if any argument is {@code null}
     */
    @Override
    public MountedDom render(View view, DomDocument document, DomElement container) {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(container, "container");

        RenderNode root = build(view, document);
        for (DomNode node : root.nodes()) {
            container.append(node);
        }
        return new MountedDom(container, List.of(root));
    }

    private static RenderNode build(View view, DomDocument document) {
        return switch (view) {
            case TextView text -> new TextRenderNode(document.createText(text.value()));
            case FragmentView fragment -> {
                List<RenderNode> children = new ArrayList<>();
                for (View child : fragment.children()) {
                    children.add(build(child, document));
                }
                yield new FragmentRenderNode(children);
            }
            case ComponentView component -> new ComponentRenderNode(build(component.resolve(), document));
            case ElementView element -> buildElement(element, document);
        };
    }

    private static RenderNode buildElement(ElementView view, DomDocument document) {
        DomElement element = document.createElement(view.tagName());
        for (Attribute attribute : view.attributes()) {
            element.setAttribute(attribute.name(), attribute.value());
        }

        List<DomEventRegistration> events = new ArrayList<>();
        for (EventBinding event : view.events()) {
            events.add(element.addEventListener(event.type(), event.handler()));
        }

        List<RenderNode> children = new ArrayList<>();
        for (View child : view.children()) {
            RenderNode childNode = build(child, document);
            for (DomNode node : childNode.nodes()) {
                element.append(node);
            }
            children.add(childNode);
        }

        return new ElementRenderNode(element, children, events);
    }
}
