package io.dejaview.router;

import io.dejaview.core.Component;
import io.dejaview.core.ComponentView;
import io.dejaview.core.FragmentView;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouterOutletTest {

    private static Component view() {
        return () -> Views.div();
    }

    private static Route home() {
        return Route.path("/").component(() -> view()).build();
    }

    @Test
    void emptyMatchRendersEmptyFragment() {
        // seed "/" matches nothing when only /users is registered
        Route usersOnly = Route.path("/users").component(() -> view()).build();
        Router router = router(usersOnly);
        RouterOutlet outlet = new RouterOutlet(router);
        View view = outlet.render();
        assertTrue(view instanceof FragmentView fragment && fragment.children().isEmpty());
        router.close();
    }

    @Test
    void matchedRouteRendersKeyedComponentView() {
        Route home = home();
        Router router = Router.builder().routes(home).history(new MemoryRouterHistory("/")).build();
        RouterOutlet outlet = new RouterOutlet(router);
        View view = outlet.render();
        assertTrue(view instanceof ComponentView);
        assertEquals(home.id(), ((ComponentView) view).key());
        router.close();
    }

    @Test
    void outletReRendersWithNewKeyAfterNavigation() {
        Route home = home();
        Route users = Route.path("/users/:id").component(() -> view()).build();
        Router router = Router.builder().routes(home, users)
                .history(new MemoryRouterHistory("/"))
                .build();
        RouterOutlet outlet = new RouterOutlet(router);

        ComponentView before = (ComponentView) outlet.render();
        router.navigate("/users/42");
        ComponentView after = (ComponentView) outlet.render();

        assertEquals(home.id(), before.key());
        assertEquals(users.id(), after.key());
        assertNotEquals(before.key(), after.key());
        router.close();
    }

    @Test
    void outletPushesPathParametersAsInputs() {
        Route users = Route.path("/users/:id").component(() -> view()).build();
        Router router = Router.builder().routes(users)
                .history(new MemoryRouterHistory("/users/42"))
                .build();
        RouterOutlet outlet = new RouterOutlet(router);
        ComponentView view = (ComponentView) outlet.render();
        assertEquals("42", view.inputs().get("id"));
        router.close();
    }

    @Test
    void layoutLevelRendersChildActivation() {
        Route child = Route.path("child").component(() -> view()).build();
        Route parent = Route.path("/layout").component(() -> view()).children(child).build();
        Router router = Router.builder().routes(parent)
                .history(new MemoryRouterHistory("/layout/child"))
                .build();

        RouterOutlet level0 = new RouterOutlet(router, 0);
        RouterOutlet level1 = new RouterOutlet(router, 1);
        RouterOutlet level2 = new RouterOutlet(router, 2);

        assertEquals(parent.id(), ((ComponentView) level0.render()).key());
        assertEquals(child.id(), ((ComponentView) level1.render()).key());
        assertTrue(level2.render() instanceof FragmentView fragment && fragment.children().isEmpty());
        router.close();
    }

    @Test
    void levelOutsideChainRendersEmptyFragment() {
        Route home = home();
        Router router = Router.builder().routes(home)
                .history(new MemoryRouterHistory("/"))
                .build();
        RouterOutlet outlet = new RouterOutlet(router, 3);
        assertTrue(outlet.render() instanceof FragmentView fragment && fragment.children().isEmpty());
        router.close();
    }

    @Test
    void navigateToNoMatchKeepsCurrentRoute() {
        Route users = Route.path("/users").component(() -> view()).build();
        Router router = Router.builder().routes(users)
                .history(new MemoryRouterHistory("/users"))
                .build();
        RouterOutlet outlet = new RouterOutlet(router);
        assertTrue(outlet.render() instanceof ComponentView);
        router.navigate("/away"); // no-match, never pushed, current unchanged
        assertTrue(outlet.render() instanceof ComponentView);
        router.close();
    }

    @Test
    void popstateToNoMatchClearsOutlet() {
        Route users = Route.path("/users").component(() -> view()).build();
        Router router = Router.builder().routes(users)
                .history(new MemoryRouterHistory("/")) // seed matches nothing
                .build();
        router.navigate("/users");
        RouterOutlet outlet = new RouterOutlet(router);
        assertTrue(outlet.render() instanceof ComponentView);
        router.back(); // browser-style adoption of "/" (no-match)
        assertTrue(outlet.render() instanceof FragmentView fragment && fragment.children().isEmpty());
        router.close();
    }

    private static Router router(Route... roots) {
        return Router.builder().routes(roots).build();
    }
}
