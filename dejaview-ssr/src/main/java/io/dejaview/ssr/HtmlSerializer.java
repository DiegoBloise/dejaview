package io.dejaview.ssr;

import io.dejaview.core.Attribute;
import io.dejaview.core.Property;
import io.dejaview.server.ServerElement;
import io.dejaview.server.ServerFragment;
import io.dejaview.server.ServerNode;
import io.dejaview.server.ServerText;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Serializes a validated {@link ServerNode} tree to HTML.
 *
 * <p>Every text node and attribute value is escaped. Tags and attribute names
 * are emitted verbatim; they are only safe because {@link SsrValidator} has
 * already accepted them — call {@link #serialize} or {@link #write} (which
 * validate) or {@code SsrRenderer}, never the package-internal writer directly
 * on an arbitrary tree.</p>
 *
 * <p>The output is a fragment: no document wrapper, no doctype. Use
 * {@link HtmlPage} for a complete document.</p>
 */
public final class HtmlSerializer {

    private HtmlSerializer() {
    }

    /**
     * Validates and serializes {@code node} to an HTML fragment.
     *
     * @param node the tree to serialize; never {@code null}
     * @throws io.dejaview.core.DejaViewException with code
     *                                            {@code ssr-validation-failed}
     *                                            when the tree is malformed
     */
    public static String serialize(ServerNode node) {
        Objects.requireNonNull(node, "node");
        SsrValidator.validate(node);
        StringBuilder out = new StringBuilder();
        try {
            writeValidated(node, out);
        } catch (IOException impossible) {
            // StringBuilder never throws; keep the Appendable-based writer honest.
            throw new UncheckedIOException(impossible);
        }
        return out.toString();
    }

    /**
     * Validates and writes {@code node} as an HTML fragment to {@code out}.
     *
     * @param node the tree to serialize; never {@code null}
     * @param out  the sink, never {@code null}
     * @throws IOException when the sink fails
     */
    public static void write(ServerNode node, Appendable out) throws IOException {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(out, "out");
        SsrValidator.validate(node);
        writeValidated(node, out);
    }

    /**
     * Writes a tree the caller has already validated.
     */
    static void writeValidated(ServerNode node, Appendable out) throws IOException {
        switch (node) {
            case ServerText text -> out.append(escapeText(text.value()));
            case ServerFragment fragment -> {
                for (ServerNode child : fragment.children()) {
                    writeValidated(child, out);
                }
            }
            case ServerElement element -> writeElement(element, out);
        }
    }

    private static void writeElement(ServerElement element, Appendable out) throws IOException {
        out.append('<').append(element.tagName());
        Set<String> written = new HashSet<>();
        for (Attribute attribute : element.attributes()) {
            out.append(' ')
                    .append(attribute.name())
                    .append("=\"")
                    .append(escapeAttribute(attribute.value()))
                    .append('"');
            written.add(attribute.name());
        }
        for (Property property : element.properties()) {
            String attributeName = HtmlRules.attributeForProperty(property.name());
            if (attributeName == null
                    || written.contains(attributeName)
                    || !HtmlRules.propertyEnabled(property.name(), property.value())) {
                continue;
            }
            out.append(' ').append(attributeName);
            if (!HtmlRules.BOOLEAN_PROPERTIES.contains(attributeName)) {
                out.append("=\"").append(escapeAttribute(property.value())).append('"');
            }
            written.add(attributeName);
        }
        out.append('>');
        if (HtmlRules.VOID_ELEMENTS.contains(element.tagName())) {
            return;
        }
        for (ServerNode child : element.children()) {
            writeValidated(child, out);
        }
        out.append("</").append(element.tagName()).append('>');
    }

    /**
     * Escapes text content: {@code &}, {@code <} and {@code >}.
     */
    static String escapeText(String value) {
        StringBuilder escaped = null;
        for (int i = 0; i < value.length(); i++) {
            String replacement = switch (value.charAt(i)) {
                case '&' -> "&amp;";
                case '<' -> "&lt;";
                case '>' -> "&gt;";
                default -> null;
            };
            if (replacement != null) {
                if (escaped == null) {
                    escaped = new StringBuilder(value.length() + 16).append(value, 0, i);
                }
                escaped.append(replacement);
            } else if (escaped != null) {
                escaped.append(value.charAt(i));
            }
        }
        return escaped == null ? value : escaped.toString();
    }

    /**
     * Escapes attribute values: everything text escapes plus the two quote
     * characters.
     */
    static String escapeAttribute(String value) {
        StringBuilder escaped = null;
        for (int i = 0; i < value.length(); i++) {
            String replacement = switch (value.charAt(i)) {
                case '&' -> "&amp;";
                case '<' -> "&lt;";
                case '>' -> "&gt;";
                case '"' -> "&quot;";
                case '\'' -> "&#x27;";
                default -> null;
            };
            if (replacement != null) {
                if (escaped == null) {
                    escaped = new StringBuilder(value.length() + 16).append(value, 0, i);
                }
                escaped.append(replacement);
            } else if (escaped != null) {
                escaped.append(value.charAt(i));
            }
        }
        return escaped == null ? value : escaped.toString();
    }
}
