package io.dejaview.dom;

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.Objects;

/**
 * {@link DomDocument} backed by the browser's native document.
 *
 * <p>This class is the only place in the framework that reaches into the
 * browser's global document object. Keeping that boundary small makes the
 * renderer testable and leaves room for a server-side implementation of the
 * same contracts.</p>
 */
public final class BrowserDomDocument implements DomDocument {

    private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";

    private final HTMLDocument document;

    private BrowserDomDocument(HTMLDocument document) {
        this.document = Objects.requireNonNull(document, "document");
    }

    /**
     * Returns an adapter for the current browser document.
     */
    public static BrowserDomDocument current() {
        return new BrowserDomDocument(HTMLDocument.current());
    }

    @Override
    public DomElement createElement(String tagName) {
        Objects.requireNonNull(tagName, "tagName");
        if (isSvgTag(tagName)) {
            return new BrowserDomElement(document.createElementNS(SVG_NAMESPACE, tagName));
        }
        return new BrowserDomElement(document.createElement(tagName));
    }

    @Override
    public DomText createText(String text) {
        return new BrowserDomText(document.createTextNode(text));
    }

    @Override
    public DomElement getElementById(String id) {
        HTMLElement element = document.getElementById(id);
        if (element == null) {
            throw new IllegalArgumentException("No element found with id '" + id + "'");
        }
        return new BrowserDomElement(element);
    }

    /**
     * SVG element names are case-sensitive and must be created in the SVG
     * namespace; HTML's {@code createElement} would put them in the XHTML
     * namespace, where browsers allocate their box but never paint them.
     * Names that exist in both vocabularies ({@code a}, {@code style},
     * {@code script}, {@code title}) stay HTML because markup generally means
     * the HTML element.
     */
    private static boolean isSvgTag(String tagName) {
        return switch (tagName) {
            case "svg", "g", "defs", "symbol", "use", "path", "rect", "circle",
                 "ellipse", "line", "polyline", "polygon", "text", "tspan",
                 "textPath", "image", "pattern", "mask", "clipPath",
                 "linearGradient", "radialGradient", "stop", "filter", "marker",
                 "foreignObject", "switch", "desc", "metadata", "view",
                 "animate", "animateMotion", "animateTransform", "mpath", "set",
                 "feBlend", "feColorMatrix", "feComponentTransfer", "feComposite",
                 "feConvolveMatrix", "feDiffuseLighting", "feDisplacementMap",
                 "feDistantLight", "feDropShadow", "feFlood", "feFuncA",
                 "feFuncB", "feFuncG", "feFuncR", "feGaussianBlur", "feImage",
                 "feMerge", "feMergeNode", "feMorphology", "feOffset",
                 "fePointLight", "feSpecularLighting", "feSpotLight", "feTile",
                 "feTurbulence" -> true;
            default -> false;
        };
    }
}
