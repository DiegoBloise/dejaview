package io.dejaview.dom;

import io.dejaview.core.DejaViewException;
import io.dejaview.core.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lifecycle owner for a mounted browser application.
 *
 * <p>A runtime binds a {@link DomDocument} and a container element, mounts view
 * trees into the container and releases them on shutdown. Shutdown is
 * idempotent and detaches mounted nodes and event listeners in reverse mount
 * order.</p>
 *
 * <p>Initialization happens once per runtime. Use {@link DejaView#start(View,
 * RuntimeConfig)} for the browser entry point, or {@link #create(DomDocument,
 * DomElement, RuntimeConfig)} to supply a platform implementation explicitly
 * (for example in tests).</p>
 */
public final class DejaViewRuntime implements AutoCloseable {

    private final DomDocument document;
    private final DomElement container;
    private final RuntimeConfig config;
    private final Renderer renderer;
    private final List<MountedDom> mounts = new ArrayList<>();

    private boolean started;
    private boolean disposed;

    private DejaViewRuntime(
            DomDocument document,
            DomElement container,
            RuntimeConfig config,
            Renderer renderer) {
        this.document = Objects.requireNonNull(document, "document");
        this.container = Objects.requireNonNull(container, "container");
        this.config = Objects.requireNonNull(config, "config");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    /**
     * Creates a runtime for the given platform document and container. The
     * runtime is not started until {@link #start(View)} is called.
     */
    public static DejaViewRuntime create(
            DomDocument document, DomElement container, RuntimeConfig config) {
        return create(document, container, config, new DomRenderer());
    }

    /**
     * Creates a runtime with an explicit renderer.
     */
    public static DejaViewRuntime create(
            DomDocument document,
            DomElement container,
            RuntimeConfig config,
            Renderer renderer) {
        return new DejaViewRuntime(document, container, config, renderer);
    }

    /**
     * Initializes the runtime by mounting the application view.
     *
     * @return this runtime
     * @throws IllegalStateException if the runtime was already initialized or
     *                               shut down
     */
    public DejaViewRuntime start(View view) {
        Objects.requireNonNull(view, "view");
        if (disposed) {
            throw new IllegalStateException("Runtime is shut down");
        }
        if (started) {
            throw new IllegalStateException("Runtime is already initialized");
        }
        started = true;
        mount(view);
        return this;
    }

    /**
     * Mounts an additional view tree into the runtime container.
     *
     * @return a handle that detaches the mounted tree
     * @throws IllegalStateException if the runtime is shut down
     */
    public MountedDom mount(View view) {
        Objects.requireNonNull(view, "view");
        ensureActive();
        MountedDom mounted = renderer.render(view, document, container);
        mounts.add(mounted);
        return mounted;
    }

    /**
     * Releases every mounted tree and listener. Calling this method more than
     * once has no additional effect.
     */
    public void shutdown() {
        if (disposed) {
            return;
        }
        disposed = true;

        RuntimeException failure = null;
        for (int index = mounts.size() - 1; index >= 0; index--) {
            try {
                mounts.get(index).dispose();
            } catch (RuntimeException disposalFailure) {
                if (failure == null) {
                    failure = new DejaViewException(
                            "Failed to dispose a mounted view", disposalFailure);
                } else {
                    failure.addSuppressed(disposalFailure);
                }
            }
        }
        mounts.clear();

        if (failure != null) {
            throw failure;
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    public DomDocument document() {
        return document;
    }

    public DomElement container() {
        return container;
    }

    public RuntimeConfig config() {
        return config;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isDisposed() {
        return disposed;
    }

    private void ensureActive() {
        if (disposed) {
            throw new IllegalStateException("Runtime is shut down");
        }
    }
}
