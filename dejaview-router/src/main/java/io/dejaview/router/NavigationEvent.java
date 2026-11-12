package io.dejaview.router;

/**
 * A navigation lifecycle event, emitted to listeners registered with
 * {@code Router#events(java.util.function.Consumer)}.
 *
 * <p>For every navigation the sequence is:</p>
 * <pre>Started(url) → (Redirected(from, to))* → Ended(finalUrl, match)</pre>
 * <p>or, when a guard denies the navigation or no route matches and no
 * not-found route is configured:</p>
 * <pre>Started(url) → (Redirected(from, to))* → Cancelled(url, reason)</pre>
 *
 * <p>{@code Ended} is emitted only after the router state has been committed,
 * so listeners observing it see the new current match. Listener exceptions
 * propagate to the caller that triggered the navigation.</p>
 */
public sealed interface NavigationEvent {

    /** The normalized URL being navigated to (fragment stripped). */
    String url();

    /** Emitted at the start of every navigation, before resolution. */
    record Started(String url) implements NavigationEvent {
    }

    /** Emitted after resolution commits the new current match. */
    record Ended(String url, RouteMatch match) implements NavigationEvent {
    }

    /**
     * Emitted when the navigation does not complete. The current match is
     * left unchanged. {@code reason} is {@code "no-match"}, the
     * {@link NavigationDecision.Deny} reason, or another diagnostic.
     */
    record Cancelled(String url, String reason) implements NavigationEvent {
    }

    /** Emitted once per redirect hop, with the resolved target. */
    record Redirected(String url, String to) implements NavigationEvent {
    }
}
