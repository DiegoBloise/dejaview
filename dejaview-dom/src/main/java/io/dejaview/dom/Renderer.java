package io.dejaview.dom;

import io.dejaview.core.View;

/**
 * Mounts a {@link View} tree into a platform container.
 *
 * <p>The contract is intentionally limited to mounting. Updating an already
 * mounted tree is described by {@link TreeReconciler} and implemented in a
 * later milestone. Keeping the two operations separate lets the runtime own a
 * mounted tree without depending on a concrete renderer.</p>
 */
public interface Renderer {

    /**
     * Mounts {@code view} into {@code container}.
     *
     * @return a handle that detaches the mounted nodes and releases listeners
     * @throws NullPointerException if any argument is {@code null}
     */
    MountedDom render(View view, DomDocument document, DomElement container);
}
