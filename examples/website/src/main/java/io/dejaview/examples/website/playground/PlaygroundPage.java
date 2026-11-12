package io.dejaview.examples.website.playground;

import io.dejaview.core.AbstractComponent;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.examples.website.Lang;
import io.dejaview.examples.website.bridge.Clipboard;
import io.dejaview.examples.website.component.SiteChrome;
import io.dejaview.reactivity.Signal;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive playground, rendered by DejaView. The Java sources shown are
 * the exact sources compiled into the showcase bundle; the preview iframe
 * executes that bundle. The editor itself is wired imperatively by
 * {@link EditorWidget}.
 */
public final class PlaygroundPage extends AbstractComponent {

    private final Lang lang;
    private final Signal<String> currentId = new Signal<>("counter");
    private final Signal<String> previewUrl = new Signal<>("about:blank");
    private final Signal<String> previewStatus;
    private final Signal<String> copyLabel;

    public PlaygroundPage(Lang lang) {
        this.lang = lang;
        this.previewStatus = new Signal<>(lang.t(
                "Select an example and press Run.",
                "Escolha um exemplo e pressione Run."));
        this.copyLabel = new Signal<>(lang.t("Copy source", "Copiar código"));
    }

    private String t(String en, String pt) {
        return lang.t(en, pt);
    }

    @Override
    public View render() {
        Examples.Example current = Examples.byId(currentId.get());

        List<View> exampleItems = new ArrayList<>();
        for (Examples.Example example : Examples.ALL) {
            exampleItems.add(exampleItem(example, example.id().equals(current.id())));
        }

        return SiteChrome.page(lang, "playground",
                Views.element("div",
                        Views.element("aside",
                                Views.element("h2", Views.text(t("Examples", "Exemplos"))),
                                Views.element("ul", exampleItems.toArray(View[]::new))
                                        .id("example-list")
                                        .classes("example-list"),
                                Views.div(
                                        Views.p(Views.element("strong", Views.text(t("How this works.", "Como funciona."))),
                                                        Views.text(t(
                                                                " Examples are written in Java against the real DejaView API, compiled to JavaScript, and executed in the preview iframe exactly like a production deployment.",
                                                                " Os exemplos são escritos em Java contra a API real do DejaView, compilados para JavaScript e executados no iframe de prévia exatamente como um deploy de produção."))))
                                        .classes("pg-note"))
                                .classes("pg-sidebar")
                                .attr("aria-label", t("Example picker", "Seletor de exemplos")),
                        Views.element("section",
                                Views.div(
                                        Views.button(t("▶ Run example", "▶ Rodar exemplo"))
                                                .id("btn-run")
                                                .classes("btn btn-primary")
                                                .attr("type", "button")
                                                .onClick(this::run),
                                        Views.button(copyLabel.get())
                                                .id("btn-copy")
                                                .classes("btn btn-ghost")
                                                .attr("type", "button")
                                                .onClick(this::copySource),
                                        Views.span("").classes("toolbar-spacer"),
                                        Views.span(current.id()).classes("current-example").id("current-example"))
                                        .classes("pg-toolbar"),
                                Views.div(
                                        Views.element("div",
                                                Views.element("div",
                                                        Views.element("pre",
                                                                        Views.element("code").id("highlight-code"))
                                                                .id("editor-pre")
                                                                .classes("editor-highlight")
                                                                .attr("aria-hidden", "true"),
                                                        Views.element("textarea", Views.text(current.source()))
                                                                .id("editor-input")
                                                                .classes("editor-input")
                                                                .attr("spellcheck", "false")
                                                                .attr("autocorrect", "off")
                                                                .attr("autocapitalize", "off")
                                                                .attr("aria-label", t("Java source code", "Código-fonte Java"))
                                                                .attr("wrap", "off"))
                                                        .classes("editor-stack"))
                                                .classes("editor-wrap")
                                                .attr("role", "group")
                                                .attr("aria-label", t("Java source editor", "Editor de código Java")),
                                        Views.div(
                                                Views.div(
                                                        Views.element("h3", Views.text(t("Preview", "Prévia"))),
                                                        Views.p(previewStatus.get())
                                                                .id("preview-status")
                                                                .classes("preview-status")
                                                                .attr("role", "status")
                                                                .attr("aria-live", "polite"))
                                                        .classes("preview-head"),
                                                Views.div(
                                                        Views.element("iframe")
                                                                .id("preview-iframe")
                                                                .classes("preview-iframe")
                                                                .attr("title", t("DejaView example preview", "Prévia do exemplo DejaView"))
                                                                .attr("src", previewUrl.get())
                                                                .on("load", this::previewLoaded))
                                                        .classes("preview-body"))
                                                .classes("preview-wrap"))
                                        .classes("pg-columns"))
                                .classes("pg-workspace")
                                .attr("aria-label", t("Editor and preview", "Editor e prévia")))
                        .classes("pg-main")
                        .id("workspace"),
                false);
    }

    private View exampleItem(Examples.Example example, boolean active) {
        return Views.element("li",
                        Views.button(
                                        Views.span(
                                                Views.span(example.name(lang)).classes("example-item-name"))
                                                .classes("example-item-top"),
                                        Views.span(example.desc(lang)).classes("example-item-desc"))
                                .classes(active ? "example-item active" : "example-item")
                                .attr("type", "button")
                                .attr("data-example", example.id())
                                .attr("aria-pressed", active ? "true" : "false")
                                .onClick(() -> select(example.id())))
                .classes("example-entry");
    }

    private void select(String id) {
        currentId.set(id);
        Examples.Example example = Examples.byId(id);
        EditorWidget.setValue(example.source());
        EditorWidget.resync();
        previewStatus.set(t(
                "Ready — press Run to execute the compiled example.",
                "Pronto — pressione Run para executar o exemplo compilado."));
    }

    private void run() {
        previewStatus.set(t("Loading compiled example…", "Carregando o exemplo compilado…"));
        previewUrl.set("/examples/showcase/target/webapp/index.html?example=" + currentId.get());
    }

    private void previewLoaded() {
        if (!"about:blank".equals(previewUrl.get())) {
            previewStatus.set(t(
                    "Running — compiled from Java, executing for real.",
                    "Rodando — compilado de Java, executando de verdade."));
        }
    }

    private void copySource() {
        Clipboard.copy(EditorWidget.value());
        copyLabel.set(t("Copied!", "Copiado!"));
    }
}
