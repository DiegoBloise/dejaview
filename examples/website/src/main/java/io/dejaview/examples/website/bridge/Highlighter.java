package io.dejaview.examples.website.bridge;

import org.teavm.jso.JSBody;

/**
 * Bridge to the syntax highlighter served by the shell pages
 * ({@code website/js/highlight.js}). The highlighter is an independent
 * language module; keeping it in JavaScript avoids compiling a full tokenizer
 * to Java. When it is absent the page still works — code renders as plain
 * text.
 */
public final class Highlighter {

    private Highlighter() {
    }

    /** Highlights every {@code [data-highlight-java]} block in the document. */
    public static void highlightAll() {
        try {
            highlightDocument();
        } catch (Throwable ignored) {
            // Highlighter unavailable; plain code blocks are fine.
        }
    }

    @JSBody(params = {}, script = "if (window.dejaviewHighlight) { window.dejaviewHighlight.highlightAll(document); }")
    private static native void highlightDocument();
}
