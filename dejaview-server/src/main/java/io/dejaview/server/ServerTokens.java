package io.dejaview.server;

import io.dejaview.di.InjectionToken;

/**
 * Canonical injection tokens owned by the server platform.
 *
 * <p>Tokens are created with {@link InjectionToken#of(Class)}, so binding with
 * the class and injecting with the token always agree: {@code PLATFORM} is the
 * same token as {@code InjectionToken.of(Platform.class)}.</p>
 */
public final class ServerTokens {

    private ServerTokens() {
    }

    /**
     * The platform this process is running on: {@link ServerPlatform#INSTANCE}
     * on the server. Applications compiled for the browser usually get a
     * browser-side binding from their runtime instead.
     */
    public static final InjectionToken<Platform> PLATFORM = InjectionToken.of(Platform.class);

    /**
     * The request being handled by the current {@link ServerContext}. Bound on
     * the request-scoped child injector, never on the root.
     */
    public static final InjectionToken<ServerRequest> REQUEST = InjectionToken.of(ServerRequest.class);

    /**
     * The current {@link ServerContext}. Bound on the request-scoped child
     * injector, never on the root.
     */
    public static final InjectionToken<ServerContext> CONTEXT = InjectionToken.of(ServerContext.class);
}
