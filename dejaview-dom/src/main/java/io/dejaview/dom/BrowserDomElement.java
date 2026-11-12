package io.dejaview.dom;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.Objects;

/**
 * Browser implementation detail: adapts a JSO {@link HTMLElement} to the
 * platform-neutral {@link DomElement} contract.
 */
final class BrowserDomElement extends BrowserDomNode implements DomElement {

    private final HTMLElement element;

    BrowserDomElement(HTMLElement element) {
        super(element);
        this.element = Objects.requireNonNull(element, "element");
    }

    @Override
    public void setAttribute(String name, String value) {
        element.setAttribute(name, value);
    }

    @Override
    public void setText(String text) {
        element.setTextContent(text);
    }

    @Override
    public void append(DomNode child) {
        if (!(child instanceof BrowserDomNode browserNode)) {
            throw new IllegalArgumentException(
                    "Cannot append a node created by a different DomDocument implementation");
        }
        element.appendChild(browserNode.jsoNode());
    }

    @Override
    public void removeChild(DomNode child) {
        if (!(child instanceof BrowserDomNode browserNode)) {
            throw new IllegalArgumentException(
                    "Cannot remove a node created by a different DomDocument implementation");
        }
        element.removeChild(browserNode.jsoNode());
    }

    @Override
    public DomEventRegistration addEventListener(String type, Runnable handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");

        EventListener<Event> listener = event -> handler.run();
        element.addEventListener(type, listener);
        return new DomEventRegistration() {
            private boolean removed;

            @Override
            public void remove() {
                if (removed) {
                    return;
                }
                removed = true;
                element.removeEventListener(type, listener);
            }
        };
    }
}
