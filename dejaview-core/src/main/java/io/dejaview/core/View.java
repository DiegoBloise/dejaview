package io.dejaview.core;

/**
 * An immutable description of a piece of user interface.
 *
 * <p>{@code View} is the central data type of the DejaView render model. It
 * deliberately contains no reference to the DOM, the browser or any other
 * platform API: the same view tree can be rendered by a browser adapter today
 * and by a server-side HTML renderer later.</p>
 *
 * <p>Views are values. Builder methods such as
 * {@link ElementView#child(View)} return a new instance instead of mutating
 * the receiver, which makes view trees safe to share, test and cache.</p>
 *
 * <p>Every view carries an optional <em>key</em>. Keys are used by the
 * reconciler to preserve node identity across renders of
 * {@code children} lists whose order can change (for example a mapped list
 * of items with stable identifiers). Views without a key are matched by
 * position. See the package documentation for the full keyed reconciliation
 * contract.</p>
 */
public sealed interface View permits ComponentView, ElementView, FragmentView, TextView {

    /**
     * Returns the reconciliation key of this view, or {@code null} when the
     * view is unkeyed and must be matched by position.
     */
    String key();
}
