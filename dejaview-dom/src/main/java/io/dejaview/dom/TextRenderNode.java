package io.dejaview.dom;

import java.util.List;
import java.util.Objects;

/**
 * Render node for a platform text node.
 */
final class TextRenderNode implements RenderNode {

    private final DomText node;
    private String text;
    private String key;

    TextRenderNode(DomText node, String text) {
        this.node = Objects.requireNonNull(node, "node");
        this.text = Objects.requireNonNull(text, "text");
    }

    /**
     * Updates the text node when the value changed.
     */
    void update(String next) {
        Objects.requireNonNull(next, "next");
        if (!text.equals(next)) {
            text = next;
            node.setText(next);
        }
    }

    @Override
    public List<DomNode> nodes() {
        return List.of(node);
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public void dispose() {
    }
}
