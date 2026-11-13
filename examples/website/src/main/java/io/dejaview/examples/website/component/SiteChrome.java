package io.dejaview.examples.website.component;

import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.examples.website.Lang;

/**
 * Shared site chrome. Rendered by DejaView on every page; the static shells
 * only carry {@code #app}, metadata and the bundle loader.
 */
public final class SiteChrome {

    private SiteChrome() {
    }

    /** Brand mark as an inline SVG, mirroring the favicon geometry. */
    public static View brandMark() {
        return Views.element("svg",
                        Views.element("rect").attr("x", "4").attr("y", "4").attr("width", "56").attr("height", "56")
                                .attr("rx", "13").attr("fill", "none").attr("stroke", "#8b7cf8")
                                .attr("stroke-width", "3").attr("opacity", "0.45"),
                        Views.element("rect").attr("x", "11.5").attr("y", "11.5").attr("width", "41").attr("height", "41")
                                .attr("rx", "8").attr("fill", "none").attr("stroke", "#22d3ee")
                                .attr("stroke-width", "3").attr("opacity", "0.7"),
                        Views.element("path").attr("d", "M18 41h11l4-13 5 17 4-9h9")
                                .attr("fill", "none").attr("stroke", "#f5f7fb")
                                .attr("stroke-width", "3.4").attr("stroke-linecap", "round").attr("stroke-linejoin", "round"))
                .classes("brand-mark")
                .attr("viewBox", "0 0 64 64")
                .attr("aria-hidden", "true")
                .attr("focusable", "false");
    }

    public static View header(Lang lang, String page) {
        View title = "playground".equals(page)
                ? Views.element("h1", Views.text("Playground")).classes("page-title")
                : Views.fragment();
        return Views.element("header",
                        Views.element("a",
                                        brandMark(),
                                        Views.element("span", Views.text("DejaView")).classes("brand-name"))
                                .classes("brand")
                                .attr("href", lang.homeUrl())
                                .attr("aria-label", lang.t("DejaView home", "Página inicial do DejaView")),
                        title,
                        Views.element("nav",
                                        navLink(lang.homeUrl() + "#features", lang.t("Features", "Recursos")),
                                        navLink(lang.homeUrl() + "#demo", lang.t("Live demo", "Demo ao vivo")),
                                        navLink(lang.getStartedUrl(), lang.t("Get started", "Primeiros passos")),
                                        Views.element("a", Views.text(lang.switchLabel()))
                                                .classes("lang-switch")
                                                .attr("href", lang.switchUrl(page))
                                                .attr("hreflang", lang.isPt() ? "en" : "pt")
                                                .attr("lang", lang.isPt() ? "en" : "pt")
                                                .attr("aria-label", lang.isPt()
                                                        ? "This site in English"
                                                        : "Este site em português"),
                                        Views.element("a", Views.text("Playground"))
                                                .classes("nav-cta")
                                                .attr("href", lang.playgroundUrl()))
                                .classes("site-nav")
                                .attr("aria-label", lang.t("Primary navigation", "Navegação principal")))
                .classes("site-header");
    }

    public static View footer(Lang lang, String page) {
        return Views.element("footer",
                        Views.div(
                                Views.p(
                                        Views.element("span", Views.text("DejaView ")).classes("footer-brand-text"),
                                        Views.element("span", Views.text(lang.t(
                                                "Java. Reimagined for the Web.",
                                                "Java. Reimaginado para a Web."))).classes("footer-tag"))
                                        .classes("footer-brand"),
                                Views.element("nav",
                                                navLink(lang.getStartedUrl(), lang.t("Get started", "Primeiros passos")),
                                                navLink(lang.playgroundUrl(), "Playground"),
                                                Views.element("a", Views.text("GitHub"))
                                                        .attr("href", "https://github.com/dejaview/dejaview")
                                                        .attr("rel", "noopener noreferrer"))
                                        .classes("footer-nav")
                                        .attr("aria-label", lang.t("Footer", "Rodapé")),
                                Views.p(
                                        Views.text(lang.t(
                                                "Made for developers who love Java — and the web. ",
                                                "Feito para quem ama Java — e a web. ")),
                                        Views.element("a", Views.text(lang.switchHint()))
                                                .attr("href", lang.switchUrl(page))
                                                .attr("hreflang", lang.isPt() ? "en" : "pt")
                                                .attr("lang", lang.isPt() ? "en" : "pt"))
                                        .classes("footer-meta"))
                                .classes("container", "footer-inner"))
                .classes("site-footer");
    }

    private static View navLink(String href, String label) {
        return Views.element("a", Views.text(label)).attr("href", href);
    }

    /** Wraps a page's main content with the shared header and footer. */
    public static View page(Lang lang, String pageKind, View content) {
        return page(lang, pageKind, content, true);
    }

    /**
     * Wraps a page's main content with the shared header, optionally without
     * the footer (used by the full-height playground).
     */
    public static View page(Lang lang, String pageKind, View content, boolean showFooter) {
        return Views.fragment(
                header(lang, pageKind),
                Views.element("main", content).id("main"),
                showFooter ? footer(lang, pageKind) : Views.fragment());
    }
}
