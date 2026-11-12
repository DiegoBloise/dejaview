package io.dejaview.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Disposes a group of resources in reverse registration order.
 *
 * <p>The registry is the shared implementation behind application contexts and
 * reactive scopes. Disposal is idempotent. If one resource fails to close, the
 * remaining resources are still disposed and the first failure is rethrown with
 * later failures attached as suppressed exceptions.</p>
 *
 * <p>Registries are not thread-safe.</p>
 */
public final class ResourceRegistry implements AutoCloseable {

    private final List<AutoCloseable> resources = new ArrayList<>();

    private boolean disposed;

    /**
     * Registers a resource for disposal.
     *
     * @return the same resource, for chaining
     * @throws IllegalStateException if the registry is already disposed
     */
    public <T extends AutoCloseable> T register(T resource) {
        Objects.requireNonNull(resource, "resource");
        if (disposed) {
            throw new IllegalStateException("Resource registry is disposed");
        }
        resources.add(resource);
        return resource;
    }

    /**
     * Disposes every registered resource in reverse registration order. Calling
     * this method more than once has no additional effect.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;

        IllegalStateException failure = null;
        for (int index = resources.size() - 1; index >= 0; index--) {
            try {
                resources.get(index).close();
            } catch (Exception exception) {
                if (failure == null) {
                    failure = new IllegalStateException(
                            "Failed to dispose a registered resource", exception);
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        resources.clear();

        if (failure != null) {
            throw failure;
        }
    }

    @Override
    public void close() {
        dispose();
    }

    /**
     * Returns whether the registry has been disposed.
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Returns the number of resources still registered.
     */
    public int size() {
        return resources.size();
    }
}
