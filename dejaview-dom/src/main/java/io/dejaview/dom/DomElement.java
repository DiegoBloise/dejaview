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
     * Removes an attribute from this element. Removing a missing attribute has
     * no effect.
     */
    void removeAttribute(String name);

    /**
     * Sets a DOM property on this element.
     */
    void setProperty(String name, String value);

    /**
     * Clears a DOM property on this element. Clearing a missing property has no
     * effect.
     */
    void removeProperty(String name);

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
     * Inserts {@code child} before {@code reference}, or appends it when
     * {@code reference} is {@code null}.
     *
     * @throws IllegalArgumentException if either node belongs to a different
     *                                  DOM implementation
     */
    void insertBefore(DomNode child, DomNode reference);

    /**
     * Removes a child node.
     *
     * @throws IllegalArgumentException if the node belongs to a different DOM
     *                                  implementation
     */
    void removeChild(DomNode child);

    /**
     * Registers a listener for the given event type.
     *
     * @return a handle that removes the listener
     */
    DomEventRegistration addEventListener(String type, Runnable handler);
}
