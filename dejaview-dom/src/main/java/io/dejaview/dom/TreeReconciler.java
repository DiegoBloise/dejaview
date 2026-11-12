package io.dejaview.dom;

import io.dejaview.core.View;

/**
 * Updates an existing render tree to represent a new view tree.
 *
 * <p>This is the contract for in-place reconciliation. Implementations must
 * preserve DOM nodes whose view equivalent did not change, so focus, selection
 * and listeners survive updates. Sprint 05 only defines the contract; the
 * in-place implementation belongs to Sprint 06.</p>
 */
public interface TreeReconciler {

    /**
     * Reconciles {@code current} with {@code next}.
     *
     * @param current  the render node currently mounted
     * @param next     the view the node must represent
     * @param document document used to create replacement nodes
     * @return the render node representing {@code next}; either {@code current}
     *         or a replacement
     * @throws NullPointerException if any argument is {@code null}
     */
    RenderNode reconcile(RenderNode current, View next, DomDocument document);
}
