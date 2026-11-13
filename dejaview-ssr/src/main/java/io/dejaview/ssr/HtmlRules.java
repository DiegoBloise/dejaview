package io.dejaview.ssr;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * The fixed HTML rules shared by {@link SsrValidator} and
 * {@link HtmlSerializer}.
 *
 * <p>Keeping the rules in one place guarantees that a tree the validator
 * accepted is exactly the tree the serializer knows how to write.</p>
 */
final class HtmlRules {

    /**
     * Maximum element nesting depth. Beyond this the tree is rejected instead
     * of risking unbounded recursion or a stack overflow on the rendering
     * thread.
     */
    static final int MAX_DEPTH = 512;

    static final Pattern TAG_NAME = Pattern.compile("^[a-z][a-z0-9-]*$");

    static final Pattern ATTRIBUTE_NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_.:-]*$");

    /**
     * HTML void elements: they never have children and never get a closing tag.
     */
    static final Set<String> VOID_ELEMENTS = Set.of(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr");

    /**
     * Elements whose content the browser treats as raw text rather than markup.
     * The serializer always escapes text nodes, which is safe but changes the
     * meaning of these elements; the validator reports a warning instead of
     * silently emitting something a browser will not execute.
     */
    static final Set<String> RAW_TEXT_ELEMENTS = Set.of("script", "style");

    /**
     * DOM properties that map to a boolean HTML attribute.
     */
    static final Set<String> BOOLEAN_PROPERTIES = Set.of(
            "checked", "selected", "disabled", "readonly", "multiple");

    private HtmlRules() {
    }

    /**
     * Returns the HTML attribute name a DOM property is serialized as, or
     * {@code null} when the property has no HTML representation in this
     * milestone.
     *
     * <p>Only the subset the initial SSR pipeline can represent faithfully is
     * supported: the {@code value} property becomes the {@code value}
     * attribute, and the boolean properties become their same-named attribute.
     * Everything else (for example {@code className}, {@code innerHTML} or
     * {@code tabIndex}) is dropped and reported as a warning.</p>
     */
    static String attributeForProperty(String propertyName) {
        if ("value".equals(propertyName)) {
            return "value";
        }
        if (BOOLEAN_PROPERTIES.contains(propertyName)) {
            return propertyName;
        }
        return null;
    }

    /**
     * Returns whether the property should be serialized at all: a boolean
     * property is only present when its value is {@code "true"}
     * (case-insensitive); a value property is always present.
     */
    static boolean propertyEnabled(String propertyName, String propertyValue) {
        if ("value".equals(propertyName)) {
            return true;
        }
        return BOOLEAN_PROPERTIES.contains(propertyName) && "true".equalsIgnoreCase(propertyValue);
    }
}
