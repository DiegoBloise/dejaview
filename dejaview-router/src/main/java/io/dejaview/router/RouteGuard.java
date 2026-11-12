package io.dejaview.router;

/**
 * Decides whether a navigation may proceed.
 *
 * <p>Guards are attached to individual routes and evaluated root-first along
 * the matched chain. A guard that returns {@link NavigationDecision.Allow}
 * lets the chain proceed; {@link NavigationDecision.Deny} cancels the
 * navigation; {@link NavigationDecision.Redirect} discards the remaining
 * chain and re-resolves the target URL. Guards on a redirecting route are
 * never evaluated — there is nothing to activate there.</p>
 *
 * @see NavigationDecision
 */
@FunctionalInterface
public interface RouteGuard {

    /**
     * Evaluates this guard.
     *
     * @param context the URL, parameters, query parameters and route metadata
     *                visible at this point of the matched chain
     * @return the decision; must not return {@code null}
     */
    NavigationDecision canActivate(NavigationContext context);

    /** Returns a guard that always allows navigation. */
    static RouteGuard allowAll() {
        return context -> NavigationDecision.allow();
    }

    /** Returns a guard that always denies navigation with the given reason. */
    static RouteGuard deny(String reason) {
        return context -> NavigationDecision.deny(reason);
    }
}
