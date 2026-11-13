package io.dejaview.dom;

/**
 * Creates nodes for a specific DOM implementation.
 */
public interface DomDocument {

    /**
     * Creates an element with the given tag name.
     *
     * <p>Tag names from the SVG vocabulary (case-sensitive names such as
     * {@code svg}, {@code path}, {@code circle} and {@code linearGradient})
     * are created in the SVG namespace, so inline graphics render correctly.
     * Every other name creates a regular HTML element.</p>
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
