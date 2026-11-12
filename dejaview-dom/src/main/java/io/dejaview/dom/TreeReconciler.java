package io.dejaview.dom;

import io.dejaview.core.View;

/**
 * Updates an existing render tree to represent a new view tree.
 *
 * <p>Implementations must preserve DOM nodes whose view equivalent did not
 * change, so focus, selection and listeners survive updates.</p>
 */
public interface TreeReconciler {

    /**
     * Reconciles {@code current} with {@code next}.
     *
     * @param current  the render node currently mounted
     * @param next     the view the node must represent
     * @param document document used to create replacement nodes
     * @param parent   element that contains the current nodes
     * @return the render node representing {@code next}; either {@code current}
     *         or a replacement
     * @throws NullPointerException if any argument is {@code null}
     */
    RenderNode reconcile(RenderNode current, View next, DomDocument document, DomElement parent);
}
