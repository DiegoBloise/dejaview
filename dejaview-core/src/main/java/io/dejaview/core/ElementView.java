package io.dejaview.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An element node in a view tree.
 *
 * <p>Instances are immutable. Each builder method returns a new element with
 * the requested change, leaving the original untouched:</p>
 *
 * <pre>{@code
 * ElementView base = Views.div();
 * ElementView decorated = base.id("app").child(Views.text("Hello"));
 * // base.children() is still empty
 * }</pre>
 *
 * <p>Attribute names must be unique. Event bindings may repeat because a
 * single event can legitimately have multiple listeners.</p>
 *
 * @param tagName    non-blank HTML tag name
 * @param attributes attributes in render order
 * @param children   child views in render order
 * @param events     event bindings in registration order
 */
public record ElementView(
        String tagName,
        List<Attribute> attributes,
        List<View> children,
        List<EventBinding> events) implements View {

    public ElementView {
        Objects.requireNonNull(tagName, "tagName");
        Objects.requireNonNull(attributes, "attributes");
        Objects.requireNonNull(children, "children");
        Objects.requireNonNull(events, "events");
        if (tagName.isBlank()) {
            throw new IllegalArgumentException("Tag name must not be blank");
        }
        attributes = List.copyOf(attributes);
        children = List.copyOf(children);
        events = List.copyOf(events);
        rejectDuplicateAttributes(attributes);
    }

    /**
     * Returns a copy of this element with an additional attribute.
     *
     * @throws IllegalArgumentException if the attribute is already present
     */
    public ElementView attr(String name, String value) {
        List<Attribute> updated = new ArrayList<>(attributes);
        updated.add(new Attribute(name, value));
        return new ElementView(tagName, updated, children, events);
    }

    /**
     * Returns a copy of this element with the given {@code id} attribute.
     */
    public ElementView id(String id) {
        return attr("id", id);
    }

    /**
     * Returns a copy of this element with a space-separated {@code class}
     * attribute.
     *
     * @throws IllegalArgumentException if no class name is provided or a name
     *                                  is blank
     */
    public ElementView classes(String... classNames) {
        Objects.requireNonNull(classNames, "classNames");
        if (classNames.length == 0) {
            throw new IllegalArgumentException("At least one class name is required");
        }
        for (String className : classNames) {
            if (className == null || className.isBlank()) {
                throw new IllegalArgumentException("Class names must not be null or blank");
            }
        }
        return attr("class", String.join(" ", classNames));
    }

    /**
     * Returns a copy of this element with one additional child.
     */
    public ElementView child(View child) {
        Objects.requireNonNull(child, "child");
        List<View> updated = new ArrayList<>(children);
        updated.add(child);
        return new ElementView(tagName, attributes, updated, events);
    }

    /**
     * Returns a copy of this element with additional children appended in
     * order.
     */
    public ElementView addChildren(View... newChildren) {
        Objects.requireNonNull(newChildren, "newChildren");
        if (newChildren.length == 0) {
            return this;
        }
        List<View> updated = new ArrayList<>(children);
        for (View child : newChildren) {
            updated.add(Objects.requireNonNull(child, "child"));
        }
        return new ElementView(tagName, attributes, updated, events);
    }

    /**
     * Returns a copy of this element with an event listener registered for
     * the given event type.
     */
    public ElementView on(String eventType, Runnable handler) {
        List<EventBinding> updated = new ArrayList<>(events);
        updated.add(new EventBinding(eventType, handler));
        return new ElementView(tagName, attributes, children, updated);
    }

    /**
     * Returns a copy of this element with a {@code click} listener.
     */
    public ElementView onClick(Runnable handler) {
        return on("click", handler);
    }

    private static void rejectDuplicateAttributes(List<Attribute> attributes) {
        for (int i = 0; i < attributes.size(); i++) {
            String name = attributes.get(i).name();
            for (int j = i + 1; j < attributes.size(); j++) {
                if (name.equals(attributes.get(j).name())) {
                    throw new IllegalArgumentException("Duplicate attribute: " + name);
                }
            }
        }
    }
}
