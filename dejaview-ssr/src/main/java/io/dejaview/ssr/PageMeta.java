package io.dejaview.ssr;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The mutable document metadata a host assembles per request.
 *
 * <p>Metadata is request-scoped, so it cannot live on the view tree: the same
 * component factories may render different titles for different requests. The
 * host creates one {@code PageMeta} per request, hands it to the components
 * that contribute metadata, and the SSR pipeline turns it into the document
 * {@code <head>} after the view has rendered.</p>
 *
 * <pre>{@code
 * PageMeta meta = new PageMeta()
 *         .lang("pt-BR")
 *         .title("Produtos")
 *         .description("Catálogo de produtos")
 *         .canonical("https://example.com/produtos");
 * meta.meta("robots", "noindex");
 * meta.metaProperty("og:type", "website");
 * }</pre>
 *
 * <p>Setters are fluent and mutate the receiver. The class is not thread-safe:
 * a {@code PageMeta} belongs to the thread that renders the request.</p>
 */
public final class PageMeta {

    private String lang = "en";
    private String title = "";
    private String description = "";
    private String canonical = "";
    private final List<Meta> metas = new ArrayList<>();

    /**
     * One {@code <meta>} tag: either a {@code name}/{@code content} pair or an
     * Open Graph style {@code property}/{@code content} pair.
     *
     * @param attribute {@code "name"} or {@code "property"}
     * @param name      the value of that attribute, never blank
     * @param content   the {@code content} attribute, may be empty
     */
    public record Meta(String attribute, String name, String content) {

        public Meta {
            if (!"name".equals(attribute) && !"property".equals(attribute)) {
                throw new IllegalArgumentException("attribute must be 'name' or 'property', got '" + attribute + "'");
            }
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new IllegalArgumentException("meta name must not be blank");
            }
            Objects.requireNonNull(content, "content");
        }
    }

    /**
     * Returns the document language tag, {@code "en"} by default.
     */
    public String lang() {
        return lang;
    }

    /**
     * Sets the document language tag, for example {@code "en"} or
     * {@code "pt-BR"}. The value is emitted verbatim (escaped) on the
     * {@code <html lang>} attribute.
     */
    public PageMeta lang(String lang) {
        Objects.requireNonNull(lang, "lang");
        if (lang.isBlank()) {
            throw new IllegalArgumentException("lang must not be blank");
        }
        this.lang = lang;
        return this;
    }

    /**
     * Returns the document title, empty when none was set.
     */
    public String title() {
        return title;
    }

    /**
     * Sets the document title. An empty title omits the {@code <title>} tag.
     */
    public PageMeta title(String title) {
        this.title = title == null ? "" : title;
        return this;
    }

    /**
     * Returns the meta description, empty when none was set.
     */
    public String description() {
        return description;
    }

    /**
     * Sets the meta description. An empty description omits the tag.
     */
    public PageMeta description(String description) {
        this.description = description == null ? "" : description;
        return this;
    }

    /**
     * Returns the canonical URL, empty when none was set.
     */
    public String canonical() {
        return canonical;
    }

    /**
     * Sets the canonical URL. An empty value omits the {@code <link rel>} tag.
     * The value is emitted verbatim (escaped); it is the host's responsibility
     * to pass a valid URL.
     */
    public PageMeta canonical(String canonical) {
        this.canonical = canonical == null ? "" : canonical;
        return this;
    }

    /**
     * Adds a {@code <meta name="..." content="...">} tag.
     */
    public PageMeta meta(String name, String content) {
        metas.add(new Meta("name", name, content));
        return this;
    }

    /**
     * Adds a {@code <meta property="..." content="...">} tag, used by
     * Open Graph and similar vocabularies.
     */
    public PageMeta metaProperty(String property, String content) {
        metas.add(new Meta("property", property, content));
        return this;
    }

    /**
     * Returns every added meta tag in insertion order, without the ones the
     * pipeline writes itself (charset, viewport, description).
     */
    public List<Meta> metas() {
        return List.copyOf(metas);
    }
}
