package io.dejaview.ssg;

import io.dejaview.ssr.PageMeta;

import java.util.Map;
import java.util.Objects;

/**
 * The render context of one generated page.
 *
 * <p>A route factory receives this object so it can read the resolved path, the
 * document language, the parameters of a dynamic route and the per-page
 * {@link PageMeta} that the generator assembled for it. Metadata written here
 * becomes the document {@code <head>} and is preserved in the generated file.</p>
 *
 * @param path   the resolved URL path, for example {@code /products/keyboard}
 * @param lang   the document language tag, for example {@code en}
 * @param params the parameter values of a dynamic route; empty for a static
 *               page, never {@code null}
 * @param meta   the mutable, page-scoped metadata; never {@code null}
 */
public record SsgPageContext(String path, String lang, Map<String, String> params, PageMeta meta) {

    public SsgPageContext {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(lang, "lang");
        params = Map.copyOf(Objects.requireNonNull(params, "params"));
        Objects.requireNonNull(meta, "meta");
    }

    /**
     * Returns one route parameter, or throws a clear error when the route did
     * not declare it.
     */
    public String param(String name) {
        Objects.requireNonNull(name, "name");
        String value = params.get(name);
        if (value == null) {
            throw new IllegalArgumentException(
                    "route " + path + " has no parameter \"" + name + "\" (declared: " + params.keySet() + ")");
        }
        return value;
    }
}
