package io.dejaview.dom;

import io.dejaview.core.Attribute;
import io.dejaview.core.ElementView;
import io.dejaview.core.EventBinding;
import io.dejaview.core.FragmentView;
import io.dejaview.core.TextView;
import io.dejaview.core.View;

import java.util.Objects;

/**
 * Renders an immutable {@link View} tree into a {@link DomDocument}.
 *
 * <p>The renderer contains no browser API calls. It depends only on the small
 * {@code Dom*} interfaces, which keeps the traversal logic testable on the JVM
 * and leaves the platform-specific code in a thin adapter.</p>
 *
 * <p>This is a first-mount renderer: it appends the view tree to the supplied
 * container. Diffing, keyed updates and hydration are later milestones and are
 * intentionally not implemented here.</p>
 */
public final class DomRenderer {

    /**
     * Appends {@code view} to {@code container}.
     *
     * @throws NullPointerException if any argument is {@code null}
     */
    public void render(View view, DomDocument document, DomElement container) {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(container, "container");
        append(view, document, container);
    }

    private static void append(View view, DomDocument document, DomElement parent) {
        switch (view) {
            case TextView text -> parent.append(document.createText(text.value()));
            case FragmentView fragment -> {
                for (View child : fragment.children()) {
                    append(child, document, parent);
                }
            }
            case ElementView element -> {
                DomElement domElement = document.createElement(element.tagName());
                for (Attribute attribute : element.attributes()) {
                    domElement.setAttribute(attribute.name(), attribute.value());
                }
                for (EventBinding event : element.events()) {
                    domElement.addEventListener(event.type(), event.handler());
                }
                for (View child : element.children()) {
                    append(child, document, domElement);
                }
                parent.append(domElement);
            }
        }
    }
}
