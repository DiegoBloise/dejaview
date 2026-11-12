package io.dejaview.examples.website.bridge;

import org.teavm.jso.JSBody;

/**
 * Reads the page marker globals set by the static shell pages before
 * {@code app.js} loads. Each shell in {@code website/} declares which page it
 * hosts so a single compiled bundle can serve the whole site from plain
 * static files.
 */
public final class PageGlobals {

    private PageGlobals() {
    }

    /** Returns the page identifier from {@code window.DEJAVIEW_PAGE}, e.g. {@code "home"}. */
    public static String page() {
        return readPage();
    }

    /** Returns {@code "pt"} or {@code "en"} from {@code window.DEJAVIEW_LANG}. */
    public static String lang() {
        String lang = readLang();
        return lang == null || lang.isBlank() ? "en" : lang;
    }

    @JSBody(params = {}, script = "return window.DEJAVIEW_PAGE || 'home';")
    private static native String readPage();

    @JSBody(params = {}, script = "return window.DEJAVIEW_LANG || 'en';")
    private static native String readLang();
}
