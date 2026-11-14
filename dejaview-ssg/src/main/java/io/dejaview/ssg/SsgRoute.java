package io.dejaview.ssg;

import io.dejaview.core.View;
import io.dejaview.ssr.PageMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * One static route of a generated site.
 *
 * <p>Routes are declared explicitly in Java — the framework has no reflection,
 * so there is no annotation scanning and no file-system discovery. A static
 * page is created with {@link #page}; a dynamic route is created with
 * {@link #parametrized}, which expands one {@code {name}} placeholder per
 * path segment from the parameter sets the application supplies at build
 * time.</p>
 *
 * <pre>{@code
 * SsgRoute.page("/", "en", meta -> new HomePage(meta));
 * SsgRoute.parametrized("/products/{slug}", "en",
 *         List.of(Map.of("slug", "keyboard"), Map.of("slug", "mouse")),
 *         ctx -> new ProductPage(ctx.meta(), ctx.param("slug")));
 * }</pre>
 *
 * <p>Paths and parameter values are validated eagerly: a route that could
 * write outside the output directory fails when it is declared, not halfway
 * through a build.</p>
 */
public final class SsgRoute {

    private final String pattern;
    private final String lang;
    private final List<Map<String, String>> parameterSets;
    private final Function<SsgPageContext, View> view;

    private SsgRoute(String pattern, String lang, List<Map<String, String>> parameterSets,
                     Function<SsgPageContext, View> view) {
        this.pattern = pattern;
        this.lang = requireLang(lang);
        this.parameterSets = List.copyOf(parameterSets);
        this.view = Objects.requireNonNull(view, "view");
    }

    /**
     * Declares a static page.
     *
     * @param path the URL path, for example {@code /} or {@code /about}
     * @param lang the document language tag, for example {@code en}
     * @param view receives the page metadata, which it may write while rendering
     */
    public static SsgRoute page(String path, String lang, Function<PageMeta, View> view) {
        Objects.requireNonNull(view, "view");
        String validated = SsgPaths.requireRoutePath(path, "route path");
        return new SsgRoute(validated, lang, List.of(Map.of()), ctx -> view.apply(ctx.meta()));
    }

    /**
     * Declares a dynamic route, expanded once per supplied parameter set.
     *
     * @param pattern       the path pattern, with whole-segment {@code {name}}
     *                      placeholders, for example {@code /products/{slug}}
     * @param lang          the document language tag
     * @param parameterSets one map per page; every map must contain exactly the
     *                      placeholders of the pattern
     * @param view          receives the resolved path, parameters and metadata
     */
    public static SsgRoute parametrized(
            String pattern, String lang, List<Map<String, String>> parameterSets,
            Function<SsgPageContext, View> view) {
        List<String> names = SsgPaths.placeholders(pattern, "route pattern");
        if (names.isEmpty()) {
            throw new IllegalArgumentException(
                    "route pattern \"" + pattern + "\" has no {name} placeholder; use SsgRoute.page instead");
        }
        Objects.requireNonNull(parameterSets, "parameterSets");
        if (parameterSets.isEmpty()) {
            throw new IllegalArgumentException(
                    "route pattern \"" + pattern + "\" needs at least one parameter set");
        }
        List<Map<String, String>> validated = new ArrayList<>(parameterSets.size());
        for (Map<String, String> parameters : parameterSets) {
            SsgPaths.expand(pattern, parameters, "route pattern");
            validated.add(Map.copyOf(parameters));
        }
        return new SsgRoute(pattern, lang, validated, Objects.requireNonNull(view, "view"));
    }

    /** Returns the declared path or pattern. */
    public String pattern() {
        return pattern;
    }

    /** Returns the document language tag. */
    public String lang() {
        return lang;
    }

    /** Returns the parameter sets in declaration order; immutable. */
    public List<Map<String, String>> parameterSets() {
        return parameterSets;
    }

    /**
     * Expands this route into every page it declares, in declaration order.
     */
    List<Expanded> expand() {
        List<Expanded> pages = new ArrayList<>(parameterSets.size());
        for (Map<String, String> parameters : parameterSets) {
            String path = parameterSets.get(0).isEmpty()
                    ? pattern
                    : SsgPaths.expand(pattern, parameters, "route pattern");
            pages.add(new Expanded(path, parameters));
        }
        return List.copyOf(pages);
    }

    View render(SsgPageContext context) {
        return Objects.requireNonNull(view.apply(context), "route " + context.path() + " returned a null View");
    }

    private static String requireLang(String lang) {
        Objects.requireNonNull(lang, "lang");
        if (lang.isBlank()) {
            throw new IllegalArgumentException("lang must not be blank");
        }
        if (lang.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("lang must not contain whitespace, got \"" + lang + "\"");
        }
        return lang;
    }

    /** One resolved route: its path and the parameters that produced it. */
    record Expanded(String path, Map<String, String> params) {
        Expanded {
            params = Map.copyOf(params);
        }
    }
}
