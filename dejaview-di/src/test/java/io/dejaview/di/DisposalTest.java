package io.dejaview.di;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.dejaview.core.DejaViewException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DisposalTest {

    record ClosableService(List<String> events, String id) implements AutoCloseable {
        @Override
        public void close() {
            events.add(id);
        }
    }

    private static Injector buildInjector(java.util.function.Consumer<InjectorBuilder> configure) {
        InjectorBuilder builder = Injector.builder();
        configure.accept(builder);
        return builder.build();
    }

    @Test
    void singletonAutoCloseableIsClosedInReverseCreationOrder() {
        List<String> events = new ArrayList<>();
        InjectionToken<ClosableService> first = InjectionToken.of("first");
        InjectionToken<ClosableService> second = InjectionToken.of("second");
        Injector injector = buildInjector(builder -> {
            builder.bind(first).to(child -> new ClosableService(events, "first"));
            builder.bind(second).to(child -> new ClosableService(events, "second"));
        });

        injector.inject(first);
        injector.inject(second);
        injector.close();

        assertEquals(List.of("second", "first"), events);
    }

    @Test
    void childrenAreClosedBeforeTheirParent() {
        List<String> events = new ArrayList<>();
        Injector parent = buildInjector(builder -> builder.bind(ClosableService.class)
                .as(Scope.SINGLETON).to(injector -> new ClosableService(events, "parent")));
        Injector child = parent.child("request");
        child.bind(ClosableService.class).as(Scope.SCOPED).to(injector -> new ClosableService(events, "child"));

        child.inject(ClosableService.class);
        parent.inject(ClosableService.class);
        parent.close();

        assertEquals(List.of("child", "parent"), events);
    }

    @Test
    void grandchildrenAreClosedBeforeChildren() {
        List<String> events = new ArrayList<>();
        InjectionToken<ClosableService> grandchildToken = InjectionToken.of("grandchild");
        InjectionToken<ClosableService> childToken = InjectionToken.of("child");
        Injector root = Injector.builder().build();
        Injector child = root.child("child");
        Injector grandchild = child.child("grandchild");
        grandchild.bind(grandchildToken).to(injector -> new ClosableService(events, "grandchild"));
        child.bind(childToken).to(injector -> new ClosableService(events, "child"));

        grandchild.inject(grandchildToken);
        child.inject(childToken);
        root.close();

        assertEquals(List.of("grandchild", "child"), events);
    }

    @Test
    void scopedInstancesAreDisposedWhenTheirScopeCloses() {
        List<String> events = new ArrayList<>();
        Injector root = buildInjector(builder -> builder.bind(ClosableService.class)
                .as(Scope.SCOPED).to(injector -> new ClosableService(events, "scoped")));
        Injector request = root.child("request");

        request.inject(ClosableService.class);
        assertEquals(List.of(), events);

        request.close();

        assertEquals(List.of("scoped"), events);
    }

    @Test
    void transientInstancesAreNeverDisposedByTheInjector() {
        List<String> events = new ArrayList<>();
        Injector injector = buildInjector(builder -> builder.bind(ClosableService.class)
                .as(Scope.TRANSIENT).to(injector2 -> new ClosableService(events, "transient")));

        injector.inject(ClosableService.class);
        injector.inject(ClosableService.class);
        injector.close();

        assertEquals(List.of(), events);
    }

    @Test
    void constantInstancesAreNeverDisposed() {
        List<String> events = new ArrayList<>();
        ClosableService constant = new ClosableService(events, "constant");
        Injector injector = buildInjector(builder -> builder.bind(ClosableService.class).toInstance(constant));

        injector.inject(ClosableService.class);
        injector.close();

        assertEquals(List.of(), events);

        assertDoesNotThrow(constant::close);
        assertEquals(List.of("constant"), events);
    }

    @Test
    void closeIsIdempotent() {
        List<String> events = new ArrayList<>();
        Injector injector = buildInjector(builder ->
                builder.bind(ClosableService.class).to(injector2 -> new ClosableService(events, "singleton")));

        injector.inject(ClosableService.class);
        injector.close();
        injector.close();

        assertEquals(List.of("singleton"), events);
    }

    @Test
    void closingAChildDetachesItFromTheParent() {
        Injector root = Injector.builder().build();
        Injector child = root.child("request");

        child.close();

        assertDoesNotThrow(root::close);
    }

    @Test
    void singletonBoundOnParentSurvivesChildClose() {
        List<String> events = new ArrayList<>();
        Injector root = buildInjector(builder ->
                builder.bind(ClosableService.class).to(injector -> new ClosableService(events, "singleton")));
        Injector child = root.child("request");

        child.inject(ClosableService.class);
        child.close();

        assertEquals(List.of(), events);

        root.close();

        assertEquals(List.of("singleton"), events);
    }

    @Test
    void closeAggregatesDisposeFailuresAndKeepsClosing() {
        List<String> events = new ArrayList<>();
        InjectionToken<ClosableService> good = InjectionToken.of("good");
        InjectionToken<AutoCloseable> bad = InjectionToken.of("bad");
        Injector injector = buildInjector(builder -> {
            builder.bind(good).to(child -> new ClosableService(events, "good"));
            builder.bind(bad).to(child -> (AutoCloseable) () -> {
                throw new IllegalStateException("boom");
            });
        });

        injector.inject(good);
        injector.inject(bad);

        IllegalStateException failure = assertThrows(IllegalStateException.class, injector::close);

        assertEquals(List.of("good"), events);
        assertTrue(failure.getCause().getMessage().contains("boom"));
    }

    @Test
    void singletonCacheSurvivesInParentAfterChildResolution() {
        Injector root = buildInjector(builder ->
                builder.bind(String.class).as(Scope.SINGLETON).to(injector -> new String("value")));
        Injector child = root.child("request");

        child.inject(String.class);
        String cached = root.inject(String.class);

        assertEquals("value", cached);
    }

    @Test
    void failingProviderDoesNotRegisterTheInstanceForDisposal() {
        List<String> events = new ArrayList<>();
        Injector injector = buildInjector(builder -> builder.bind(ClosableService.class).to(injector2 -> {
            throw new IllegalStateException("boom");
        }));

        assertThrows(DejaViewException.class, () -> injector.inject(ClosableService.class));

        injector.close();

        assertEquals(List.of(), events);
    }
}
