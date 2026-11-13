package io.dejaview.dom;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.xml.Element;

import java.util.Objects;

/**
 * Browser implementation detail: adapts a JSO {@link Element} to the
 * platform-neutral {@link DomElement} contract.
 *
 * <p>{@code Element} covers both HTML and SVG elements, so views can build
 * inline graphics as well as regular markup.</p>
 */
final class BrowserDomElement extends BrowserDomNode implements DomElement {

    private final Element element;

    BrowserDomElement(Element element) {
        super(element);
        this.element = Objects.requireNonNull(element, "element");
    }

    @Override
    public void setAttribute(String name, String value) {
        element.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        element.removeAttribute(name);
    }

    @Override
    public void setProperty(String name, String value) {
        setJsProperty(element, name, value);
    }

    @Override
    public void removeProperty(String name) {
        clearJsProperty(element, name);
    }

    @Override
    public void setText(String text) {
        element.setTextContent(text);
    }

    @Override
    public void append(DomNode child) {
        element.appendChild(browserNode(child).jsoNode());
    }

    @Override
    public void insertBefore(DomNode child, DomNode reference) {
        if (reference == null) {
            append(child);
            return;
        }
        element.insertBefore(browserNode(child).jsoNode(), browserNode(reference).jsoNode());
    }

    @Override
    public void removeChild(DomNode child) {
        element.removeChild(browserNode(child).jsoNode());
    }

    @Override
    public DomEventRegistration addEventListener(String type, Runnable handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");

        EventListener<Event> listener = event -> handler.run();
        return register(type, listener);
    }

    @Override
    public DomEventRegistration addValueEventListener(String type, DomValueListener listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(listener, "listener");

        EventListener<Event> eventListener = event ->
                listener.handle(eventValue(event), eventChecked(event));
        return register(type, eventListener);
    }

    private DomEventRegistration register(String type, EventListener<Event> listener) {
        listen(element, type, listener);
        return new DomEventRegistration() {
            private boolean removed;

            @Override
            public void remove() {
                if (removed) {
                    return;
                }
                removed = true;
                unlisten(element, type, listener);
            }
        };
    }

    private static BrowserDomNode browserNode(DomNode node) {
        if (!(node instanceof BrowserDomNode browserNode)) {
            throw new IllegalArgumentException(
                    "Cannot use a node created by a different DomDocument implementation");
        }
        return browserNode;
    }

    @JSBody(
            params = {"element", "name", "value"},
            script = "element[name] = value === 'true' ? true : value === 'false' ? false : value;")
    private static native void setJsProperty(JSObject element, String name, String value);

    @JSBody(
            params = {"event"},
            script = "var target = event.target; return String(target && target.value != null ? target.value : '');")
    private static native String eventValue(Event event);

    @JSBody(
            params = {"event"},
            script = "var target = event.target; return !!(target && target.checked);")
    private static native boolean eventChecked(Event event);

    @JSBody(
            params = {"element", "name"},
            script = "element[name] = null;")
    private static native void clearJsProperty(JSObject element, String name);

    @JSBody(
            params = {"element", "type", "listener"},
            script = "element.addEventListener(type, listener);")
    private static native void listen(JSObject element, String type, EventListener<Event> listener);

    @JSBody(
            params = {"element", "type", "listener"},
            script = "element.removeEventListener(type, listener);")
    private static native void unlisten(JSObject element, String type, EventListener<Event> listener);
}
