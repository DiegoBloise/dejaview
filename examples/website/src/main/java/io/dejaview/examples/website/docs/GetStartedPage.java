package io.dejaview.examples.website.docs;

import io.dejaview.core.AbstractComponent;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.examples.website.Lang;
import io.dejaview.examples.website.component.SiteChrome;

import java.util.List;

/**
 * User documentation — the complete step-by-step tutorial, rendered by
 * DejaView. Content lives in {@link TutorialEn} / {@link TutorialPt}; the
 * table of contents is generated from the headings.
 */
public final class GetStartedPage extends AbstractComponent {

    private final Lang lang;

    public GetStartedPage(Lang lang) {
        this.lang = lang;
    }

    @Override
    public View render() {
        List<Doc.Block> blocks = lang.isPt() ? TutorialPt.content() : TutorialEn.content();

        List<View> article = new java.util.ArrayList<>();
        article.add(Views.p(lang.t("Get started", "Primeiros passos")).classes("eyebrow"));
        article.add(Views.element("h1", Views.text(lang.t(
                        "From zero to a running DejaView app",
                        "Do zero a um app DejaView rodando"))).classes("docs-title"));
        article.add(Views.p(lang.t(
                        "A complete, hands-on tour of the framework — every step is real, runnable code.",
                        "Um tour completo e prático pelo framework — cada passo é código real e executável."))
                .classes("section-sub"));
        article.addAll(BlockView.renderAll(blocks, lang));

        return SiteChrome.page(lang, "get-started",
                Views.div(
                        toc(blocks),
                        Views.element("article", article.toArray(View[]::new))
                                .classes("docs-content"))
                        .classes("container", "docs-layout"));
    }

    private View toc(List<Doc.Block> blocks) {
        List<View> links = new java.util.ArrayList<>();
        for (Doc.Block block : blocks) {
            if (block instanceof Doc.H2 h2) {
                links.add(Views.element("a", Views.text(h2.text()))
                        .attr("href", "#" + h2.id()));
            }
        }
        return Views.element("aside",
                        Views.p(lang.t("On this page", "Nesta página")).classes("toc-title"),
                        Views.element("nav", links.toArray(View[]::new))
                                .classes("docs-toc-nav")
                                .attr("aria-label", lang.t("Table of contents", "Sumário")))
                .classes("docs-toc");
    }
}
