package io.dejaview.dom;

import io.dejaview.core.View;
import io.dejaview.reactivity.Effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A view tree that has been mounted into a container.
 *
 * <p>The mounted tree owns one reactive effect. The effect renders the view,
 * tracks every signal or computed read during rendering, and reconciles the
 * mounted tree when one of them changes. Because a single effect drives the
 * whole tree, updates inside {@code Reactive.batch(...)} are coalesced into one
 * reconciliation.</p>
 *
 * <p>Disposing the result stops the effect, detaches every node that was
 * appended to the container and releases the listeners and child nodes owned by
 * the render tree. Calling {@link #dispose()} more than once has no additional
 * effect.</p>
 */
public final class MountedDom implements AutoCloseable {

    private final DomElement container;
    private final DomDocument document;
    private final View view;
    private final TreeReconciler reconciler;

    private RenderNode root;
    private Effect updateEffect;
    private boolean disposed;

    MountedDom(
            DomElement container,
            DomDocument document,
            View view,
            TreeReconciler reconciler) {
        this.container = Objects.requireNonNull(container, "container");
        this.document = Objects.requireNonNull(document, "document");
        this.view = Objects.requireNonNull(view, "view");
        this.reconciler = Objects.requireNonNull(reconciler, "reconciler");
    }

    /**
     * Performs the initial mount and starts tracking reactive dependencies.
     */
    void start() {
        updateEffect = new Effect(this::render);
    }

    /**
     * Returns whether this mounted tree has been disposed.
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Stops reactive updates, detaches the mounted nodes and releases their
     * listeners. Calling this method more than once has no additional effect.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;

        if (updateEffect != null) {
            updateEffect.dispose();
            updateEffect = null;
        }
        if (root != null) {
            for (DomNode node : root.nodes()) {
                container.removeChild(node);
            }
            root.dispose();
            root = null;
        }
    }

    @Override
    public void close() {
        dispose();
    }

    private void render() {
        if (disposed) {
            return;
        }
        if (root == null) {
            root = mountInitial();
        } else {
            root = reconciler.reconcile(root, view, document, container);
        }
    }

    private RenderNode mountInitial() {
        RenderNode mounted = RenderTreeBuilder.build(view, document);
        List<DomNode> appended = new ArrayList<>();
        try {
            for (DomNode node : mounted.nodes()) {
                container.append(node);
                appended.add(node);
            }
        } catch (RuntimeException | Error failure) {
            for (int index = appended.size() - 1; index >= 0; index--) {
                container.removeChild(appended.get(index));
            }
            mounted.dispose();
            throw failure;
        }
        return mounted;
    }
}
