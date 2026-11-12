package io.dejaview.router;

import io.dejaview.core.DejaViewException;
import io.dejaview.reactivity.Signal;
import io.dejaview.router.internal.Resolution;
import io.dejaview.router.internal.RouteResolver;
import io.dejaview.router.internal.UrlParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The client-side router: resolves URLs against the route tree, coordinates
 * history and emits navigation events.
 *
 * <p>Typical setup:</p>
 * <pre>{@code
 * Router router = Router.builder()
 *         .routes(home, users, about)
 *         .notFound(catchAll)
 *         .history(RouterHistory.browser())
 *         .build();
 * }</pre>
 *
 * <p>The initial URL is resolved eagerly at construction; the match (or its
 * absence when no route matches) is committed without emitting events.
 * {@link #navigate(String)} and {@link #replace(String)} resolve the URL
 * <em>before</em> touching history: a denied or unmatched navigation leaves
 * the current match and the history stack unchanged. Resolution happens on
 * the calling thread; listener exceptions propagate to the caller.</p>
 *
 * <p>Navigations initiated by the browser (back/forward) are adopted by the
 * router's history listener and re-resolved; history is never written in that
 * path. A guard that denies a browser-initiated navigation keeps the previous
 * match on screen, although the browser URL has already changed — a known
 * limitation of {@code popstate}.</p>
 */
public final class Router implements AutoCloseable {

    private final RouterHistory history;
    private final RouteResolver resolver;
    private final Signal<Optional<RouteMatch>> current = new Signal<>(Optional.empty());
    private final List<Consumer<NavigationEvent>> listeners = new CopyOnWriteArrayList<>();
    private final AutoCloseable historySubscription;

    private volatile boolean closed;
    private String currentUrl; // normalized URL of the last commit attempt (or initial)

    private Router(List<Route> roots, Route notFound, RouterHistory history) {
        this.history = history;
        this.resolver = new RouteResolver(roots, notFound);
        this.currentUrl = UrlParser.parse(history.current()).url();
        this.historySubscription = history.listen(this::adopt);
        Resolution initial = resolver.resolve(history.current(), event -> { });
        commit(initial);
    }

    /** Starts building a router. See {@link Router} for the typical setup. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Resolves {@code url} and, on success, pushes it onto history.
     *
     * @return {@code true} when the navigation was committed, {@code false}
     *         when it was cancelled (guard denial or no-match)
     * @throws DejaViewException with code {@code closed-router} when the
     *         router is closed, or {@code redirect-loop} when the redirect
     *         budget is exhausted
     */
    public boolean navigate(String url) {
        ensureOpen();
        String norm = normalize(url);
        emit(new NavigationEvent.Started(norm));
        Resolution resolution = resolver.resolve(norm, this::emit);
        switch (resolution.kind()) {
            case MATCHED -> {
                history.push(resolution.match().url());
                commit(resolution);
                emit(new NavigationEvent.Ended(resolution.match().url(), resolution.match()));
                return true;
            }
            case CANCELLED -> {
                emit(new NavigationEvent.Cancelled(resolution.url(), resolution.reason()));
                return false;
            }
            default -> { // NO_MATCH
                emit(new NavigationEvent.Cancelled(resolution.url(), "no-match"));
                return false;
            }
        }
    }

    /**
     * Resolves {@code url} and, on success, replaces the current history
     * entry. See {@link #navigate(String)} for the failure semantics.
     */
    public boolean replace(String url) {
        ensureOpen();
        String norm = normalize(url);
        emit(new NavigationEvent.Started(norm));
        Resolution resolution = resolver.resolve(norm, this::emit);
        switch (resolution.kind()) {
            case MATCHED -> {
                history.replace(resolution.match().url());
                commit(resolution);
                emit(new NavigationEvent.Ended(resolution.match().url(), resolution.match()));
                return true;
            }
            case CANCELLED -> {
                emit(new NavigationEvent.Cancelled(resolution.url(), resolution.reason()));
                return false;
            }
            default -> {
                emit(new NavigationEvent.Cancelled(resolution.url(), "no-match"));
                return false;
            }
        }
    }

    /** Moves one history entry back; adoption happens asynchronously via the history listener. */
    public void back() {
        ensureOpen();
        history.back();
    }

    /** Moves one history entry forward; adoption happens asynchronously via the history listener. */
    public void forward() {
        ensureOpen();
        history.forward();
    }

    /** The underlying history implementation. */
    public RouterHistory history() {
        return history;
    }

    /** The current match, empty when nothing resolved (no-match or initial guard denial). */
    public Optional<RouteMatch> currentMatch() {
        return current.get();
    }

    /**
     * Registers a navigation event listener. Events fire on the calling
     * thread, in resolution order; exceptions propagate to the caller.
     *
     * @return a handle that unregisters the listener
     */
    public EventSubscription events(Consumer<NavigationEvent> listener) {
        ensureOpen();
        Objects.requireNonNull(listener, "listener must not be null");
        listeners.add(listener);
        return new EventSubscription() {
            private boolean removed;

            @Override
            public void remove() {
                if (removed) {
                    return;
                }
                removed = true;
                listeners.remove(listener);
            }
        };
    }

    /** Closes the router: unsubscribes from history and drops event listeners. Idempotent. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            historySubscription.close();
        } catch (Exception e) {
            throw new DejaViewException("history-error", "failed to unsubscribe from history", e);
        } finally {
            listeners.clear();
        }
    }

    // ------------------------------------------------------------------

    Signal<Optional<RouteMatch>> matchSignal() {
        return current;
    }

    private void adopt(String url) {
        if (closed) {
            return;
        }
        String norm = normalize(url);
        if (norm.equals(currentUrl)) {
            return; // our own commit; nothing to adopt
        }
        emit(new NavigationEvent.Started(norm));
        Resolution resolution = resolver.resolve(norm, this::emit);
        switch (resolution.kind()) {
            case MATCHED -> {
                commit(resolution);
                emit(new NavigationEvent.Ended(resolution.match().url(), resolution.match()));
            }
            case CANCELLED ->
                    // Guard denied a browser-initiated navigation: the outlet keeps the
                    // previous match (the browser URL cannot be changed back).
                    emit(new NavigationEvent.Cancelled(resolution.url(), resolution.reason()));
            default -> {
                // No route matches the restored browser URL: clear the outlet.
                current.set(Optional.empty());
                currentUrl = resolution.url();
                emit(new NavigationEvent.Cancelled(resolution.url(), "no-match"));
            }
        }
    }

    private void commit(Resolution resolution) {
        switch (resolution.kind()) {
            case MATCHED -> {
                current.set(Optional.of(resolution.match()));
                currentUrl = resolution.match().url();
            }
            default -> {
                current.set(Optional.empty());
                currentUrl = resolution.url();
            }
        }
    }

    private void emit(NavigationEvent event) {
        for (Consumer<NavigationEvent> listener : listeners) {
            listener.accept(event);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new DejaViewException("closed-router", "router is closed", null);
        }
    }

    private static String normalize(String url) {
        return UrlParser.parse(Objects.requireNonNull(url, "url must not be null")).url();
    }

    /** Builder for {@link Router}. */
    public static final class Builder {

        private final List<Route> roots = new ArrayList<>();
        private Route notFound;
        private RouterHistory history;

        private Builder() {
        }

        /** Adds root routes in declaration order (earlier roots win on overlap). */
        public Builder routes(Route... routes) {
            roots.addAll(List.of(routes));
            return this;
        }

        /** Adds root routes in declaration order. See {@link #routes(Route...)}. */
        public Builder routes(List<Route> routes) {
            roots.addAll(Objects.requireNonNull(routes, "routes must not be null"));
            return this;
        }

        /**
         * Declares the route to resolve when no root matches. It is matched
         * as a normal chain — guards included. Typically {@code path("**")}.
         */
        public Builder notFound(Route route) {
            this.notFound = Objects.requireNonNull(route, "route must not be null");
            return this;
        }

        /** Sets the history implementation. Defaults to {@link MemoryRouterHistory}. */
        public Builder history(RouterHistory history) {
            this.history = Objects.requireNonNull(history, "history must not be null");
            return this;
        }

        /** Validates and builds the router. The initial URL is resolved eagerly. */
        public Router build() {
            if (roots.isEmpty()) {
                throw new DejaViewException("invalid-route", "at least one root route is required", null);
            }
            for (Route root : roots) {
                if (!isRootPath(root.path())) {
                    throw new DejaViewException("invalid-route",
                            "root route '" + root.path() + "' must be '', '/' or start with '/'", null);
                }
            }
            RouterHistory effectiveHistory = history != null ? history : new MemoryRouterHistory();
            return new Router(List.copyOf(roots), notFound, effectiveHistory);
        }

        private static boolean isRootPath(String path) {
            return path.isEmpty() || path.startsWith("/") || path.equals("**");
        }
    }
}
