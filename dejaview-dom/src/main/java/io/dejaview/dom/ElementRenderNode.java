package io.dejaview.dom;

import io.dejaview.core.Attribute;
import io.dejaview.core.ElementView;
import io.dejaview.core.EventBinding;
import io.dejaview.core.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Render node for an element, its listeners and its children.
 */
final class ElementRenderNode implements RenderNode {

    private final DomElement element;
    private final String tagName;
    private List<Attribute> attributes;
    private List<Property> properties;
    private List<EventBinding> events;
    private List<DomEventRegistration> registrations;
    private List<RenderNode> children;
    private String key;

    private boolean disposed;

    ElementRenderNode(
            DomElement element,
            ElementView view,
            List<DomEventRegistration> registrations,
            List<RenderNode> children) {
        this.element = Objects.requireNonNull(element, "element");
        this.tagName = view.tagName();
        this.attributes = view.attributes();
        this.properties = view.properties();
        this.events = view.events();
        this.registrations = new ArrayList<>(registrations);
        this.children = List.copyOf(children);
    }

    String tagName() {
        return tagName;
    }

    DomElement element() {
        return element;
    }

    List<RenderNode> children() {
        return children;
    }

    /**
     * Applies attributes, properties and events from {@code view}. Children are
     * reconciled separately because they need a parent-aware algorithm.
     */
    void apply(ElementView view) {
        if (!tagName.equals(view.tagName())) {
            throw new IllegalArgumentException("Cannot apply a different tag name");
        }
        updateAttributes(view.attributes());
        updateProperties(view.properties());
        updateEvents(view.events());
    }

    void replaceChildren(List<RenderNode> next) {
        children = List.copyOf(next);
    }

    private void updateAttributes(List<Attribute> next) {
        Set<String> nextNames = next.stream().map(Attribute::name).collect(java.util.stream.Collectors.toSet());
        for (Attribute attribute : attributes) {
            if (!nextNames.contains(attribute.name())) {
                element.removeAttribute(attribute.name());
            }
        }
        for (Attribute attribute : next) {
            element.setAttribute(attribute.name(), attribute.value());
        }
        attributes = List.copyOf(next);
    }

    private void updateProperties(List<Property> next) {
        Set<String> nextNames = next.stream().map(Property::name).collect(java.util.stream.Collectors.toSet());
        for (Property property : properties) {
            if (!nextNames.contains(property.name())) {
                element.removeProperty(property.name());
            }
        }
        for (Property property : next) {
            element.setProperty(property.name(), property.value());
        }
        properties = List.copyOf(next);
    }

    private void updateEvents(List<EventBinding> next) {
        if (events.equals(next)) {
            return;
        }
        for (int index = registrations.size() - 1; index >= 0; index--) {
            registrations.get(index).remove();
        }
        registrations = new ArrayList<>();
        for (EventBinding event : next) {
            registrations.add(element.addEventListener(event.type(), event.handler()));
        }
        events = List.copyOf(next);
    }

    @Override
    public List<DomNode> nodes() {
        return List.of(element);
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
        if (disposed) {
            return;
        }
        disposed = true;

        for (int index = registrations.size() - 1; index >= 0; index--) {
            registrations.get(index).remove();
        }
        for (int index = children.size() - 1; index >= 0; index--) {
            children.get(index).dispose();
        }
    }
}
