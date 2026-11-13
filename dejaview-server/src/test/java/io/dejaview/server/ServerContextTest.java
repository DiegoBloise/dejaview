package io.dejaview.server;

import io.dejaview.core.DejaViewException;
import io.dejaview.core.ResourceRegistry;
import io.dejaview.di.Injector;
import io.dejaview.state.StateStorage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerContextTest {

    private Injector newRoot() {
        return Injector.builder().name("test-root").build();
    }

    @Test
    void bindsRequestAndContextInsideTheChildScope() {
        try (Injector root = newRoot()) {
            try (ServerContext context = ServerContext.create(root, ServerRequest.of("GET", "/items"))) {
                ServerRequest request = context.injector().inject(ServerTokens.REQUEST);
                ServerContext bound = context.injector().inject(ServerTokens.CONTEXT);

                assertEquals("/items", request.path());
                assertSame(context, bound);
            }
        }
    }

    @Test
    void parentInjectorNeverSeesRequestBindings() {
        try (Injector root = newRoot()) {
            try (ServerContext context = ServerContext.create(root, ServerRequest.of("GET", "/items"))) {
                assertFalse(root.has(ServerTokens.REQUEST));
                assertFalse(root.has(ServerTokens.CONTEXT));
            }
        }
    }

    @Test
    void siblingContextsAreIsolated() {
        try (Injector root = newRoot()) {
            try (ServerContext first = ServerContext.create(root, ServerRequest.of("GET", "/a"));
                    ServerContext second = ServerContext.create(root, ServerRequest.of("GET", "/b"))) {
                StateStorage firstStorage = first.injector().inject(StateStorage.class);
                StateStorage secondStorage = second.injector().inject(StateStorage.class);

                firstStorage.write("k", "first");
                secondStorage.write("k", "second");

                assertEquals(Optional.of("first"), firstStorage.read("k"));
                assertEquals(Optional.of("second"), secondStorage.read("k"));
            }
        }
    }

    @Test
    void contextDoesNotShadowParentStateStorageBinding() {
        StateStorage marker = new StateStorage() {
            @Override
            public Optional<String> read(String key) {
                return Optional.of("shared");
            }

            @Override
            public void write(String key, String value) {
            }

            @Override
            public void remove(String key) {
            }
        };
        io.dejaview.di.InjectorBuilder builder = Injector.builder().name("marker-root");
        builder.bind(StateStorage.class).toInstance(marker);
        try (Injector root = builder.build()) {
            try (ServerContext context = ServerContext.create(root, ServerRequest.of("GET", "/"))) {
                assertSame(marker, context.injector().inject(StateStorage.class));
            }
        }
    }

    @Test
    void resourcesAreDisposedInReverseRegistrationOrderOnClose() {
        List<String> closed = new ArrayList<>();
        try (Injector root = newRoot()) {
            try (ServerContext context = ServerContext.create(root, ServerRequest.of("GET", "/"))) {
                context.resources().register(named("one", closed));
                context.resources().register(named("two", closed));
                context.resources().register(named("three", closed));
            }
        }
        assertEquals(List.of("three", "two", "one"), closed);
    }

    @Test
    void closeIsIdempotent() {
        List<String> closed = new ArrayList<>();
        try (Injector root = newRoot()) {
            ServerContext context = ServerContext.create(root, ServerRequest.of("GET", "/"));
            context.resources().register(named("one", closed));
            context.close();
            context.close();
        }
        assertEquals(List.of("one"), closed);
    }

    @Test
    void resourceDisposalFailureStillClosesEverythingElseAndRethrows() {
        List<String> closed = new ArrayList<>();
        try (Injector root = newRoot()) {
            ServerContext context = ServerContext.create(root, ServerRequest.of("GET", "/"));
            context.resources().register(named("one", closed));
            context.resources().register(failing("boom", closed));
            context.resources().register(named("three", closed));

            assertThrows(RuntimeException.class, context::close);
            assertEquals(List.of("three", "boom", "one"), closed);
            assertTrue(context.isClosed());

            // Injector was closed despite the failure.
            assertThrows(DejaViewException.class, () -> context.injector().inject(ServerTokens.REQUEST));
        }
    }

    @Test
    void closedContextFailsFastForInjectorAccess() {
        try (Injector root = newRoot()) {
            ServerContext context = ServerContext.create(root, ServerRequest.of("GET", "/"));
            context.close();

            DejaViewException failure = assertThrows(DejaViewException.class, context::injector);
            assertEquals("closed-context", failure.code());
        }
    }

    @Test
    void contextReportsRequestAndClosedState() {
        try (Injector root = newRoot()) {
            ServerContext context = ServerContext.create(root, ServerRequest.of("POST", "/submit"));

            assertEquals("POST", context.request().method());
            assertEquals("/submit", context.request().path());
            assertFalse(context.isClosed());

            context.close();
            assertTrue(context.isClosed());
        }
    }

    @Test
    void compatibilityRegistryIsPerContext() {
        try (Injector root = newRoot()) {
            try (ServerContext first = ServerContext.create(root, ServerRequest.of("GET", "/a"));
                    ServerContext second = ServerContext.create(root, ServerRequest.of("GET", "/b"))) {
                first.compatibility().register(String.class, PlatformCompatibility.BROWSER_ONLY);

                assertEquals(PlatformCompatibility.BROWSER_ONLY, first.compatibility().classify(String.class));
                assertEquals(PlatformCompatibility.UNIVERSAL, second.compatibility().classify(String.class));
            }
        }
    }

    @Test
    void registryDisposedThroughCloseStopsFurtherRegistration() {
        try (Injector root = newRoot()) {
            ServerContext context = ServerContext.create(root, ServerRequest.of("GET", "/"));
            ResourceRegistry resources = context.resources();
            context.close();

            assertThrows(IllegalStateException.class,
                    () -> resources.register(named("late", new ArrayList<>())));
        }
    }

    private static AutoCloseable named(String name, List<String> closed) {
        return () -> closed.add(name);
    }

    private static AutoCloseable failing(String name, List<String> closed) {
        return () -> {
            closed.add(name);
            throw new RuntimeException("failure closing " + name);
        };
    }
}
