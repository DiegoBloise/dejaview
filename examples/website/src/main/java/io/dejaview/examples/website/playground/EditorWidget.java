package io.dejaview.examples.website.playground;

import org.teavm.jso.JSBody;

/**
 * Imperative glue for the playground's code editor. Textarea input, scroll
 * sync and Tab insertion are inherently DOM-stateful, so they live behind
 * one-line JS bodies instead of fighting the declarative API. Everything
 * visible stays DejaView.
 */
public final class EditorWidget {

    private EditorWidget() {
    }

    /**
     * Wires input/scroll/keydown listeners once per page lifetime. No-op
     * outside the playground page or when the widget is already initialized.
     */
    public static void init() {
        try {
            initOnce();
        } catch (Throwable ignored) {
            // Editor unavailable; the page still renders.
        }
    }

    /** Re-highlights the editor content, e.g. after switching examples. */
    public static void resync() {
        try {
            resyncHighlight();
        } catch (Throwable ignored) {
            // Plain-text fallback is fine.
        }
    }

    /** Returns the current textarea content (empty when editor is absent). */
    public static String value() {
        try {
            return readValue();
        } catch (Throwable ignored) {
            return "";
        }
    }


    /** Replaces the textarea content, e.g. when the user picks another example. */
    public static void setValue(String value) {
        try {
            writeValue(value);
        } catch (Throwable ignored) {
            // Editor unavailable; nothing to sync.
        }
    }

    @JSBody(params = {"value"}, script =
            "var input = document.getElementById('editor-input');"
            + "if (input) { input.value = value; }")
    private static native void writeValue(String value);

    @JSBody(params = {}, script =
            "var input = document.getElementById('editor-input');"
            + "if (!input || input.dataset.dejaviewReady) { return; }"
            + "input.dataset.dejaviewReady = 'true';"
            + "var pre = document.getElementById('editor-pre');"
            + "var code = document.getElementById('highlight-code');"
            + "function sync() {"
            + "  var source = input.value;"
            + "  if (window.dejaviewHighlight) {"
            + "    code.innerHTML = window.dejaviewHighlight.java(source);"
            + "  } else {"
            + "    code.textContent = source;"
            + "  }"
            + "  if (source.endsWith('\\n')) { code.innerHTML += ' '; }"
            + "  pre.scrollTop = input.scrollTop; pre.scrollLeft = input.scrollLeft;"
            + "}"
            + "input.addEventListener('input', sync);"
            + "input.addEventListener('scroll', function () {"
            + "  pre.scrollTop = input.scrollTop; pre.scrollLeft = input.scrollLeft;"
            + "});"
            + "input.addEventListener('keydown', function (event) {"
            + "  if (event.key === 'Tab') {"
            + "    event.preventDefault();"
            + "    input.setRangeText('    ', input.selectionStart, input.selectionEnd, 'end');"
            + "    sync();"
            + "  }"
            + "});"
            + "sync();")
    private static native void initOnce();

    @JSBody(params = {}, script =
            "var input = document.getElementById('editor-input');"
            + "var code = document.getElementById('highlight-code');"
            + "if (input && code && window.dejaviewHighlight) {"
            + "  code.innerHTML = window.dejaviewHighlight.java(input.value);"
            + "}")
    private static native void resyncHighlight();

    @JSBody(params = {}, script =
            "var input = document.getElementById('editor-input');"
            + "return input ? input.value : '';")
    private static native String readValue();
}
