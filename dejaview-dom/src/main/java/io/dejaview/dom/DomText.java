package io.dejaview.dom;

/**
 * A text node in a platform DOM.
 */
public interface DomText extends DomNode {

    /**
     * Replaces this node's text.
     */
    void setText(String text);
}
