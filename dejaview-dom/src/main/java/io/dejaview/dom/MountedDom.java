package io.dejaview.dom;

import java.util.List;
import java.util.Objects;

/**
 * A view tree that has been mounted into a container.
 *
 * <p>Disposing the result detaches every node that was appended to the
 * container and removes all event listeners registered during the mount. This
 * is the minimal renderer lifecycle: it gives the runtime a way to release a
 * mounted tree without diffing or unmounting individual children.</p>
 */
public final class MountedDom implements AutoCloseable {

    private final DomElement container;
    private final List<DomNode> roots;
    private final List<DomEventRegistration> events;

    private boolean disposed;

    MountedDom(DomElement container, List<DomNode> roots, List<DomEventRegistration> events) {
        this.container = Objects.requireNonNull(container, "container");
        this.roots = List.copyOf(roots);
        this.events = List.copyOf(events);
    }

    /**
     * Returns whether this mounted tree has been disposed.
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Detaches the mounted nodes and removes their listeners. Calling this
     * method more than once has no additional effect.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;

        for (int index = events.size() - 1; index >= 0; index--) {
            events.get(index).remove();
        }
        for (int index = roots.size() - 1; index >= 0; index--) {
            container.removeChild(roots.get(index));
        }
    }

    @Override
    public void close() {
        dispose();
    }
}
