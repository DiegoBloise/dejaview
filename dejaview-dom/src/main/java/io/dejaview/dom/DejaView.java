package io.dejaview.dom;

import io.dejaview.core.View;

import java.util.Objects;

/**
 * Browser entry point for a DejaView application.
 *
 * <p>The current runtime performs a first mount only: it renders the supplied
 * view tree into a container element. A component system, diffing and reactive
 * updates are planned for later milestones.</p>
 */
public final class DejaView {

    private DejaView() {
    }

    /**
     * Renders {@code view} into the element with the given id.
     *
     * @return the container element
     * @throws IllegalArgumentException if no element has the given id
     */
    public static DomElement mount(View view, String containerId) {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(containerId, "containerId");
        DomDocument document = Dom.document();
        DomElement container = document.getElementById(containerId);
        new DomRenderer().render(view, document, container);
        return container;
    }

    /**
     * Renders {@code view} into an existing container element.
     */
    public static void mount(View view, DomElement container) {
        Objects.requireNonNull(container, "container");
        new DomRenderer().render(view, Dom.document(), container);
    }
}
