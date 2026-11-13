package io.dejaview.server;

import io.dejaview.core.DejaViewException;
import io.dejaview.core.ResourceRegistry;
import io.dejaview.di.Injector;
import io.dejaview.state.MemoryStateStorage;
import io.dejaview.state.StateStorage;

import java.util.Objects;

/**
 * Request-scoped execution context for server-side rendering.
 *
 * <p>One context exists per request. It owns:</p>
 * <ul>
 *   <li>a child {@link Injector} named {@code server.request}, created from the
 *       given parent and bound with {@link ServerTokens#REQUEST} and
 *       {@link ServerTokens#CONTEXT} as constants visible only inside the
 *       request subtree;</li>
 *   <li>a {@link ResourceRegistry} for request-scoped resources, disposed in
 *       reverse registration order on {@link #close()};</li>
 *   <li>a {@link PlatformCompatibilityRegistry} for classifying components
 *       during server rendering.</li>
 * </ul>
 *
 * <h2>State isolation</h2>
 * <p>If the parent injector cannot resolve {@link StateStorage}, the child binds
 * a fresh {@link MemoryStateStorage}. Components resolved inside the request
 * scope therefore observe request-isolated state out of the box, and nothing
 * leaks between two contexts of the same process. If the parent already
 * resolves {@code StateStorage} (for example because {@link ServerBindings} was
 * applied with the default {@code SCOPED} binding), that binding is left
 * untouched.</p>
 *
 * <p>Closing disposes the registry first, then closes the child injector
 * (which closes its own scoped instances before the parent). Closing is
 * idempotent. A closed context must not be reused; operations that touch the
 * injector fail fast.</p>
 */
public final class ServerContext implements AutoCloseable {

    private final ServerRequest request;
    private final Injector injector;
    private final ResourceRegistry resources = new ResourceRegistry();
    private final PlatformCompatibilityRegistry compatibility = new PlatformCompatibilityRegistry();

    private boolean closed;

    private ServerContext(Injector parent, ServerRequest request) {
        Objects.requireNonNull(parent, "parent");
        this.request = Objects.requireNonNull(request, "request");
        boolean stateStorageMissing = !parent.has(StateStorage.class);
        this.injector = parent.child("server.request");
        if (stateStorageMissing) {
            this.injector.bind(StateStorage.class).toInstance(new MemoryStateStorage());
        }
        this.injector.bind(ServerTokens.REQUEST).toInstance(request);
        this.injector.bind(ServerTokens.CONTEXT).toInstance(this);
    }

    /**
     * Creates a context for {@code request} under {@code parent}.
     *
     * @param parent  the application root injector; never closed by this method
     * @param request the request being handled, bound to
     *                {@link ServerTokens#REQUEST} inside the context
     * @return a live context; call {@link #close()} when the request completes
     */
    public static ServerContext create(Injector parent, ServerRequest request) {
        return new ServerContext(parent, request);
    }

    /**
     * Returns the request this context handles.
     */
    public ServerRequest request() {
        return request;
    }

    /**
     * Returns the request-scoped child injector. Bindings made here shadow the
     * parent and are visible only inside this request's subtree.
     *
     * @throws DejaViewException with code {@code closed-context} when the context
     *                           is closed
     */
    public Injector injector() {
        ensureOpen();
        return injector;
    }

    /**
     * Returns the registry for request-owned resources. Registering on a closed
     * context fails fast.
     */
    public ResourceRegistry resources() {
        return resources;
    }

    /**
     * Returns the component compatibility registry for this request.
     */
    public PlatformCompatibilityRegistry compatibility() {
        return compatibility;
    }

    /**
     * Disposes this context: registered resources first (reverse order), then
     * the child injector. Idempotent. If resource disposal fails, remaining
     * cleanup still runs and the first failure is rethrown after the injector
     * is closed.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        RuntimeException disposeFailure = null;
        try {
            resources.dispose();
        } catch (RuntimeException failure) {
            disposeFailure = failure;
        } finally {
            injector.close();
        }
        if (disposeFailure != null) {
            throw disposeFailure;
        }
    }

    /**
     * Returns whether this context has been closed.
     */
    public boolean isClosed() {
        return closed;
    }

    private void ensureOpen() {
        if (closed) {
            throw new DejaViewException(
                    "closed-context", "Server context for '" + request.path() + "' is closed", null);
        }
    }
}
