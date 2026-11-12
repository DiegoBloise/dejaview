package io.dejaview.router;

import io.dejaview.core.AbstractComponent;
import io.dejaview.core.ComponentView;
import io.dejaview.core.View;
import io.dejaview.core.Views;

import java.util.Objects;
import java.util.Optional;

/**
 * Renders the route resolved by a {@link Router} at a given chain level.
 *
 * <p>{@code new RouterOutlet(router)} renders the root-level match;
 * {@code new RouterOutlet(router, level)} renders the activation at
 * {@code level} of the matched chain (layouts typically embed the next level,
 * for example {@code new RouterOutlet(router, 1)} inside the layout
 * component). When the level is outside the matched chain the outlet renders
 * an empty fragment.</p>
 *
 * <p>The outlet is keyed by the matched route's stable id ({@code route:N}),
 * so swapping the active route disposes the previous component and mounts the
 * next — navigation never reuses component instances across routes.</p>
 *
 * <p>The match's path parameters are pushed as inputs with the parameter
 * name, so a route component reads {@code /users/:id} with
 * {@code input("id", String.class)}.</p>
 */
public final class RouterOutlet extends AbstractComponent {

    private final Router router;
    private final int level;

    /** Creates an outlet for the root level (0). */
    public RouterOutlet(Router router) {
        this(router, 0);
    }

    /**
     * Creates an outlet for the given chain level.
     *
     * @param level 0 renders the root match; each layout level renders {@code level + 1}
     */
    public RouterOutlet(Router router, int level) {
        this.router = Objects.requireNonNull(router, "router must not be null");
        if (level < 0) {
            throw new IllegalArgumentException("level must not be negative");
        }
        this.level = level;
    }

    @Override
    public String name() {
        return "RouterOutlet";
    }

    @Override
    public View render() {
        Optional<RouteMatch> current = router.matchSignal().get();
        if (current.isEmpty()) {
            return Views.fragment();
        }
        RouteMatch match = current.get();
        if (level >= match.chain().size()) {
            return Views.fragment();
        }
        Route route = match.chain().get(level);
        if (route.componentFactory() == null) {
            // Redirect routes are resolved before render; this is defensive.
            return Views.fragment();
        }
        ComponentView view = route.componentType() != null
                ? Views.componentFactory(route.componentType(), route.componentFactory())
                : Views.componentFactory(route.componentFactory());
        for (var entry : match.params().entrySet()) {
            view = view.input(entry.getKey(), entry.getValue());
        }
        return view.key(route.id());
    }
}
