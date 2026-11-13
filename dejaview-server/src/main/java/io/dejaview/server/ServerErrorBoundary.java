package io.dejaview.server;

import io.dejaview.core.Component;
import io.dejaview.core.View;

/**
 * A component that catches failures of the subtree it renders and renders a
 * fallback instead of letting the exception fail the whole server render.
 *
 * <p>On the server, unmounting before responding is meaningless: an unhandled
 * exception must either fail the request with a safe response or be contained
 * by a boundary. A boundary is any mounted component implementing this
 * interface. The protected subtree is whatever the boundary's
 * {@code render()} returns — typically the default slot:</p>
 *
 * <pre>{@code
 * final class PageBoundary extends AbstractComponent implements ServerErrorBoundary {
 *     @Override
 *     public View render() {
 *         return Views.fragment(slot().toArray(View[]::new));
 *     }
 *
 *     @Override
 *     public View fallback(Throwable error) {
 *         return Views.p("This section is unavailable right now.");
 *     }
 * }
 *
 * Views.componentFactory(PageBoundary::new).slot(Views.componentFactory(DangerousSection::new));
 * }</pre>
 *
 * <p>When a component created inside the boundary's subtree throws while
 * binding, initializing, rendering or producing its own subtree, the failed
 * subtree's components are destroyed and closed (reverse creation order, like
 * the rest of a server render), the failure is recorded as a
 * {@link BoundaryError} on the resulting {@link ServerTree}, and the
 * boundary's {@link #fallback(Throwable) fallback} is rendered in place of
 * the failed subtree. The boundary itself stays mounted and is destroyed and
 * closed at the end of the render like any other component.</p>
 *
 * <p>If the fallback itself throws, the failure propagates to the next
 * enclosing boundary, or fails the render as usual.</p>
 */
public interface ServerErrorBoundary extends Component {

    /**
     * Returns the view rendered in place of the failed subtree.
     *
     * @param error the normalized failure; never {@code null}
     */
    View fallback(Throwable error);
}
