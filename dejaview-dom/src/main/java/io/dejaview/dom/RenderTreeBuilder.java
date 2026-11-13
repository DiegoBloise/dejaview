package io.dejaview.dom;

import io.dejaview.core.Attribute;
import io.dejaview.core.Component;
import io.dejaview.core.ComponentView;
import io.dejaview.core.ElementView;
import io.dejaview.core.EventBinding;
import io.dejaview.core.FragmentView;
import io.dejaview.core.Property;
import io.dejaview.core.TextView;
import io.dejaview.core.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Creates render nodes and their DOM nodes for a view tree.
 *
 * <p>The builder is shared by the initial mount and by replacement during
 * reconciliation. It never mutates an existing tree.</p>
 *
 * <p>A mounted component receives its bindings and its {@code onInit()} hook
 * before its first render; see {@link ComponentView} and the lifecycle
 * contract on {@link Component}.</p>
 */
final class RenderTreeBuilder {

    private RenderTreeBuilder() {
    }

    static RenderNode build(View view, DomDocument document) {
        Objects.requireNonNull(view, "view");
        RenderNode node = switch (view) {
            case TextView text -> new TextRenderNode(document.createText(text.value()), text.value());
            case FragmentView fragment -> buildFragment(fragment, document);
            case ComponentView component -> {
                Component instance = component.create();
                instance.bind(component.bindings());
                instance.onInit();
                View rendered = Objects.requireNonNull(instance.render(), "component view");
                yield new ComponentRenderNode(instance, build(rendered, document),
                        component.factory(), component.componentType());
            }
            case ElementView element -> buildElement(element, document);
        };
        node.setKey(view.key());
        return node;
    }

    private static RenderNode buildFragment(FragmentView fragment, DomDocument document) {
        List<RenderNode> children = new ArrayList<>();
        for (View child : fragment.children()) {
            children.add(build(child, document));
        }
        return new FragmentRenderNode(children);
    }

    private static RenderNode buildElement(ElementView view, DomDocument document) {
        DomElement element = document.createElement(view.tagName());
        for (Attribute attribute : view.attributes()) {
            element.setAttribute(attribute.name(), attribute.value());
        }
        for (Property property : view.properties()) {
            element.setProperty(property.name(), property.value());
        }

        List<DomEventRegistration> registrations = new ArrayList<>();
        for (EventBinding event : view.events()) {
            registrations.add(register(element, event));
        }

        List<RenderNode> children = new ArrayList<>();
        for (View child : view.children()) {
            RenderNode childNode = build(child, document);
            for (DomNode node : childNode.nodes()) {
                element.append(node);
            }
            children.add(childNode);
        }

        return new ElementRenderNode(element, view, registrations, children);
    }

    /**
     * Registers the correct listener kind for a binding. Value-aware bindings
     * let form controls read the target control state without the core module
     * knowing about browser events.
     */
    static DomEventRegistration register(DomElement element, EventBinding event) {
        if (event.hasValueHandler()) {
            return element.addValueEventListener(event.type(), event.valueHandler()::handle);
        }
        return element.addEventListener(event.type(), event.handler());
    }
}
