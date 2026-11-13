package io.dejaview.server;

/**
 * The server platform: no DOM, no browser events, no browser storage.
 *
 * <p>{@link #INSTANCE} is the single instance bound to
 * {@link ServerTokens#PLATFORM} by {@link ServerBindings#apply}. It holds no
 * state, so sharing it between requests is safe.</p>
 */
public final class ServerPlatform implements Platform {

    /**
     * The singleton server platform.
     */
    public static final ServerPlatform INSTANCE = new ServerPlatform();

    private ServerPlatform() {
    }

    @Override
    public PlatformKind kind() {
        return PlatformKind.SERVER;
    }

    @Override
    public String toString() {
        return "server";
    }
}
