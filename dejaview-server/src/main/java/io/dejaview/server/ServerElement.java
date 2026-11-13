package io.dejaview.server;

import io.dejaview.core.Attribute;
import io.dejaview.core.Property;
import io.dejaview.core.internal.Preconditions;

import java.util.List;
import java.util.Objects;

/**
 * An element node in the server render tree.
 *
 * <p>Attributes are carried verbatim, in render order. Properties are carried
 * alongside them so a serializer can decide how to encode DOM-property-only
 * state (for example {@code input.value}); the server renderer never interprets
 * them. Event handlers are intentionally absent: they are not serializable and
 * are dropped during rendering.</p>
 *
 * @param tagName    the HTML tag name, lower case by convention; never blank
 * @param attributes attributes in render order, never {@code null}
 * @param properties properties in render order, never {@code null}
 * @param children   child nodes in render order, never {@code null}
 */
public record ServerElement(
        String tagName,
        List<Attribute> attributes,
        List<Property> properties,
        List<ServerNode> children) implements ServerNode {

    public ServerElement {
        tagName = Preconditions.requireText(tagName, "tagName");
        Objects.requireNonNull(attributes, "attributes");
        Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(children, "children");
        attributes = List.copyOf(attributes);
        properties = List.copyOf(properties);
        for (ServerNode child : children) {
            Objects.requireNonNull(child, "children must not contain null");
        }
        children = List.copyOf(children);
    }
}
