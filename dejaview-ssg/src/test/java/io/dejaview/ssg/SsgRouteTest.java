package io.dejaview.ssg;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsgRouteTest {

    @Test
    void aStaticPageExpandsToItsOwnPath() {
        SsgRoute route = SsgRoute.page("/about", "en", meta -> null);

        assertEquals("/about", route.pattern());
        assertEquals("en", route.lang());
        assertEquals(List.of(Map.of()), route.parameterSets());
        assertEquals(List.of(new SsgRoute.Expanded("/about", Map.of())), route.expand());
    }

    @Test
    void aParametrizedRouteExpandsEveryParameterSetInOrder() {
        SsgRoute route = SsgRoute.parametrized(
                "/products/{slug}", "en",
                List.of(Map.of("slug", "keyboard"), Map.of("slug", "mouse")),
                ctx -> null);

        List<String> paths = route.expand().stream().map(SsgRoute.Expanded::path).toList();

        assertEquals(List.of("/products/keyboard", "/products/mouse"), paths);
        assertEquals(Map.of("slug", "mouse"), route.expand().get(1).params());
    }

    @Test
    void aRouteMustStartWithASlash() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> SsgRoute.page("about", "en", meta -> null));

        assertTrue(error.getMessage().contains("must start with '/'"));
    }

    @Test
    void traversalSegmentsAreRejectedAtDeclaration() {
        assertThrows(IllegalArgumentException.class,
                () -> SsgRoute.page("/../secret", "en", meta -> null));
        assertThrows(IllegalArgumentException.class,
                () -> SsgRoute.page("/a//b", "en", meta -> null));
        assertThrows(IllegalArgumentException.class,
                () -> SsgRoute.page("/a b", "en", meta -> null));
        assertThrows(IllegalArgumentException.class,
                () -> SsgRoute.page("/a?q=1", "en", meta -> null));
    }

    @Test
    void parametrizedRequiresAtLeastOnePlaceholder() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> SsgRoute.parametrized("/about", "en", List.of(Map.of()), ctx -> null));

        assertTrue(error.getMessage().contains("has no {name} placeholder"));
    }

    @Test
    void parametrizedRequiresAtLeastOneParameterSet() {
        assertThrows(IllegalArgumentException.class,
                () -> SsgRoute.parametrized("/products/{slug}", "en", List.of(), ctx -> null));
    }

    @Test
    void everyParameterSetMustMatchThePattern() {
        assertThrows(IllegalArgumentException.class,
                () -> SsgRoute.parametrized("/products/{slug}", "en",
                        List.of(Map.of()), ctx -> null));
        assertThrows(IllegalArgumentException.class,
                () -> SsgRoute.parametrized("/products/{slug}", "en",
                        List.of(Map.of("slug", "mouse", "extra", "x")), ctx -> null));
    }

    @Test
    void parameterValuesMustBeASingleSafeSegment() {
        assertThrows(IllegalArgumentException.class,
                () -> SsgRoute.parametrized("/products/{slug}", "en",
                        List.of(Map.of("slug", "../secret")), ctx -> null));
        assertThrows(IllegalArgumentException.class,
                () -> SsgRoute.parametrized("/products/{slug}", "en",
                        List.of(Map.of("slug", "a/b")), ctx -> null));
        assertThrows(IllegalArgumentException.class,
                () -> SsgRoute.parametrized("/products/{slug}", "en",
                        List.of(Map.of("slug", "two words")), ctx -> null));
        assertThrows(IllegalArgumentException.class,
                () -> SsgRoute.parametrized("/products/{slug}", "en",
                        List.of(Map.of("slug", "%2e%2e")), ctx -> null));
    }

    @Test
    void aRepeatedPlaceholderIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> SsgRoute.parametrized("/{slug}/{slug}", "en",
                        List.of(Map.of("slug", "x")), ctx -> null));

        assertTrue(error.getMessage().contains("repeats the parameter"));
    }

    @Test
    void parametersAreOnlyAllowedAsWholeSegments() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> SsgRoute.parametrized("/products/prefix-{slug}", "en",
                        List.of(Map.of("slug", "x")), ctx -> null));

        assertTrue(error.getMessage().contains("whole-segment {name}"));
    }

    @Test
    void aBlankLanguageIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> SsgRoute.page("/", " ", meta -> null));
    }

    @Test
    void aRouteCannotReturnANullView() {
        SsgRoute route = SsgRoute.page("/", "en", meta -> null);

        assertThrows(NullPointerException.class,
                () -> route.render(new SsgPageContext("/", "en", Map.of(), new io.dejaview.ssr.PageMeta())));
    }
}
