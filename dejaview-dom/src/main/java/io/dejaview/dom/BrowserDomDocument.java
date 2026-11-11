package io.dejaview.dom;

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.Objects;

/**
 * {@link DomDocument} backed by the browser's native document.
 *
 * <p>This class is the only place in the framework that reaches into the
 * browser's global document object. Keeping that boundary small makes the
 * renderer testable and leaves room for a server-side implementation of the
 * same contracts.</p>
 */
public final class BrowserDomDocument implements DomDocument {

    private final HTMLDocument document;

    private BrowserDomDocument(HTMLDocument document) {
        this.document = Objects.requireNonNull(document, "document");
    }

    /**
     * Returns an adapter for the current browser document.
     */
    public static BrowserDomDocument current() {
        return new BrowserDomDocument(HTMLDocument.current());
    }

    @Override
    public DomElement createElement(String tagName) {
        return new BrowserDomElement(document.createElement(tagName));
    }

    @Override
    public DomNode createText(String text) {
        return new BrowserDomNode(document.createTextNode(text));
    }

    @Override
    public DomElement getElementById(String id) {
        HTMLElement element = document.getElementById(id);
        if (element == null) {
            throw new IllegalArgumentException("No element found with id '" + id + "'");
        }
        return new BrowserDomElement(element);
    }
}
