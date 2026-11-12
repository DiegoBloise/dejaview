package io.dejaview.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.dejaview.core.DejaViewException;
import org.junit.jupiter.api.Test;

class CircularDependencyTest {

    private static Injector buildInjector(java.util.function.Consumer<InjectorBuilder> configure) {
        InjectorBuilder builder = Injector.builder();
        configure.accept(builder);
        return builder.build();
    }

    @Test
    void directSelfCycleIsDetected() {
        Injector injector = buildInjector(builder ->
                builder.bind(String.class).to(injector2 -> injector2.inject(String.class)));

        DejaViewException failure = assertThrows(DejaViewException.class, () -> injector.inject(String.class));

        assertEquals("circular-dependency", failure.code());
        assertTrue(failure.getMessage().contains(String.class.getName() + " -> " + String.class.getName()));
    }

    @Test
    void indirectCycleIsDetected() {
        InjectionToken<String> a = InjectionToken.of("A");
        InjectionToken<String> b = InjectionToken.of("B");
        Injector injector = buildInjector(builder -> {
            builder.bind(a).to(injector2 -> injector2.inject(b) + "-a");
            builder.bind(b).to(injector2 -> injector2.inject(a) + "-b");
        });

        DejaViewException failure = assertThrows(DejaViewException.class, () -> injector.inject(a));

        assertEquals("circular-dependency", failure.code());
        assertTrue(failure.getMessage().contains("A -> B -> A"));
    }

    @Test
    void longChainCycleIsDetected() {
        InjectionToken<String> one = InjectionToken.of("one");
        InjectionToken<String> two = InjectionToken.of("two");
        InjectionToken<String> three = InjectionToken.of("three");
        InjectionToken<String> four = InjectionToken.of("four");
        Injector injector = buildInjector(builder -> {
            builder.bind(one).to(injector2 -> injector2.inject(two));
            builder.bind(two).to(injector2 -> injector2.inject(three));
            builder.bind(three).to(injector2 -> injector2.inject(four));
            builder.bind(four).to(injector2 -> injector2.inject(two));
        });

        DejaViewException failure = assertThrows(DejaViewException.class, () -> injector.inject(one));

        assertEquals("circular-dependency", failure.code());
        assertTrue(failure.getMessage().contains("two -> three -> four -> two"));
    }

    @Test
    void cycleAcrossInjectorsIsDetected() {
        InjectionToken<String> aToken = InjectionToken.of("cycle-a");
        InjectionToken<String> bToken = InjectionToken.of("cycle-b");
        Injector root = Injector.builder().build();
        Injector child = root.child("request");
        root.bind(aToken).to(injector -> child.inject(bToken));
        child.bind(bToken).to(injector -> root.inject(aToken));

        DejaViewException failure = assertThrows(DejaViewException.class, () -> root.inject(aToken));

        assertEquals("circular-dependency", failure.code());
        assertTrue(failure.getMessage().contains("cycle-a"));
        assertTrue(failure.getMessage().contains("cycle-b"));
    }

    @Test
    void sequentialReuseOfASingletonIsNotACycle() {
        Injector injector = buildInjector(builder -> {
            builder.bind(String.class).as(Scope.SINGLETON).to(injector2 -> "value");
            builder.bind(StringBuilder.class).to(injector2 -> new StringBuilder(injector2.inject(String.class)));
        });

        StringBuilder builderInstance = injector.inject(StringBuilder.class);
        injector.inject(String.class);

        assertEquals("value", builderInstance.toString());
    }

    @Test
    void cycleDetectionFailsWithoutPoisoningTheInjector() {
        Injector injector = buildInjector(builder ->
                builder.bind(String.class).to(injector2 -> injector2.inject(String.class)));

        assertThrows(DejaViewException.class, () -> injector.inject(String.class));
        // A failed resolution must not poison the cache or the resolution path.
        DejaViewException second = assertThrows(DejaViewException.class, () -> injector.inject(String.class));
        assertEquals("circular-dependency", second.code());
    }
}
