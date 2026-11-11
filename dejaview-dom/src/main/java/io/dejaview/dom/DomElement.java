package io.dejaview.dom;

/**
 * An element in a platform DOM.
 */
public interface DomElement extends DomNode {

    /**
     * Sets an attribute on this element.
     */
    void setAttribute(String name, String value);

    /**
     * Replaces this element's text content.
     *
     * <p>Text is applied through the platform API, never through HTML string
     * concatenation.</p>
     */
    void setText(String text);

    /**
     * Appends a child node.
     *
     * @throws IllegalArgumentException if the node belongs to a different DOM
     *                                  implementation
     */
    void append(DomNode child);

    /**
     * Registers a listener for the given event type.
     */
    void addEventListener(String type, Runnable handler);
}
