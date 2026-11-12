package io.dejaview.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.dejaview.core.DejaViewException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InjectorHierarchyTest {

    @Test
    void childResolvesBindingsFromParent() {
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).to(injector -> "value");
        Injector parent = builder.build();
        Injector child = parent.child("request");

        assertEquals("value", child.inject(String.class));
    }

    @Test
    void childBindingShadowsParentBinding() {
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).to(injector -> "base");
        Injector root = builder.build();
        Injector child = root.child("shadowing");
        child.bind(String.class).to(injector -> "shadowed");

        assertEquals("shadowed", child.inject(String.class));
        assertEquals("base", root.inject(String.class));
    }

    @Test
    void shadowedSingletonOnParentIsCachedOnParentNotOnChild() {
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).as(Scope.SINGLETON).to(injector -> new String("base"));
        Injector root = builder.build();
        Injector child = root.child("request");

        assertSame(root.inject(String.class), child.inject(String.class));
    }

    @Test
    void bindOnTheSameInjectorTwiceFailsFast() {
        Injector root = Injector.builder().build();
        root.bind(String.class).to(injector -> "first");

        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> root.bind(String.class).to(injector -> "second"));

        assertEquals("duplicate-binding", failure.code());
    }

    @Test
    void bindingOnAChildDoesNotLeakIntoSiblings() {
        Injector root = Injector.builder().build();
        Injector first = root.child("first");
        Injector second = root.child("second");

        first.bind(String.class).to(injector -> "first-only");

        assertEquals("first-only", first.inject(String.class));
        DejaViewException failure = assertThrows(DejaViewException.class, () -> second.inject(String.class));
        assertEquals("missing-provider", failure.code());
    }

    @Test
    void deepHierarchyResolvesFromNearestAncestor() {
        InjectorBuilder builder = Injector.builder();
        builder.bind(String.class).to(injector -> "root");
        Injector root = builder.build();
        Injector level1 = root.child("level1");
        Injector level2 = level1.child("level2");
        level1.bind(String.class).to(injector -> "level1");

        assertEquals("level1", level2.inject(String.class));
        assertEquals("level1", level1.inject(String.class));
        assertEquals("root", root.inject(String.class));
    }

    @Test
    void childrenCreatedWithoutNameAreNumbered() {
        Injector parent = Injector.builder().name("root").build();

        Injector first = parent.child();
        Injector second = parent.child();

        assertEquals("root.child-1", first.name());
        assertEquals("root.child-2", second.name());
    }

    @Test
    void parentReferencePointsBackToParent() {
        Injector parent = Injector.builder().build();
        Injector child = parent.child("request");
        Injector grandchild = child.child("view");

        assertEquals(parent, child.parent());
        assertEquals(child, grandchild.parent());
        assertNull(parent.parent());
    }

    @Test
    void childCannotBeCreatedAfterClose() {
        Injector parent = Injector.builder().build();
        parent.close();

        DejaViewException failure = assertThrows(DejaViewException.class, () -> parent.child("request"));

        assertEquals("closed-injector", failure.code());
    }

    @Test
    void bindAfterCloseFails() {
        Injector parent = Injector.builder().build();
        parent.close();

        DejaViewException failure = assertThrows(DejaViewException.class,
                () -> parent.bind(String.class).to(injector -> "value"));

        assertEquals("closed-injector", failure.code());
    }

    @Test
    void nameMustNotBeBlank() {
        Injector parent = Injector.builder().build();

        assertThrows(IllegalArgumentException.class, () -> parent.child(""));
        assertThrows(IllegalArgumentException.class, () -> parent.child("   "));
    }

    @Test
    void siblingScopesDoNotShareScopedInstances() {
        InjectorBuilder builder = Injector.builder();
        builder.bind(List.class).as(Scope.SCOPED).to(child -> new ArrayList<>());
        Injector root = builder.build();
        Injector first = root.child("first");
        Injector second = root.child("second");

        List<Object> fromFirst = first.inject(List.class);
        fromFirst.add("marker");

        assertEquals(1, fromFirst.size());
        assertEquals(0, second.inject(List.class).size());
    }
}
