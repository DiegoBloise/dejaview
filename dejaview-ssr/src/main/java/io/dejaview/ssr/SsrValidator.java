package io.dejaview.ssr;

import io.dejaview.core.DejaViewException;
import io.dejaview.server.ServerElement;
import io.dejaview.server.ServerFragment;
import io.dejaview.server.ServerNode;
import io.dejaview.server.ServerText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Validates a {@link ServerNode} tree before it is serialized to HTML.
 *
 * <p>Validation is the security boundary of the module: the serializer never
 * invents markup, so the only way a tree can produce broken or unsafe HTML is
 * through a malformed tag name, a malformed attribute name, a void element with
 * children, a duplicate attribute, or a tree deep enough to exhaust the stack.
 * Those are rejected with a {@link DejaViewException} carrying the code
 * {@code ssr-validation-failed} and a message listing every violation.</p>
 *
 * <p>Conditions that are safe but lossy become {@link SsrFinding warnings}
 * instead: text inside a raw-text element ({@code script}, {@code style}) is
 * escaped rather than treated as code, DOM properties outside the supported
 * subset are not serialized, and a render that produced no nodes yields an
 * empty body.</p>
 */
public final class SsrValidator {

    private SsrValidator() {
    }

    /**
     * Validates {@code root} and returns its non-fatal findings in a stable
     * order: raw-text elements in first-seen order, then skipped properties in
     * first-seen order, then the empty-root finding.
     *
     * @param root the tree to validate; never {@code null}
     * @return the warnings for a valid tree; empty when there are none
     * @throws DejaViewException with code {@code ssr-validation-failed} when
     *                           the tree cannot be serialized safely
     */
    public static List<SsrFinding> validate(ServerNode root) {
        Objects.requireNonNull(root, "root");
        List<String> violations = new ArrayList<>();
        Set<String> rawTextTags = new LinkedHashSet<>();
        Set<String> skippedProperties = new LinkedHashSet<>();
        walk(root, 0, violations, rawTextTags, skippedProperties);
        if (!violations.isEmpty()) {
            throw new DejaViewException("ssr-validation-failed", String.join("; ", violations), null);
        }
        List<SsrFinding> findings = new ArrayList<>();
        for (String tag : rawTextTags) {
            findings.add(new SsrFinding(
                    "raw-text-escaped",
                    "text inside <" + tag + "> is escaped, so the browser renders it as text instead of raw content"));
        }
        for (String property : skippedProperties) {
            findings.add(new SsrFinding(
                    "skipped-property",
                    "property '" + property + "' has no HTML representation in this milestone and was not serialized"));
        }
        if (isEmpty(root)) {
            findings.add(new SsrFinding("empty-root", "the render produced no content"));
        }
        return List.copyOf(findings);
    }

    private static void walk(
            ServerNode node,
            int depth,
            List<String> violations,
            Set<String> rawTextTags,
            Set<String> skippedProperties) {
        switch (node) {
            case ServerText ignored -> {
                // Text is always escaped by the serializer; nothing to validate.
            }
            case ServerFragment fragment -> {
                for (ServerNode child : fragment.children()) {
                    walk(child, depth, violations, rawTextTags, skippedProperties);
                }
            }
            case ServerElement element -> walkElement(element, depth, violations, rawTextTags, skippedProperties);
        }
    }

    private static void walkElement(
            ServerElement element,
            int depth,
            List<String> violations,
            Set<String> rawTextTags,
            Set<String> skippedProperties) {
        String tag = element.tagName();
        if (depth > HtmlRules.MAX_DEPTH) {
            violations.add("depth limit of " + HtmlRules.MAX_DEPTH + " exceeded at <" + tag + ">");
            return;
        }
        if (!HtmlRules.TAG_NAME.matcher(tag).matches()) {
            violations.add("illegal tag name '" + tag + "'");
        }
        if (HtmlRules.RAW_TEXT_ELEMENTS.contains(tag)) {
            rawTextTags.add(tag);
        }
        if (HtmlRules.VOID_ELEMENTS.contains(tag) && !element.children().isEmpty()) {
            violations.add("void element <" + tag + "> must not have children");
        }
        Set<String> seenAttributes = new HashSet<>();
        for (var attribute : element.attributes()) {
            if (!HtmlRules.ATTRIBUTE_NAME.matcher(attribute.name()).matches()) {
                violations.add("illegal attribute name '" + attribute.name() + "' on <" + tag + ">");
            }
            if (!seenAttributes.add(attribute.name())) {
                violations.add("duplicate attribute '" + attribute.name() + "' on <" + tag + ">");
            }
        }
        for (var property : element.properties()) {
            String attributeName = HtmlRules.attributeForProperty(property.name());
            if (attributeName == null) {
                skippedProperties.add(property.name());
            }
        }
        for (ServerNode child : element.children()) {
            walk(child, depth + 1, violations, rawTextTags, skippedProperties);
        }
    }

    private static boolean isEmpty(ServerNode node) {
        return switch (node) {
            case ServerText text -> text.value().isEmpty();
            case ServerFragment fragment -> fragment.children().isEmpty();
            case ServerElement ignored -> false;
        };
    }
}
