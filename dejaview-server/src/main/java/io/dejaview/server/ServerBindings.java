package io.dejaview.server;

import io.dejaview.di.InjectorBuilder;
import io.dejaview.di.Scope;
import io.dejaview.state.MemoryStateStorage;
import io.dejaview.state.StateStorage;

import java.util.Objects;

/**
 * Platform bindings a host applies once when building the root injector.
 *
 * <pre>{@code
 * InjectorBuilder builder = Injector.builder().name("server");
 * ServerBindings.apply(builder);
 * try (Injector root = builder.build()) {
 *     ...
 * }
 * }</pre>
 *
 * <h2>What gets bound</h2>
 * <ul>
 *   <li>{@link ServerTokens#PLATFORM} → the shared {@link ServerPlatform#INSTANCE}.
 *       It is stateless, so one instance serves the whole process.</li>
 *   <li>{@code StateStorage} → a {@link MemoryStateStorage} provider bound with
 *       {@link Scope#SCOPED}: one instance per resolving injector. Resolved from
 *       a request-scoped child (see {@link ServerContext}), each request gets
 *       isolated in-memory state; resolved from the root, the storage is shared
 *       and root-owned. Hosts that need durable or distributed state should bind
 *       their own {@code StateStorage} instead of calling this method's storage
 *       binding.</li>
 * </ul>
 *
 * <p>Binding the same token twice fails fast with {@code duplicate-binding};
 * a host that wants to customize a binding applies its own instead of
 * delegating to this method.</p>
 */
public final class ServerBindings {

    private ServerBindings() {
    }

    /**
     * Applies the server platform bindings to {@code builder}.
     *
     * @param builder the root injector builder the host is assembling
     */
    public static void apply(InjectorBuilder builder) {
        Objects.requireNonNull(builder, "builder");
        builder.bind(ServerTokens.PLATFORM).toInstance(ServerPlatform.INSTANCE);
        builder.bind(StateStorage.class).as(Scope.SCOPED).to(injector -> new MemoryStateStorage());
    }
}
