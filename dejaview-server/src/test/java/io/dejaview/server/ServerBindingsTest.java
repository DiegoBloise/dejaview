package io.dejaview.server;

import io.dejaview.core.DejaViewException;
import io.dejaview.di.Injector;
import io.dejaview.di.InjectorBuilder;
import io.dejaview.state.StateStorage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerBindingsTest {

    @Test
    void applyBindsTheServerPlatform() {
        InjectorBuilder builder = Injector.builder().name("server-test");
        ServerBindings.apply(builder);

        try (Injector root = builder.build()) {
            assertSame(ServerPlatform.INSTANCE, root.inject(ServerTokens.PLATFORM));
            assertSame(ServerPlatform.INSTANCE, root.inject(Platform.class));
        }
    }

    @Test
    void applyBindsStateStorageScopedPerRequestInjector() {
        InjectorBuilder builder = Injector.builder().name("server-test");
        ServerBindings.apply(builder);

        try (Injector root = builder.build()) {
            try (ServerContext first = ServerContext.create(root, ServerRequest.of("GET", "/a"));
                    ServerContext second = ServerContext.create(root, ServerRequest.of("GET", "/b"))) {
                StateStorage firstStorage = first.injector().inject(StateStorage.class);
                StateStorage secondStorage = second.injector().inject(StateStorage.class);

                // SCOPED: one instance per resolving injector, cached per request.
                assertSame(firstStorage, first.injector().inject(StateStorage.class));
                assertSame(secondStorage, second.injector().inject(StateStorage.class));
                assertNotSame(firstStorage, secondStorage);
            }
        }
    }

    @Test
    void applyingTwiceOnTheSameBuilderFailsFast() {
        InjectorBuilder builder = Injector.builder().name("server-test");
        ServerBindings.apply(builder);

        assertThrows(DejaViewException.class, () -> ServerBindings.apply(builder));
    }

    @Test
    void nullBuilderIsRejected() {
        assertThrows(NullPointerException.class, () -> ServerBindings.apply(null));
    }

    @Test
    void rootResolutionSharesOneStorageInstance() {
        InjectorBuilder builder = Injector.builder().name("server-test");
        ServerBindings.apply(builder);

        try (Injector root = builder.build()) {
            assertSame(root.inject(StateStorage.class), root.inject(StateStorage.class));
        }
    }
}
