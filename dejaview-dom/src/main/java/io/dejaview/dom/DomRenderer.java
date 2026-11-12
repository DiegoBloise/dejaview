package io.dejaview.dom;

import io.dejaview.core.Attribute;
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
 * <p>This is a first-mount renderer: it appends the view tree to the supplied
 * container and returns a {@link MountedDom} handle. Diffing, keyed updates and
 * hydration are later milestones and are intentionally not implemented here.</p>
 */
public final class DomRenderer {

    /**
     * Appends {@code view} to {@code container}.
     *
     * @return a handle that detaches the mounted nodes and listeners
     * @throws NullPointerException if any argument is {@code null}
     */
    public MountedDom render(View view, DomDocument document, DomElement container) {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(container, "container");

        List<DomNode> roots = new ArrayList<>();
        List<DomEventRegistration> events = new ArrayList<>();
        append(view, document, container, roots, events);
        return new MountedDom(container, roots, events);
    }

    private static void append(
            View view,
            DomDocument document,
            DomElement parent,
            List<DomNode> roots,
            List<DomEventRegistration> events) {
        switch (view) {
            case TextView text -> appendNode(document.createText(text.value()), parent, roots);
            case FragmentView fragment -> {
                for (View child : fragment.children()) {
                    append(child, document, parent, roots, events);
                }
            }
            case ElementView element -> {
                DomElement domElement = document.createElement(element.tagName());
                for (Attribute attribute : element.attributes()) {
                    domElement.setAttribute(attribute.name(), attribute.value());
                }
                for (EventBinding event : element.events()) {
                    events.add(domElement.addEventListener(event.type(), event.handler()));
                }
                for (View child : element.children()) {
                    append(child, document, domElement, null, events);
                }
                appendNode(domElement, parent, roots);
            }
        }
    }

    private static void appendNode(DomNode node, DomElement parent, List<DomNode> roots) {
        parent.append(node);
        if (roots != null) {
            roots.add(node);
        }
    }
}
