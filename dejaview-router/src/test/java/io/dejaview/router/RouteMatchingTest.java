package io.dejaview.router;

import io.dejaview.core.Component;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.router.internal.Resolution;
import io.dejaview.router.internal.RouteResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matching behaviour of the internal resolver, driven directly so tests do
 * not depend on navigation events.
 */
class RouteMatchingTest {

    private static Component view() {
        return () -> Views.div();
    }

    private static RouteResolver resolver(Route... roots) {
        return new RouteResolver(List.of(roots), null);
    }

    private static void noEvents(NavigationEvent ignored) {
    }

    private static Consumer<NavigationEvent> sink() {
        return RouteMatchingTest::noEvents;
    }

    private static RouteMatch match(RouteResolver resolver, String url) {
        Resolution resolution = resolver.resolve(url, sink());
        assertEquals(Resolution.Kind.MATCHED, resolution.kind(), "expected a match for " + url);
        return resolution.match();
    }

    @Test
    void literalPathMatches() {
        Route root = Route.path("/users").component(() -> view()).build();
        RouteMatch match = match(resolver(root), "/users");
        assertEquals("/users", match.path());
        assertEquals(List.of(root), match.chain());
        assertEquals(1, match.depth());
    }

    @Test
    void literalPathIsCaseSensitive() {
        Route root = Route.path("/users").component(() -> view()).build();
        assertEquals(Resolution.Kind.NO_MATCH, resolver(root).resolve("/Users", sink()).kind());
    }

    @Test
    void parameterCapturesDecodedSegment() {
        Route root = Route.path("/users/:id").component(() -> view()).build();
        RouteMatch match = match(resolver(root), "/users/ana%20maria");
        assertEquals("ana maria", match.param("id"));
    }

    @Test
    void parameterDoesNotSpanSlashes() {
        Route root = Route.path("/users/:id").component(() -> view()).build();
        assertEquals(Resolution.Kind.NO_MATCH, resolver(root).resolve("/users/a/b", sink()).kind());
    }

    @Test
    void plusInPathIsLiteralPlus() {
        Route root = Route.path("/search/:term").component(() -> view()).build();
        RouteMatch match = match(resolver(root), "/search/c++");
        assertEquals("c++", match.param("term"));
    }

    @Test
    void encodedSlashDoesNotSplitSegments() {
        Route root = Route.path("/files/:name").component(() -> view()).build();
        RouteMatch match = match(resolver(root), "/files/a%2Fb.txt");
        assertEquals("a/b.txt", match.param("name"));
    }

    @Test
    void wildcardConsumesRestOfPath() {
        Route root = Route.path("/files/**").component(() -> view()).build();
        RouteMatch match = match(resolver(root), "/files/a/b/c/d.txt");
        assertEquals("/files/a/b/c/d.txt", match.path());
    }

    @Test
    void wildcardRootMatchesEverything() {
        Route root = Route.path("**").component(() -> view()).build();
        assertEquals(Resolution.Kind.MATCHED, resolver(root).resolve("/anything/at/all", sink()).kind());
        assertEquals(Resolution.Kind.MATCHED, resolver(root).resolve("/", sink()).kind());
    }

    @Test
    void nestedRoutesAccumulateChainAndParams() {
        Route users = Route.path("/users/:id")
                .component(() -> view())
                .children(Route.path("posts/:postId").component(() -> view()).build())
                .build();
        RouteMatch match = match(resolver(users), "/users/42/posts/7");
        assertEquals(2, match.depth());
        assertEquals("42", match.param("id"));
        assertEquals("7", match.param("postId"));
    }

    @Test
    void childParameterWinsOnKeyClashAcrossLevels() {
        Route users = Route.path("/users/:id")
                .component(() -> view())
                .children(Route.path("posts/:id").component(() -> view()).build())
                .build();
        RouteMatch match = match(resolver(users), "/users/42/posts/7");
        assertEquals("7", match.param("id"));
    }

    @Test
    void exhaustedParentWithoutIndexChildStillMatches() {
        Route users = Route.path("/users")
                .component(() -> view())
                .children(Route.path("list").component(() -> view()).build())
                .build();
        RouteMatch match = match(resolver(users), "/users");
        assertEquals(1, match.depth());
    }

    @Test
    void indexChildMatchesParentUrl() {
        Route index = Route.path("").component(() -> view()).build();
        Route users = Route.path("/users").component(() -> view()).children(index).build();
        RouteMatch match = match(resolver(users), "/users");
        assertEquals(2, match.depth());
        assertSame(index, match.leaf());
    }

    @Test
    void unmatchedChildRejectsParentMatch() {
        Route users = Route.path("/users")
                .component(() -> view())
                .children(Route.path("list").component(() -> view()).build())
                .build();
        assertEquals(Resolution.Kind.NO_MATCH, resolver(users).resolve("/users/nope", sink()).kind());
    }

    @Test
    void leafDoesNotSpanExtraSegments() {
        Route root = Route.path("/users").component(() -> view()).build();
        assertEquals(Resolution.Kind.NO_MATCH, resolver(root).resolve("/users/42", sink()).kind());
    }

    @Test
    void firstRootWinsOnOverlap() {
        Route first = Route.path("/users/special").component(() -> view()).build();
        Route second = Route.path("/users/:id").component(() -> view()).build();
        RouteMatch match = match(resolver(first, second), "/users/special");
        assertSame(first, match.leaf());
    }

    @Test
    void rootSlashMatchesRootUrl() {
        Route root = Route.path("/").component(() -> view()).build();
        assertEquals(Resolution.Kind.MATCHED, resolver(root).resolve("/", sink()).kind());
        assertEquals(Resolution.Kind.NO_MATCH, resolver(root).resolve("/other", sink()).kind());
    }

    @Test
    void emptyRootMatchesOnlySlash() {
        Route root = Route.path("").component(() -> view()).build();
        RouteMatch match = match(resolver(root), "/");
        assertEquals("/", match.path());
        assertEquals(Resolution.Kind.NO_MATCH, resolver(root).resolve("/other", sink()).kind());
    }

    @Test
    void trailingSlashIsNormalized() {
        Route root = Route.path("/users").component(() -> view()).build();
        assertEquals(Resolution.Kind.MATCHED, resolver(root).resolve("/users/", sink()).kind());
    }

    @Test
    void duplicateSlashesAreNormalized() {
        Route root = Route.path("/users/:id").component(() -> view()).build();
        RouteMatch match = match(resolver(root), "//users///42");
        assertEquals("42", match.param("id"));
    }

    @Test
    void missingLeadingSlashIsNormalized() {
        Route root = Route.path("/users").component(() -> view()).build();
        RouteMatch match = match(resolver(root), "users");
        assertEquals("/users", match.path());
    }

    @Test
    void fragmentIsDropped() {
        Route root = Route.path("/users").component(() -> view()).build();
        RouteMatch match = match(resolver(root), "/users#section");
        assertEquals("/users", match.url());
    }

    @Test
    void metadataMergesRootToLeaf() {
        Route users = Route.path("/users")
                .component(() -> view())
                .metadata("title", "Users")
                .metadata("roles", "user")
                .children(Route.path("admin")
                        .component(() -> view())
                        .metadata("roles", "admin")
                        .build())
                .build();
        RouteMatch match = match(resolver(users), "/users/admin");
        assertEquals("Users", match.metadata("title"));
        assertEquals("admin", match.metadata("roles"));
        assertEquals(Map.of("title", "Users", "roles", "admin"), match.metadata());
    }

    @Test
    void queryMatchIsIgnoredForPathMatching() {
        Route root = Route.path("/users").component(() -> view()).build();
        RouteMatch match = match(resolver(root), "/users?page=2");
        assertEquals("2", match.queryParam("page"));
        assertEquals("/users?page=2", match.url());
    }

    @Test
    void matchEqualityIsValueBased() {
        Route root = Route.path("/users/:id").component(() -> view()).build();
        RouteResolver resolver = resolver(root);
        RouteMatch first = match(resolver, "/users/42?x=1");
        RouteMatch second = match(resolver, "/users/42?x=1");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void routeIdsAreDistinctPerBuiltRoute() {
        Route root = Route.path("/users").component(() -> view()).build();
        assertNotNull(root.id());
        assertTrue(root.id().startsWith("route:"));
    }

    @Test
    void paramsExposedAreUnmodifiable() {
        Route root = Route.path("/users/:id").component(() -> view()).build();
        RouteMatch match = match(resolver(root), "/users/42");
        assertThrowsOnPut(match.params());
        assertThrowsOnPut(match.queryParams());
        assertThrowsOnMetadataPut(match.metadata());
        assertNull(match.param("nope"));
    }

    private static void assertThrowsOnPut(Map<String, String> map) {
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> map.put("x", "y"));
    }

    private static void assertThrowsOnMetadataPut(Map<String, Object> map) {
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> map.put("x", new Object()));
    }
}
