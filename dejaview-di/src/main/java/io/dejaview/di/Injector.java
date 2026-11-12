package io.dejaview.di;

import io.dejaview.core.DejaViewException;
import io.dejaview.core.ResourceRegistry;
import io.dejaview.core.internal.Preconditions;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Hierarchical dependency container.
 *
 * <p>An injector holds bindings created through {@link InjectorBuilder}, caches
 * instances according to their {@link Scope}, and delegates to its parent when it
 * cannot resolve a token itself. Child injectors are cheap and intended to model
 * request or view scopes: bindings on a child shadow those on the parent, and
 * scoped instances are created and owned per scope.</p>
 *
 * <h2>Resolution rules</h2>
 * <ol>
 *   <li>A registration on the resolving injector or its nearest ancestor wins;
 *       nearer injectors shadow farther ones.</li>
 *   <li>{@link Scope#SINGLETON} caches in the injector that registered the
 *       provider; the instance is owned by that injector and shared by the whole
 *       subtree.</li>
 *   <li>{@link Scope#SCOPED} caches in the injector that resolved the provider;
 *       each scope gets its own instance, owned by that scope.</li>
 *   <li>{@link Scope#TRANSIENT} never caches and never owns.</li>
 * </ol>
 *
 * <h2>Factories and disposal</h2>
 * <p>Providers receive the injector that owns the instance they produce
 * ({@code SINGLETON}: the registering injector; {@code SCOPED} and
 * {@code TRANSIENT}: the resolving injector). Instances implementing
 * {@link AutoCloseable} are closed by the owning injector in reverse creation
 * order when it closes; children close before their parent. Transient instances
 * and constant instances bound with
 * {@link TokenBinding#toInstance(Object) toInstance} are never closed by the
 * injector.</p>
 *
 * <p>Instances are resolved lazily. Bindings can be added to a live injector
 * with {@link #bind(InjectionToken)}, which is how child scopes shadow parent
 * bindings. An injector is not thread-safe. Closing is idempotent.</p>
 */
public final class Injector implements AutoCloseable {

    private static final ThreadLocal<Deque<InjectionToken<?>>> RESOLUTION_PATH =
            ThreadLocal.withInitial(ArrayDeque::new);

    private final Injector parent;
    private final Registry registry;
    private final Map<InjectionToken<?>, Object> singletonCache = new java.util.LinkedHashMap<>();
    private final Map<InjectionToken<?>, Object> scopedCache = new java.util.LinkedHashMap<>();
    private final List<Injector> children = new ArrayList<>();
    private final ResourceRegistry resources = new ResourceRegistry();

    private boolean closed;

    Injector(Registry registry, Injector parent) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.parent = parent;
        if (parent != null) {
            parent.children.add(this);
        }
    }

    /**
     * Creates a builder for a root injector.
     */
    public static InjectorBuilder builder() {
        return new InjectorBuilder();
    }

    /**
     * Creates an empty child scope with the given diagnostic name.
     *
     * <p>The child inherits every provider visible to this injector and can
     * shadow individual bindings with {@link #bind(InjectionToken)}.</p>
     */
    public Injector child(String name) {
        ensureOpen();
        return new Injector(new Registry(Preconditions.requireText(name, "scope name")), this);
    }

    /**
     * Creates an empty child scope named after this injector with a sequence
     * number, e.g. {@code root.child-1}.
     */
    public Injector child() {
        ensureOpen();
        return new Injector(new Registry(name() + ".child-" + (children.size() + 1)), this);
    }

    /**
     * Starts a binding on this live injector. Registrations added this way are
     * visible immediately and shadow any binding with the same token inherited
     * from an ancestor.
     *
     * <p>Binding a token that is already bound on this injector fails fast with
     * {@code duplicate-binding}, even when called on a closed-over live scope.</p>
     *
     * @throws DejaViewException with code {@code closed-injector} when this
     *                           injector is closed
     */
    public <T> TokenBinding<T> bind(InjectionToken<T> token) {
        ensureOpen();
        return registry.bind(Objects.requireNonNull(token, "token"));
    }

    /**
     * Starts a binding for the canonical token of {@code type}. See
     * {@link #bind(InjectionToken)}.
     */
    public <T> TokenBinding<T> bind(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return bind(InjectionToken.of(type));
    }

    /**
     * Returns the injector this scope resolves against when a token is not bound
     * here, or {@code null} for the root.
     */
    public Injector parent() {
        return parent;
    }

    /**
     * Diagnostic name used in error messages.
     */
    public String name() {
        return registry.name();
    }

    /**
     * Returns whether a provider for {@code token} exists in this injector or an
     * ancestor, without instantiating anything.
     */
    public boolean has(InjectionToken<?> token) {
        Objects.requireNonNull(token, "token");
        ensureOpen();
        return lookup(token) != null;
    }

    /**
     * Returns whether a provider for the canonical token of {@code type} exists.
     */
    public <T> boolean has(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return has(InjectionToken.of(type));
    }

    /**
     * Resolves {@code token}, creating the instance when required by its scope.
     *
     * @throws DejaViewException with code {@code missing-provider} when no
     *                           provider exists in this injector or its ancestors
     * @throws DejaViewException with code {@code circular-dependency} when the
     *                           resolution path closes a loop
     * @throws DejaViewException with code {@code provider-error} when a factory
     *                           throws a non-{@code DejaViewException}
     * @throws DejaViewException with code {@code closed-injector} when this
     *                           injector is closed
     */
    public <T> T inject(InjectionToken<T> token) {
        Objects.requireNonNull(token, "token");
        ensureOpen();

        Injector owner = lookup(token);
        if (owner == null) {
            throw missingProvider(token);
        }
        @SuppressWarnings("unchecked")
        Registry.TokenRegistration<T> registration =
                (Registry.TokenRegistration<T>) owner.registry.get(token);
        return resolve(registration, owner);
    }

    /**
     * Resolves the canonical token for {@code type}.
     */
    public <T> T inject(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return inject(InjectionToken.of(type));
    }

    /**
     * Closes this scope and everything it owns: child scopes first, then cached
     * instances, in reverse creation order. Child scopes created after this call
     * are not registered; closing is idempotent.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        List<Injector> copy = new ArrayList<>(children);
        for (int index = copy.size() - 1; index >= 0; index--) {
            copy.get(index).close();
        }
        children.clear();

        try {
            resources.dispose();
        } finally {
            if (parent != null) {
                parent.children.remove(this);
            }
        }
    }

    /**
     * Returns whether this injector is closed.
     */
    public boolean isClosed() {
        return closed;
    }

    private Injector lookup(InjectionToken<?> token) {
        Injector current = this;
        while (current != null) {
            if (current.registry.contains(token)) {
                return current;
            }
            current = current.parent;
        }
        return null;
    }

    private <T> T resolve(Registry.TokenRegistration<T> registration, Injector owner) {
        Scope scope = registration.scope();

        Deque<InjectionToken<?>> path = RESOLUTION_PATH.get();
        if (path.contains(registration.token())) {
            String chain = path.stream().map(InjectionToken::name)
                    .collect(java.util.stream.Collectors.joining(" -> "));
            throw new DejaViewException(
                    "circular-dependency",
                    "Circular dependency detected: " + chain + " -> " + registration.token().name(),
                    null);
        }

        if (scope == Scope.SINGLETON) {
            Object cached = owner.singletonCache.get(registration.token());
            if (cached != null) {
                return cast(registration.token(), cached);
            }
        } else if (scope == Scope.SCOPED) {
            Object cached = scopedCache.get(registration.token());
            if (cached != null) {
                return cast(registration.token(), cached);
            }
        }

        Injector receiver = scope == Scope.SINGLETON ? owner : this;
        path.addLast(registration.token());
        T created;
        try {
            created = callProvider(registration, receiver);
        } finally {
            path.removeLast();
        }

        if (scope == Scope.SINGLETON) {
            owner.singletonCache.put(registration.token(), created);
            if (registration.owned() && created instanceof AutoCloseable closeable) {
                owner.resources.register(closeable);
            }
        } else if (scope == Scope.SCOPED) {
            scopedCache.put(registration.token(), created);
            if (registration.owned() && created instanceof AutoCloseable closeable) {
                resources.register(closeable);
            }
        }
        return created;
    }

    private <T> T callProvider(Registry.TokenRegistration<T> registration, Injector injector) {
        try {
            T created = registration.provider().provide(injector);
            return Objects.requireNonNull(created,
                    "Provider for token '" + registration.token().name() + "' returned null");
        } catch (DejaViewException failure) {
            throw failure;
        } catch (RuntimeException | Error failure) {
            throw new DejaViewException(
                    "provider-error",
                    "Provider for token '" + registration.token().name()
                            + "' failed: " + failure.getMessage(),
                    failure);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(InjectionToken<T> token, Object instance) {
        // Tokens carry no runtime type; the API boundary provides static safety.
        // An unchecked cast cannot fail for registry-created instances.
        return (T) instance;
    }

    private void ensureOpen() {
        if (closed) {
            throw new DejaViewException(
                    "closed-injector",
                    "Injector '" + name() + "' is closed",
                    null);
        }
    }

    private <T> DejaViewException missingProvider(InjectionToken<T> token) {
        Deque<InjectionToken<?>> path = RESOLUTION_PATH.get();
        StringBuilder message = new StringBuilder("No provider for '")
                .append(token.name())
                .append("' in injector '")
                .append(name())
                .append('\'');
        if (!path.isEmpty()) {
            message.append(" while resolving ")
                    .append(path.stream().map(InjectionToken::name)
                            .collect(java.util.stream.Collectors.joining(" -> ")));
        }
        return new DejaViewException("missing-provider", message.toString(), null);
    }
}
