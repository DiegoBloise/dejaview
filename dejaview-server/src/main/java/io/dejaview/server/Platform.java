package io.dejaview.server;

/**
 * A running platform environment.
 *
 * <p>Platforms are process-lifetime values, not per-request state: they expose
 * only what is constant for the whole process (the kind of platform). Anything
 * that varies per request belongs on {@link ServerRequest} or the request-scoped
 * child injector created by {@link ServerContext}.</p>
 */
public interface Platform {

    /**
     * Returns the kind of platform this process is running on.
     */
    PlatformKind kind();

    /**
     * Returns whether this platform is the browser platform.
     */
    default boolean isBrowser() {
        return kind() == PlatformKind.BROWSER;
    }

    /**
     * Returns whether this platform is a server platform.
     */
    default boolean isServer() {
        return kind() == PlatformKind.SERVER;
    }
}
