package io.dejaview.router;

/**
 * The outcome of a {@link RouteGuard} evaluation.
 *
 * <p>Guards run in route order, root first. The first non-{@link Allow}
 * decision wins and prevents the rest of the chain from being evaluated.</p>
 */
public sealed interface NavigationDecision {

    /** Navigation may proceed to the next guard (or to activation). */
    record Allow() implements NavigationDecision {
        @Override
        public String toString() {
            return "Allow";
        }
    }

    /**
     * Navigation must not proceed. The current route stays active and the
     * navigation ends with a {@link NavigationEvent.Cancelled} event whose
     * reason is this reason.
     */
    record Deny(String reason) implements NavigationDecision {
        public Deny {
            reason = reason == null ? "denied" : reason;
        }

        @Override
        public String toString() {
            return "Deny[" + reason + ']';
        }
    }

    /**
     * Navigation must continue at {@code url} instead. Redirects are resolved
     * recursively with a hop limit of ten, after which resolution fails with
     * {@code DejaViewException} code {@code redirect-loop}.
     */
    record Redirect(String url) implements NavigationDecision {
        public Redirect {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("redirect url must not be null or blank");
            }
        }

        @Override
        public String toString() {
            return "Redirect[" + url + ']';
        }
    }

    /** Convenience factory for {@link Allow}. */
    static NavigationDecision allow() {
        return new Allow();
    }

    /** Convenience factory for {@link Deny}. */
    static NavigationDecision deny(String reason) {
        return new Deny(reason);
    }

    /** Convenience factory for {@link Redirect}. */
    static NavigationDecision redirect(String url) {
        return new Redirect(url);
    }
}
