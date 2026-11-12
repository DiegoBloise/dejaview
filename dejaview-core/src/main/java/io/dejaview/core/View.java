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
 */
public sealed interface View permits ComponentView, ElementView, FragmentView, TextView {
}
