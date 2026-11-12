package io.dejaview.router;

import io.dejaview.core.Component;
import io.dejaview.core.DejaViewException;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.router.internal.Resolution;
import io.dejaview.router.internal.RouteResolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolutionTest {

    private static Component view() {
        return () -> Views.div();
    }

    private static final Consumer<NavigationEvent> SINK = event -> { };

    private static Resolution match(RouteResolver resolver, String url) {
        return resolver.resolve(url, SINK);
    }

    private static Resolution match(RouteResolver resolver, String url, List<NavigationEvent> events) {
        return resolver.resolve(url, events::add);
    }

    @Test
    void guardsRunRootFirst() {
        List<String> order = new ArrayList<>();
        Route child = Route.path("child")
                .component(() -> view())
                .guard(context -> {
                    order.add("child");
                    return NavigationDecision.allow();
                })
                .build();
        Route root = Route.path("/root")
                .component(() -> view())
                .guard(context -> {
                    order.add("root");
                    return NavigationDecision.allow();
                })
                .children(child)
                .build();
        Resolution resolution = match(new RouteResolver(List.of(root), null), "/root/child");
        assertEquals(Resolution.Kind.MATCHED, resolution.kind());
        assertEquals(List.of("root", "child"), order);
    }

    @Test
    void guardSeesOnlyPrefixAccumulatedParams() {
        List<String> seenAtRoot = new ArrayList<>();
        List<String> seenAtChild = new ArrayList<>();
        Route child = Route.path("child/:childParam")
                .component(() -> view())
                .guard(context -> {
                    seenAtChild.add(context.param("rootParam") + "/" + context.param("childParam"));
                    return NavigationDecision.allow();
                })
                .build();
        Route root = Route.path("/root/:rootParam")
                .component(() -> view())
                .guard(context -> {
                    seenAtRoot.add(context.param("rootParam") + "/" + context.param("childParam"));
                    return NavigationDecision.allow();
                })
                .children(child)
                .build();
        match(new RouteResolver(List.of(root), null), "/root/a/child/b");
        assertEquals(List.of("a/null"), seenAtRoot);
        assertEquals(List.of("a/b"), seenAtChild);
    }

    @Test
    void denyCancelsNavigation() {
        Route root = Route.path("/admin")
                .component(() -> view())
                .guard(NavigationGuards.deny("forbidden"))
                .build();
        Resolution resolution = match(new RouteResolver(List.of(root), null), "/admin");
        assertEquals(Resolution.Kind.CANCELLED, resolution.kind());
        assertEquals("forbidden", resolution.reason());
    }

    @Test
    void firstDenyWinsAndLaterGuardsDoNotRun() {
        List<String> ran = new ArrayList<>();
        Route root = Route.path("/admin")
                .component(() -> view())
                .guard(context -> NavigationDecision.deny("no"))
                .guard(context -> {
                    ran.add("second");
                    return NavigationDecision.allow();
                })
                .build();
        match(new RouteResolver(List.of(root), null), "/admin");
        assertTrue(ran.isEmpty());
    }

    @Test
    void nullDecisionFromGuardFailsWithGuardError() {
        Route root = Route.path("/admin")
                .component(() -> view())
                .guard(context -> null)
                .build();
        DejaViewException e = assertThrows(DejaViewException.class,
                () -> match(new RouteResolver(List.of(root), null), "/admin"));
        assertEquals("guard-error", e.code());
    }

    @Test
    void staticRedirectReresolvesTarget() {
        Route target = Route.path("/new").component(() -> view()).build();
        Route old = Route.path("/old").redirectTo("/new").build();
        Resolution resolution = match(new RouteResolver(List.of(old, target), null), "/old");
        assertEquals(Resolution.Kind.MATCHED, resolution.kind());
        assertEquals("/new", resolution.match().path());
    }

    @Test
    void redirectEmitsRedirectedEvents() {
        Route target = Route.path("/new").component(() -> view()).build();
        Route old = Route.path("/old").redirectTo("/new").build();
        List<NavigationEvent> events = new ArrayList<>();
        match(new RouteResolver(List.of(old, target), null), "/old", events);
        assertEquals(List.of(new NavigationEvent.Redirected("/old", "/new")), events);
    }

    @Test
    void redirectFunctionReceivesContext() {
        Route target = Route.path("/users/:id").component(() -> view()).build();
        Route old = Route.path("/people/:id")
                .redirectTo(context -> "/users/" + context.param("id"))
                .build();
        Resolution resolution = match(new RouteResolver(List.of(old, target), null), "/people/42");
        assertEquals(Resolution.Kind.MATCHED, resolution.kind());
        assertEquals("42", resolution.match().param("id"));
    }

    @Test
    void guardsOnRedirectingRouteAreNotEvaluated() {
        Route target = Route.path("/new").component(() -> view()).build();
        Route old = Route.path("/old")
                .redirectTo("/new")
                .guard(NavigationGuards.deny("should-not-run"))
                .build();
        Resolution resolution = match(new RouteResolver(List.of(old, target), null), "/old");
        assertEquals(Resolution.Kind.MATCHED, resolution.kind());
    }

    @Test
    void redirectLoopFailsWithRedirectLoopCode() {
        Route a = Route.path("/a").redirectTo("/b").build();
        Route b = Route.path("/b").redirectTo("/a").build();
        DejaViewException e = assertThrows(DejaViewException.class,
                () -> match(new RouteResolver(List.of(a, b), null), "/a"));
        assertEquals("redirect-loop", e.code());
    }

    @Test
    void guardRedirectLoopFailsWithRedirectLoopCode() {
        Route a = Route.path("/a").component(() -> view())
                .guard(context -> NavigationDecision.redirect("/b"))
                .build();
        Route b = Route.path("/b").component(() -> view())
                .guard(context -> NavigationDecision.redirect("/a"))
                .build();
        DejaViewException e = assertThrows(DejaViewException.class,
                () -> match(new RouteResolver(List.of(a, b), null), "/a"));
        assertEquals("redirect-loop", e.code());
    }

    @Test
    void guardRedirectReresolvesAndDiscardsRemainingChain() {
        Route target = Route.path("/target").component(() -> view()).build();
        Route leaf = Route.path("leaf").component(() -> view()).build();
        Route root = Route.path("/root")
                .component(() -> view())
                .guard(context -> NavigationDecision.redirect("/target"))
                .children(leaf)
                .build();
        Resolution resolution = match(new RouteResolver(List.of(root, target), null), "/root/leaf");
        assertEquals(Resolution.Kind.MATCHED, resolution.kind());
        assertEquals("/target", resolution.match().path());
    }

    @Test
    void notFoundRouteActivatesWhenNothingMatches() {
        Route missing = Route.path("**").component(() -> view()).build();
        Route found = Route.path("/found").component(() -> view()).build();
        Resolution resolution = match(new RouteResolver(List.of(found), missing), "/nope");
        assertEquals(Resolution.Kind.MATCHED, resolution.kind());
        assertEquals("/nope", resolution.match().path());
    }

    @Test
    void notFoundRouteGuardsStillApply() {
        Route missing = Route.path("**")
                .component(() -> view())
                .guard(NavigationGuards.deny("hidden"))
                .build();
        Resolution resolution = match(new RouteResolver(List.of(), missing), "/nope");
        assertEquals(Resolution.Kind.CANCELLED, resolution.kind());
        assertEquals("hidden", resolution.reason());
    }

    @Test
    void noMatchWhenNotFoundDoesNotMatch() {
        Route missing = Route.path("/expected").component(() -> view()).build();
        Resolution resolution = match(new RouteResolver(List.of(), missing), "/nope");
        assertEquals(Resolution.Kind.NO_MATCH, resolution.kind());
    }

    private static final class NavigationGuards {
        static RouteGuard deny(String reason) {
            return context -> NavigationDecision.deny(reason);
        }
    }
}
