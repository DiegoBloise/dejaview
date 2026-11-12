package io.dejaview.dom;

import io.dejaview.core.View;

import java.util.Objects;

/**
 * Renders an immutable {@link View} tree into a {@link DomDocument}.
 *
 * <p>The renderer contains no browser API calls. It depends only on the small
 * {@code Dom*} interfaces, which keeps the traversal logic testable on the JVM
 * and leaves the platform-specific code in a thin adapter.</p>
 *
 * <p>Mounting creates a render tree and returns a {@link MountedDom} handle.
 * The handle owns the reactive effect that keeps the tree in sync with any
 * signals read while rendering. Component views are the reactive boundary:
 * re-rendering a component produces a new view tree, and the
 * {@link TreeReconciler} updates the existing DOM nodes in place.</p>
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

        MountedDom mounted = new MountedDom(container, document, view, new DomTreeReconciler());
        mounted.start();
        return mounted;
    }
}
