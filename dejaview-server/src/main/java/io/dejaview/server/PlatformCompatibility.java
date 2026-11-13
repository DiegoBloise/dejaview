package io.dejaview.server;

/**
 * Declares which platforms a component is compatible with.
 *
 * <ul>
 *   <li>{@link #UNIVERSAL} — renders on every platform. The default: components
 *       are server-safe unless told otherwise.</li>
 *   <li>{@link #BROWSER_ONLY} — requires browser APIs. The server renderer
 *       refuses to mount it and throws
 *       {@code DejaViewException("browser-only-component", ...)}.</li>
 *   <li>{@link #SERVER_ONLY} — never mounts on the browser. Reserved for
 *       server-rendered content; allowed to render on the server.</li>
 * </ul>
 */
public enum PlatformCompatibility {
    UNIVERSAL,
    BROWSER_ONLY,
    SERVER_ONLY
}
