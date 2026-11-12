package io.dejaview.router;

import io.dejaview.core.Component;
import io.dejaview.core.DejaViewException;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationTest {

    private static Component view() {
        return () -> Views.div();
    }

    private static Route users() {
        return Route.path("/users/:id").component(() -> view()).build();
    }

    private static Router router(Route... roots) {
        return Router.builder().routes(roots).build();
    }

    @Test
    void navigateResolvesAndPushes() {
        Router router = router(users());
        assertTrue(router.navigate("/users/42"));
        assertEquals("42", router.currentMatch().map(match -> match.param("id")).orElseThrow());
        assertEquals("/users/42", router.history().current());
        router.close();
    }

    @Test
    void navigateEmitsStartedThenEnded() {
        Router router = router(users());
        List<NavigationEvent> events = new ArrayList<>();
        try (EventSubscription ignored = router.events(events::add)) {
            router.navigate("/users/42");
        }
        assertEquals(2, events.size());
        assertEquals(new NavigationEvent.Started("/users/42"), events.get(0));
        assertEquals("Ended", events.get(1).getClass().getSimpleName());
        assertEquals("/users/42", ((NavigationEvent.Ended) events.get(1)).match().url());
        router.close();
    }

    @Test
    void navigateUnmatchedReturnsFalseAndKeepsState() {
        Router router = router(users());
        router.navigate("/users/42");
        assertFalse(router.navigate("/elsewhere"));
        assertEquals("/users/42", router.currentMatch().map(RouteMatch::url).orElseThrow());
        assertEquals("/users/42", router.history().current()); // no push happened
        router.close();
    }

    @Test
    void guardDenialEmitsCancelledAndKeepsHistoryUnchanged() {
        Route protectedRoute = Route.path("/admin")
                .component(() -> view())
                .guard(NavigationGuard.deny("nope"))
                .build();
        Router router = router(protectedRoute);
        List<NavigationEvent> events = new ArrayList<>();
        try (EventSubscription ignored = router.events(events::add)) {
            assertFalse(router.navigate("/admin"));
        }
        assertEquals(List.of(
                new NavigationEvent.Started("/admin"),
                new NavigationEvent.Cancelled("/admin", "nope")), events);
        assertEquals("/", router.history().current()); // seeded default, never pushed
        router.close();
    }

    @Test
    void replaceRewritesCurrentEntry() {
        Router router = router(users());
        router.navigate("/users/1");
        assertTrue(router.replace("/users/2"));
        assertEquals("/users/2", router.history().current());
        router.close();
    }

    @Test
    void backForwardAdoptsHistoryEntries() {
        Router router = router(users());
        router.navigate("/users/1");
        router.navigate("/users/2");

        router.back();
        assertEquals("1", router.currentMatch().map(match -> match.param("id")).orElseThrow());
        router.back();
        assertTrue(router.currentMatch().isEmpty()); // seed "/" matches nothing

        router.forward();
        assertEquals("1", router.currentMatch().map(match -> match.param("id")).orElseThrow());
        router.close();
    }

    @Test
    void eventsUnregisterStopsDelivery() {
        Router router = router(users());
        List<NavigationEvent> events = new ArrayList<>();
        EventSubscription handle = router.events(events::add);
        router.navigate("/users/1");
        handle.close();
        router.navigate("/users/2");
        long endedCount = events.stream().filter(NavigationEvent.Ended.class::isInstance).count();
        assertEquals(1, endedCount);
        router.close();
    }

    @Test
    void closeMakesRouterUnusable() {
        Router router = router(users());
        router.close();
        assertThrows(DejaViewException.class, () -> router.navigate("/users/1"));
        router.close(); // idempotent
    }

    @Test
    void initialNoMatchYieldsEmptyMatch() {
        Router router = router(users());
        assertTrue(router.currentMatch().isEmpty()); // seed "/" matches nothing
        router.close();
    }

    @Test
    void initialMatchResolvesEagerly() {
        Router router = Router.builder()
                .routes(users())
                .history(new MemoryRouterHistory("/users/9"))
                .build();
        assertEquals(Optional.of("9"), router.currentMatch().map(match -> match.param("id")));
        router.close();
    }

    @Test
    void navigatingWithRedirectEmitsRedirectAndEnded() {
        Route dest = users();
        Route old = Route.path("/old/:id").redirectTo(ctx -> "/users/" + ctx.param("id")).build();
        Router router = router(old, dest);
        List<NavigationEvent> events = new ArrayList<>();
        try (EventSubscription ignored = router.events(events::add)) {
            assertTrue(router.navigate("/old/7"));
        }
        assertEquals(new NavigationEvent.Started("/old/7"), events.get(0));
        assertEquals(new NavigationEvent.Redirected("/old/7", "/users/7"), events.get(1));
        assertTrue(events.get(2) instanceof NavigationEvent.Ended ended
                && "/users/7".equals(ended.url()));
        assertEquals("/users/7", router.history().current());
        router.close();
    }

    private static final class NavigationGuard {
        static RouteGuard deny(String reason) {
            return context -> NavigationDecision.deny(reason);
        }
    }
}
