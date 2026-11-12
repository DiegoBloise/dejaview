package io.dejaview.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ProviderScopeTest {

    @Test
    void singletonIsCreatedOnceAndReused() {
        AtomicInteger calls = new AtomicInteger();
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).as(Scope.SINGLETON).to(injector -> "instance-" + calls.incrementAndGet());
        Injector injector = builder.build();

        String first = injector.inject(String.class);
        String second = injector.inject(String.class);

        assertSame(first, second);
        assertEquals(1, calls.get());
    }

    @Test
    void singletonIsTheDefaultScope() {
        AtomicInteger calls = new AtomicInteger();
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).to(injector -> "instance-" + calls.incrementAndGet());
        Injector injector = builder.build();

        assertSame(injector.inject(String.class), injector.inject(String.class));
        assertEquals(1, calls.get());
    }

    @Test
    void transientCreatesIndependentInstances() {
        InjectorBuilder builder = Injector.builder();
        builder.bind(StringBuilder.class).as(Scope.TRANSIENT).to(injector -> new StringBuilder());
        Injector injector = builder.build();

        assertNotSame(injector.inject(StringBuilder.class), injector.inject(StringBuilder.class));
    }

    @Test
    void transientIsNeverCached() {
        AtomicInteger calls = new AtomicInteger();
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).as(Scope.TRANSIENT).to(injector -> "instance-" + calls.incrementAndGet());
        Injector injector = builder.build();

        injector.inject(String.class);
        injector.inject(String.class);

        assertEquals(2, calls.get());
    }

    @Test
    void scopedIsCachedPerResolvingInjector() {
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).as(Scope.SCOPED).to(injector -> new String("value"));
        Injector parent = builder.build();
        Injector first = parent.child("first");
        Injector second = parent.child("second");

        assertSame(first.inject(String.class), first.inject(String.class));
        assertSame(second.inject(String.class), second.inject(String.class));
        assertNotSame(first.inject(String.class), second.inject(String.class));
    }

    @Test
    void scopedInjectedThroughRootCachesInRoot() {
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).as(Scope.SCOPED).to(injector -> new String("value"));
        Injector injector = builder.build();

        assertSame(injector.inject(String.class), injector.inject(String.class));
    }

    @Test
    void scopedInstancesAreIndependentPerChildEvenForTheSameBinding() {
        InjectorBuilder builder = Injector.builder();
        builder.bind(StringBuilder.class).as(Scope.SCOPED).to(injector -> new StringBuilder());
        Injector root = builder.build();
        Injector childA = root.child("a");
        Injector childB = root.child("b");

        StringBuilder fromA = childA.inject(StringBuilder.class);
        StringBuilder fromB = childB.inject(StringBuilder.class);

        assertNotSame(fromA, fromB);
    }

    @Test
    void singletonBoundOnRootIsSharedByChildren() {
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).as(Scope.SINGLETON).to(injector -> new String("value"));
        Injector root = builder.build();
        Injector child = root.child("request");

        assertSame(root.inject(String.class), child.inject(String.class));
    }
}
