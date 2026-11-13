package io.dejaview.examples.website.docs;

import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.examples.website.Lang;
import io.dejaview.examples.website.component.CodeBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the {@link Doc.Block} content model into DejaView views. All text
 * is inserted as text nodes, so snippets never need manual escaping.
 */
public final class BlockView {

    private BlockView() {
    }

    public static List<View> renderAll(List<Doc.Block> blocks, Lang lang) {
        List<View> views = new ArrayList<>(blocks.size());
        for (Doc.Block block : blocks) {
            views.add(render(block, lang));
        }
        return views;
    }

    public static View render(Doc.Block block, Lang lang) {
        if (block instanceof Doc.H2 h2) {
            return Views.element("h2", Views.text(h2.text())).id(h2.id());
        }
        if (block instanceof Doc.H3 h3) {
            return Views.element("h3", Views.text(h3.text()));
        }
        if (block instanceof Doc.Prose prose) {
            return Views.element("p", children(prose.segments()));
        }
        if (block instanceof Doc.FencedCode code) {
            return Views.componentFactory(() -> new CodeBlock(lang, code.source(), code.highlightJava()));
        }
        if (block instanceof Doc.Bullets bullets) {
            List<View> items = new ArrayList<>(bullets.items().size());
            for (List<Doc.Segment> item : bullets.items()) {
                items.add(Views.element("li", children(item)));
            }
            return Views.element(bullets.ordered() ? "ol" : "ul", items.toArray(View[]::new));
        }
        if (block instanceof Doc.Note note) {
            return Views.div(
                            Views.element("strong", Views.text(noteLabel(lang, note.tone()))),
                            Views.element("span", children(note.segments())))
                    .classes("callout", "callout-" + note.tone());
        }
        throw new IllegalArgumentException("Unknown block: " + block);
    }

    private static View[] children(List<Doc.Segment> segments) {
        List<View> views = new ArrayList<>(segments.size());
        for (Doc.Segment segment : segments) {
            views.add(segment(segment));
        }
        return views.toArray(View[]::new);
    }

    private static View segment(Doc.Segment segment) {
        if (segment instanceof Doc.Text text) {
            return Views.text(text.value());
        }
        if (segment instanceof Doc.InlineCode code) {
            return Views.element("code", Views.text(code.value()));
        }
        if (segment instanceof Doc.Strong strong) {
            return Views.element("strong", Views.text(strong.value()));
        }
        if (segment instanceof Doc.Link link) {
            return Views.element("a", Views.text(link.label())).attr("href", link.href());
        }
        throw new IllegalArgumentException("Unknown segment: " + segment);
    }

    private static String noteLabel(Lang lang, String tone) {
        return switch (tone) {
            case "tip" -> lang.t("Tip", "Dica");
            case "warning" -> lang.t("Heads up", "Atenção");
            default -> lang.t("Info", "Informação");
        };
    }
}
