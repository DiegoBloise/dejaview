package io.dejaview.router.internal;

import io.dejaview.router.RouteMatch;

/**
 * The outcome of resolving a URL against a route set.
 */
public final class Resolution {

    /** Discriminates the resolution outcome. */
    public enum Kind {
        /** A chain matched and all guards allowed. */
        MATCHED,
        /** No route matched and no not-found route resolved. */
        NO_MATCH,
        /** A guard (or match pipeline) produced a terminal denial. */
        CANCELLED
    }

    private final Kind kind;
    private final RouteMatch match;
    private final String url;
    private final String reason;

    private Resolution(Kind kind, RouteMatch match, String url, String reason) {
        this.kind = kind;
        this.match = match;
        this.url = url;
        this.reason = reason;
    }

    /** A successful match. */
    public static Resolution matched(RouteMatch match) {
        return new Resolution(Kind.MATCHED, match, match.url(), null);
    }

    /** No route matched; {@code url} is the normalized URL that failed. */
    public static Resolution noMatch(String url) {
        return new Resolution(Kind.NO_MATCH, null, url, "no-match");
    }

    /** A guard denied the navigation; {@code reason} is the guard's reason. */
    public static Resolution cancelled(String url, String reason) {
        return new Resolution(Kind.CANCELLED, null, url, reason);
    }

    /** The discriminator. */
    public Kind kind() {
        return kind;
    }

    /** The matched route, present only when {@link #kind()} is {@code MATCHED}. */
    public RouteMatch match() {
        return match;
    }

    /** The normalized URL that was resolved. */
    public String url() {
        return url;
    }

    /** The diagnostic reason, present when {@code kind()} is not {@code MATCHED}. */
    public String reason() {
        return reason;
    }
}
