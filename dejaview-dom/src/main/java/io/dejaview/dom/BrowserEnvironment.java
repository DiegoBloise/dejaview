package io.dejaview.dom;

import org.teavm.jso.browser.Window;

/**
 * Detects whether the runtime is executing inside a browser.
 *
 * <p>TeaVM exposes the global {@code window} object. On a plain JVM there is no
 * browser global, so the lookup fails and the runtime reports that it is not in
 * a browser. This lets the same code base be exercised by JVM tests and by the
 * compiled JavaScript output.</p>
 */
public final class BrowserEnvironment {

    private BrowserEnvironment() {
    }

    /**
     * Returns whether a browser global is available.
     */
    public static boolean isBrowser() {
        try {
            return Window.current() != null;
        } catch (RuntimeException | LinkageError unavailable) {
            // The JVM has no browser global; TeaVM JSO calls cannot resolve it.
            return false;
        }
    }
}
