package io.dejaview.examples.website.bridge;

import org.teavm.jso.JSBody;

/**
 * Minimal clipboard bridge. Copy failures (permissions, missing API) are
 * swallowed so the UI never crashes on a restricted browser.
 */
public final class Clipboard {

    private Clipboard() {
    }

    /** Copies {@code text} to the clipboard; no-op when unavailable. */
    public static void copy(String text) {
        try {
            write(text);
        } catch (Throwable ignored) {
            // Older browsers and denied-permission contexts silently skip.
        }
    }

    @JSBody(params = {"text"}, script = "navigator.clipboard.writeText(text);")
    private static native void write(String text);
}
