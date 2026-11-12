package io.dejaview.dom;

import java.util.List;

/**
 * A node in the mounted render tree.
 *
 * <p>Render nodes connect the immutable {@code View} model to the concrete DOM
 * nodes created by a renderer. They own listeners and child nodes so a mounted
 * tree can be disposed without re-walking the original view. They also expose
 * enough state for {@link TreeReconciler} to update the tree in place.</p>
 */
public sealed interface RenderNode
        permits TextRenderNode, ElementRenderNode, FragmentRenderNode, ComponentRenderNode {

    /**
     * Returns the DOM nodes this render node contributes to its parent.
     */
    List<DomNode> nodes();

    /**
     * Releases listeners and child nodes owned by this render node. The caller
     * is responsible for detaching {@link #nodes()} from the parent.
     */
    void dispose();
}
