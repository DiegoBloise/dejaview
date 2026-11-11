package io.dejaview.dom;

/**
 * Low-level access to the browser document.
 *
 * <p>Application code should prefer the view API. This entry point exists for
 * imperative interop with browser APIs and for the temporary manual updates
 * used by the first milestone example, before the reactivity engine exists.</p>
 */
public final class Dom {

    private Dom() {
    }

    /**
     * Returns the current browser document adapter.
     */
    public static DomDocument document() {
        return BrowserDomDocument.current();
    }

    /**
     * Finds an element in the current browser document by id.
     *
     * @throws IllegalArgumentException if no element has the given id
     */
    public static DomElement byId(String id) {
        return document().getElementById(id);
    }
}
