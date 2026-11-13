package io.dejaview.dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal in-memory DOM used to test renderer behaviour on the JVM.
 */
final class FakeDomDocument implements DomDocument {

    private final Map<String, FakeDomElement> elementsById = new LinkedHashMap<>();
    private final List<FakeDomElement> createdElements = new ArrayList<>();
    private final List<FakeText> createdTexts = new ArrayList<>();

    @Override
    public FakeDomElement createElement(String tagName) {
        FakeDomElement element = new FakeDomElement(this, tagName);
        createdElements.add(element);
        return element;
    }

    @Override
    public FakeText createText(String text) {
        FakeText node = new FakeText(text);
        createdTexts.add(node);
        return node;
    }

    @Override
    public FakeDomElement getElementById(String id) {
        FakeDomElement element = elementsById.get(id);
        if (element == null) {
            throw new IllegalArgumentException("No element found with id '" + id + "'");
        }
        return element;
    }

    List<FakeDomElement> createdElements() {
        return List.copyOf(createdElements);
    }

    List<FakeText> createdTexts() {
        return List.copyOf(createdTexts);
    }

    void registerId(String id, FakeDomElement element) {
        elementsById.put(id, element);
    }
}

class FakeDomNode implements DomNode {
}

final class FakeText extends FakeDomNode implements DomText {

    private String text;

    FakeText(String text) {
        this.text = Objects.requireNonNull(text, "text");
    }

    @Override
    public void setText(String text) {
        this.text = Objects.requireNonNull(text, "text");
    }

    String text() {
        return text;
    }
}

final class FakeDomElement extends FakeDomNode implements DomElement {

    private final FakeDomDocument document;
    private final String tagName;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final Map<String, String> properties = new LinkedHashMap<>();
    private final List<DomNode> children = new ArrayList<>();
    private final Map<String, List<Runnable>> listeners = new LinkedHashMap<>();
    private final Map<String, List<DomValueListener>> valueListeners = new LinkedHashMap<>();
    private String textContent = "";

    FakeDomElement(FakeDomDocument document, String tagName) {
        this.document = Objects.requireNonNull(document, "document");
        this.tagName = Objects.requireNonNull(tagName, "tagName");
    }

    private final Map<String, Integer> attributeSetCounts = new HashMap<>();
    private final Map<String, Integer> propertySetCounts = new HashMap<>();

    @Override
    public void setAttribute(String name, String value) {
        attributes.put(name, value);
        attributeSetCounts.merge(name, 1, Integer::sum);
        if ("id".equals(name)) {
            document.registerId(value, this);
        }
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public void setProperty(String name, String value) {
        properties.put(name, value);
        propertySetCounts.merge(name, 1, Integer::sum);
    }

    @Override
    public void removeProperty(String name) {
        properties.remove(name);
    }

    @Override
    public void setText(String text) {
        this.textContent = Objects.requireNonNull(text, "text");
    }

    @Override
    public void append(DomNode child) {
        children.add(Objects.requireNonNull(child, "child"));
    }

    @Override
    public void insertBefore(DomNode child, DomNode reference) {
        Objects.requireNonNull(child, "child");
        // Like the platform DOM, inserting a node that is already a child
        // moves it instead of duplicating it.
        children.remove(child);
        if (reference == null) {
            children.add(child);
            return;
        }
        int index = children.indexOf(reference);
        if (index < 0) {
            throw new IllegalArgumentException("Reference node is not a child of this element");
        }
        children.add(index, child);
    }

    @Override
    public void removeChild(DomNode child) {
        children.remove(Objects.requireNonNull(child, "child"));
    }

    @Override
    public DomEventRegistration addEventListener(String type, Runnable handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");

        List<Runnable> handlers = listeners.computeIfAbsent(type, ignored -> new ArrayList<>());
        handlers.add(handler);
        return () -> handlers.remove(handler);
    }

    @Override
    public DomEventRegistration addValueEventListener(String type, DomValueListener listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(listener, "listener");

        List<DomValueListener> handlers =
                valueListeners.computeIfAbsent(type, ignored -> new ArrayList<>());
        handlers.add(listener);
        return () -> handlers.remove(listener);
    }

    String tagName() {
        return tagName;
    }

    Map<String, String> attributes() {
        return Map.copyOf(attributes);
    }

    Map<String, String> properties() {
        return Map.copyOf(properties);
    }

    int attributeSetCount(String name) {
        return attributeSetCounts.getOrDefault(name, 0);
    }

    int propertySetCount(String name) {
        return propertySetCounts.getOrDefault(name, 0);
    }

    List<DomNode> children() {
        return List.copyOf(children);
    }

    String textContent() {
        return textContent;
    }

    void fire(String type) {
        for (Runnable listener : List.copyOf(listeners.getOrDefault(type, List.of()))) {
            listener.run();
        }
    }

    void fireValue(String type, String value, boolean checked) {
        for (DomValueListener listener : List.copyOf(valueListeners.getOrDefault(type, List.of()))) {
            listener.handle(value, checked);
        }
    }

    int listenerCount(String type) {
        return listeners.getOrDefault(type, List.of()).size();
    }

    int valueListenerCount(String type) {
        return valueListeners.getOrDefault(type, List.of()).size();
    }
}
