package io.dejaview.server;

/**
 * Identifies the platform a DejaView application is running on.
 *
 * <p>The enum is intentionally small. Renderers, adapters and platform bindings
 * switch on it when behavior genuinely differs between environments; anything
 * that works everywhere must not branch on it at all.</p>
 */
public enum PlatformKind {

    /**
     * The browser platform: DOM, events, storage and TeaVM-compiled to native
     * JavaScript.
     */
    BROWSER,

    /**
     * A server platform: no DOM, no browser events, no browser storage. The
     * server render tree produced by {@link ServerRenderer} is the only UI
     * representation available.
     */
    SERVER
}
