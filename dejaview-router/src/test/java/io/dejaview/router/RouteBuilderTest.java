package io.dejaview.router;

import io.dejaview.core.Component;
import io.dejaview.core.DejaViewException;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteBuilderTest {

    private static Component view() {
        return () -> Views.div();
    }

    @Test
    void buildsSimpleRoute() {
        Route route = Route.path("/users").component(() -> view()).build();
        assertEquals("/users", route.path());
        assertEquals(List.of(), route.children());
        assertTrue(route.componentFactory() != null);
    }

    @Test
    void emptyChildPathIsIndex() {
        Route index = Route.path("").component(() -> view()).build();
        assertEquals("", index.path());
    }

    @Test
    void trailingSlashIsStripped() {
        Route route = Route.path("/users/").component(() -> view()).build();
        assertEquals("/users", route.path());
    }

    @Test
    void rejectsNullPath() {
        assertThrows(NullPointerException.class, () -> Route.path(null));
    }

    @Test
    void rejectsQueryAndFragmentInPath() {
        DejaViewException e = assertThrows(DejaViewException.class,
                () -> Route.path("/users?x=1").component(() -> view()).build());
        assertEquals("invalid-route", e.code());
        e = assertThrows(DejaViewException.class,
                () -> Route.path("/users#frag").component(() -> view()).build());
        assertEquals("invalid-route", e.code());
    }

    @Test
    void rejectsDuplicateSlashes() {
        DejaViewException e = assertThrows(DejaViewException.class,
                () -> Route.path("/users//list").component(() -> view()).build());
        assertEquals("invalid-route", e.code());
    }

    @Test
    void rejectsInvalidParameterName() {
        DejaViewException e = assertThrows(DejaViewException.class,
                () -> Route.path("/users/:1id").component(() -> view()).build());
        assertEquals("invalid-route", e.code());
    }

    @Test
    void rejectsDuplicateParameterInSameRoute() {
        DejaViewException e = assertThrows(DejaViewException.class,
                () -> Route.path("/users/:id/posts/:id").component(() -> view()).build());
        assertEquals("invalid-route", e.code());
    }

    @Test
    void wildcardMustBeFinalSegment() {
        DejaViewException e = assertThrows(DejaViewException.class,
                () -> Route.path("/files/**/name").component(() -> view()).build());
        assertEquals("invalid-route", e.code());
    }

    @Test
    void rejectsSingleStarWildcard() {
        DejaViewException e = assertThrows(DejaViewException.class,
                () -> Route.path("/files/*").component(() -> view()).build());
        assertEquals("invalid-route", e.code());
    }

    @Test
    void wildcardRouteCannotHaveChildren() {
        Route child = Route.path("extra").component(() -> view()).build();
        DejaViewException e = assertThrows(DejaViewException.class,
                () -> Route.path("/files/**").component(() -> view()).children(child).build());
        assertEquals("invalid-route", e.code());
    }

    @Test
    void redirectRouteCannotHaveChildren() {
        Route child = Route.path("extra").component(() -> view()).build();
        DejaViewException e = assertThrows(DejaViewException.class,
                () -> Route.path("/old").redirectTo("/new").children(child).build());
        assertEquals("invalid-route", e.code());
    }

    @Test
    void routeWithNeitherComponentNorRedirectIsRejected() {
        DejaViewException e = assertThrows(DejaViewException.class, () -> Route.path("/users").build());
        assertEquals("invalid-route", e.code());
    }

    @Test
    void routeWithBothComponentAndRedirectIsRejected() {
        DejaViewException e = assertThrows(DejaViewException.class,
                () -> Route.path("/users").component(() -> view()).redirectTo("/people").build());
        assertEquals("invalid-route", e.code());
    }

    @Test
    void childRouteMustNotStartWithSlash() {
        Route child = Route.path("/nested").component(() -> view()).build();
        DejaViewException e = assertThrows(DejaViewException.class,
                () -> Route.path("/parent").component(() -> view()).children(child).build());
        assertEquals("invalid-route", e.code());
    }

    @Test
    void metadataRejectsBlankKeys() {
        assertThrows(IllegalArgumentException.class,
                () -> Route.path("/users").component(() -> view()).metadata("  ", "value"));
    }

    @Test
    void routesHaveStableUniqueIds() {
        Route a = Route.path("/a").component(() -> view()).build();
        Route b = Route.path("/b").component(() -> view()).build();
        org.junit.jupiter.api.Assertions.assertNotEquals(a.id(), b.id());
    }
}
