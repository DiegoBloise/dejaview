package io.dejaview.dom;

import io.dejaview.core.View;

import java.util.Objects;

/**
 * Browser entry point for a DejaView application.
 *
 * <p>{@link #start(View, RuntimeConfig)} initializes a runtime that can be shut
 * down explicitly. {@link #mount(View, String)} is the first-mount convenience
 * used by early examples; it leaves the runtime attached for the lifetime of the
 * page. A component system, diffing and reactive updates are planned for later
 * milestones.</p>
 */
public final class DejaView {

    private DejaView() {
    }

    /**
     * Starts a runtime in the current browser document and mounts {@code view}.
     *
     * @return the running runtime, which owns the mounted tree
     * @throws IllegalStateException if no browser environment is available
     */
    public static DejaViewRuntime start(View view, RuntimeConfig config) {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(config, "config");
        if (!BrowserEnvironment.isBrowser()) {
            throw new IllegalStateException(
                    "DejaView requires a browser environment to start the runtime");
        }

        DomDocument document = Dom.document();
        DomElement container = document.getElementById(config.containerId());
        return DejaViewRuntime.create(document, container, config).start(view);
    }

    /**
     * Renders {@code view} into the element with the given id and returns the
     * container. Use {@link #start(View, RuntimeConfig)} when the runtime needs
     * to be shut down explicitly.
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
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(container, "container");
        new DomRenderer().render(view, Dom.document(), container);
    }
}
