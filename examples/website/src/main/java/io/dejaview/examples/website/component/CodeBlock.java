package io.dejaview.examples.website.component;

import io.dejaview.core.AbstractComponent;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.examples.website.Lang;
import io.dejaview.examples.website.bridge.Clipboard;
import io.dejaview.reactivity.Signal;

/**
 * Fenced code block with a working copy button, rendered by DejaView.
 * Java samples are marked for the shell's syntax highlighter.
 */
public final class CodeBlock extends AbstractComponent {

    private final Lang lang;
    private final String source;
    private final boolean highlightJava;
    private final Signal<String> copyLabel;

    public CodeBlock(Lang lang, String source, boolean highlightJava) {
        this.lang = lang;
        this.source = source;
        this.highlightJava = highlightJava;
        this.copyLabel = new Signal<>(lang.t("Copy", "Copiar"));
    }

    @Override
    public View render() {
        var code = Views.element("code", Views.text(source));
        if (highlightJava) {
            code = code.attr("data-highlight-java", "");
        }
        return Views.div(
                        Views.element("pre", code),
                        Views.button(copyLabel.get())
                                .classes("copy-btn")
                                .attr("type", "button")
                                .attr("aria-label", lang.t("Copy code", "Copiar código"))
                                .onClick(this::copy))
                .classes("copy-block");
    }

    private void copy() {
        Clipboard.copy(source);
        copyLabel.set(lang.t("Copied!", "Copiado!"));
    }
}
