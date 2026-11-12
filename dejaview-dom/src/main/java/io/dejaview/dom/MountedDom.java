package io.dejaview.dom;

import java.util.List;
import java.util.Objects;

/**
 * A view tree that has been mounted into a container.
 *
 * <p>Disposing the result detaches every node that was appended to the
 * container and releases the listeners and child nodes owned by the render
 * tree. This is the minimal renderer lifecycle: it gives the runtime a way to
 * release a mounted tree without diffing or unmounting individual children.</p>
 */
public final class MountedDom implements AutoCloseable {

    private final DomElement container;
    private final List<RenderNode> roots;

    private boolean disposed;

    MountedDom(DomElement container, List<RenderNode> roots) {
        this.container = Objects.requireNonNull(container, "container");
        this.roots = List.copyOf(roots);
    }

    /**
     * Returns whether this mounted tree has been disposed.
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Detaches the mounted nodes and releases their listeners. Calling this
     * method more than once has no additional effect.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;

        for (int index = roots.size() - 1; index >= 0; index--) {
            RenderNode root = roots.get(index);
            for (DomNode node : root.nodes()) {
                container.removeChild(node);
            }
            root.dispose();
        }
    }

    @Override
    public void close() {
        dispose();
    }
}
