package io.dejaview.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.dejaview.core.DejaViewException;
import org.junit.jupiter.api.Test;

class InjectorResolutionTest {

    @Test
    void resolvesDependencyFromProvider() {
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).to(injector -> "database-url");
        Injector injector = builder.build();

        assertEquals("database-url", injector.inject(String.class));
    }

    @Test
    void classTokenAndExplicitTokenResolveToTheSameInstance() {
        InjectionToken<String> explicit = InjectionToken.of(String.class);
        InjectorBuilder builder = Injector.builder();
        builder.bind(explicit).to(injector -> "value");
        Injector injector = builder.build();

        assertSame(injector.inject(String.class), injector.inject(explicit));
    }

    @Test
    void missingProviderNamesTheTokenAndInjector() {
        Injector injector = Injector.builder().name("app").build();

        DejaViewException failure = assertThrows(DejaViewException.class, () -> injector.inject(String.class));

        assertEquals("missing-provider", failure.code());
        assertTrue(failure.getMessage().contains(String.class.getName()));
        assertTrue(failure.getMessage().contains("app"));
    }

    @Test
    void missingProviderDuringResolutionReportsTheInFlightChain() {
        InjectionToken<String> inner = InjectionToken.of("inner");
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).to(injector -> injector.inject(inner));
        Injector injector = builder.build();

        DejaViewException failure = assertThrows(DejaViewException.class, () -> injector.inject(String.class));

        assertEquals("missing-provider", failure.code());
        assertTrue(failure.getMessage().contains("while resolving"));
        assertTrue(failure.getMessage().contains(String.class.getName()));
    }

    @Test
    void hasReportsBindingsWithoutInstantiating() {
        boolean[] created = {false};
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).to(injector -> {
            created[0] = true;
            return "value";
        });
        Injector injector = builder.build();

        assertTrue(injector.has(String.class));
        assertTrue(injector.has(InjectionToken.of(String.class)));
        assertFalse(injector.has(Integer.class));
        assertFalse(created[0]);
    }

    @Test
    void hasWalksUpTheHierarchy() {
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).to(injector -> "value");
        Injector parent = builder.build();
        Injector child = parent.child("request");

        assertTrue(child.has(String.class));
        assertFalse(child.has(Integer.class));
    }

    @Test
    void providerReturningNullIsRejected() {
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).to(injector -> null);
        Injector injector = builder.build();

        DejaViewException failure = assertThrows(DejaViewException.class, () -> injector.inject(String.class));

        assertEquals("provider-error", failure.code());
        assertTrue(failure.getMessage().contains(String.class.getName()));
    }

    @Test
    void providerFailureIsWrappedWithCause() {
        IllegalStateException boom = new IllegalStateException("boom");
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).to(injector -> {
            throw boom;
        });
        Injector injector = builder.build();

        DejaViewException failure = assertThrows(DejaViewException.class, () -> injector.inject(String.class));

        assertEquals("provider-error", failure.code());
        assertSame(boom, failure.getCause());
        assertTrue(failure.getMessage().contains("boom"));
    }

    @Test
    void providerThrowingDejaViewExceptionKeepsTheOriginalCode() {
        DejaViewException custom = new DejaViewException("custom-code", "custom failure", null);
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).to(injector -> {
            throw custom;
        });
        Injector injector = builder.build();

        DejaViewException failure = assertThrows(DejaViewException.class, () -> injector.inject(String.class));

        assertSame(custom, failure);
        assertEquals("custom-code", failure.code());
    }

    @Test
    void duplicateBindingInOneBuilderFailsFast() {
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).to(injector -> "first");

        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> builder.bind(String.class).to(injector -> "second"));

        assertEquals("duplicate-binding", failure.code());
        assertTrue(failure.getMessage().contains(String.class.getName()));
    }

    @Test
    void duplicateBindingWithExplicitTokenFailsFast() {
        InjectionToken<String> token = InjectionToken.of("service");
        InjectorBuilder builder = Injector.builder();
        builder.bind(token).to(injector -> "first");

        assertThrows(DejaViewException.class, () -> builder.bind(token).to(injector -> "second"));
    }

    @Test
    void isolatedCopiesMeanBuilderReuseDoesNotLeak() {
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).to(injector -> new String("value"));
        Injector first = builder.build();
        Injector second = builder.build();

        assertSame(first.inject(String.class), first.inject(String.class));
        assertSame(second.inject(String.class), second.inject(String.class));
        org.junit.jupiter.api.Assertions.assertNotSame(first.inject(String.class), second.inject(String.class));
    }

    @Test
    void injectAfterCloseFails() {
        Injector injector = Injector.builder().build();
        injector.close();

        DejaViewException failure = assertThrows(DejaViewException.class, () -> injector.inject(String.class));

        assertEquals("closed-injector", failure.code());
    }

    @Test
    void hasAfterCloseFails() {
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).to(injector -> "value");
        Injector injector = builder.build();
        injector.close();

        assertThrows(DejaViewException.class, () -> injector.has(String.class));
    }

    @Test
    void injectingThroughAClosedChildFails() {
        Injector parent = Injector.builder().build();
        Injector child = parent.child("request");
        child.close();

        DejaViewException failure = assertThrows(DejaViewException.class, () -> child.inject(String.class));

        assertEquals("closed-injector", failure.code());
    }

    @Test
    void constantInstancesAreReturnedAsIs() {
        Object constant = new Object();
        InjectorBuilder builder = Injector.builder();
        builder.bind(Object.class).toInstance(constant);
        Injector injector = builder.build();

        assertSame(constant, injector.inject(Object.class));
        assertSame(constant, injector.inject(Object.class));
    }
}
