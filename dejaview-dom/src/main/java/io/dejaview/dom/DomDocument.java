package io.dejaview.dom;

/**
 * Creates nodes for a specific DOM implementation.
 */
public interface DomDocument {

    /**
     * Creates an element with the given tag name.
     */
    DomElement createElement(String tagName);

    /**
     * Creates a text node.
     */
    DomText createText(String text);

    /**
     * Finds an element by id.
     *
     * @throws IllegalArgumentException if no element has the given id
     */
    DomElement getElementById(String id);
}
