package io.dejaview.router;

import io.dejaview.core.Component;
import io.dejaview.core.DejaViewException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * An immutable route definition: a URL pattern plus the component factory (or
 * redirect) to activate, guards, metadata and child routes.
 *
 * <p>Patterns are slash-separated segments. Three segment kinds exist:</p>
 * <ul>
 *   <li>literal — {@code users}, matched exactly against the decoded segment;</li>
 *   <li>parameter — {@code :id}, matches one segment and captures it;</li>
 *   <li>wildcard — {@code **}, matches the rest of the path; only allowed as
 *       the last segment and consumes everything remaining.</li>
 * </ul>
 *
 * <p>A child route with an empty path ({@code ""}) is the index of its
 * parent. Root routes must start with {@code /} (or be {@code ""}, which is
 * rationalized to {@code /}); child routes must not start with {@code /}.
 * Every route must activate exactly one of a component factory or a redirect.</p>
 *
 * <p>Route definitions are created with {@link #path(String)} and built with
 * {@link Builder#build()}. Built routes are immutable. Component factories
 * are not evaluated at match time — activation is deferred to render.</p>
 */
public final class Route {

    private static final Pattern PARAM_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    private final String id;
    private final String path;
    private final Supplier<? extends Component> componentFactory;
    private final Class<? extends Component> componentType;
    private final Function<NavigationContext, String> redirect;
    private final List<RouteGuard> guards;
    private final Map<String, Object> metadata;
    private final List<Route> children;

    private Route(String id,
                  String path,
                  Supplier<? extends Component> componentFactory,
                  Class<? extends Component> componentType,
                  Function<NavigationContext, String> redirect,
                  List<RouteGuard> guards,
                  Map<String, Object> metadata,
                  List<Route> children) {
        this.id = id;
        this.path = path;
        this.componentFactory = componentFactory;
        this.componentType = componentType;
        this.redirect = redirect;
        this.guards = List.copyOf(guards);
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
        this.children = List.copyOf(children);
    }

    /** Stable identifier assigned at build time. Unique within the JVM. */
    public String id() {
        return id;
    }

    /** The route's path pattern as declared (not normalized for roots). */
    public String path() {
        return path;
    }

    /** The deferred component factory, or {@code null} for redirect routes. */
    public Supplier<? extends Component> componentFactory() {
        return componentFactory;
    }

    /** The class token for component identity, or {@code null} when undeclared. */
    public Class<? extends Component> componentType() {
        return componentType;
    }

    /** The redirect function, or {@code null} for component routes. */
    public Function<NavigationContext, String> redirect() {
        return redirect;
    }

    /** Guards evaluated root-first when this route activates. Never {@code null}. */
    public List<RouteGuard> guards() {
        return guards;
    }

    /** Route-local metadata. Never {@code null}. Unmodifiable. */
    public Map<String, Object> metadata() {
        return metadata;
    }

    /** Child routes in declaration order. Never {@code null}. */
    public List<Route> children() {
        return children;
    }

    /** {@code true} when this route redirects instead of activating a component. */
    public boolean isRedirect() {
        return redirect != null;
    }

    /** Starts building a route for the given path pattern. */
    public static Builder path(String path) {
        return new Builder(path);
    }

    @Override
    public String toString() {
        return "Route[" + path + ']';
    }

    /** Builder for {@link Route}. See {@link Route} for the pattern grammar. */
    public static final class Builder {

        private static final java.util.concurrent.atomic.AtomicInteger IDS =
                new java.util.concurrent.atomic.AtomicInteger();

        private final String path;
        private Supplier<? extends Component> componentFactory;
        private Class<? extends Component> componentType;
        private Function<NavigationContext, String> redirect;
        private final List<RouteGuard> guards = new ArrayList<>();
        private final Map<String, Object> metadata = new LinkedHashMap<>();
        private List<Route> children = List.of();

        private Builder(String path) {
            this.path = Objects.requireNonNull(path, "path must not be null");
        }

        /** Sets the component factory. Mutually exclusive with {@link #redirectTo(String)}. */
        public Builder component(Supplier<? extends Component> factory) {
            Objects.requireNonNull(factory, "factory must not be null");
            this.componentFactory = factory;
            return this;
        }

        /** Sets the component factory with an explicit class token for view identity. */
        public Builder component(Class<? extends Component> type, Supplier<? extends Component> factory) {
            Objects.requireNonNull(type, "type must not be null");
            this.componentType = type;
            this.componentFactory = Objects.requireNonNull(factory, "factory must not be null");
            return this;
        }

        /** Sets a static redirect target. Mutually exclusive with {@link #component(Supplier)}. */
        public Builder redirectTo(String url) {
            Objects.requireNonNull(url, "url must not be null");
            if (url.isBlank()) {
                throw new IllegalArgumentException("redirect url must not be blank");
            }
            this.redirect = context -> url;
            return this;
        }

        /** Sets a computed redirect target. See also {@link #redirectTo(String)}. */
        public Builder redirectTo(Function<NavigationContext, String> function) {
            this.redirect = Objects.requireNonNull(function, "function must not be null");
            return this;
        }

        /** Adds a guard. Guards run in declaration order, root before children. */
        public Builder guard(RouteGuard guard) {
            guards.add(Objects.requireNonNull(guard, "guard must not be null"));
            return this;
        }

        /** Adds several guards. See {@link #guard(RouteGuard)}. */
        public Builder guards(RouteGuard... guards) {
            for (RouteGuard guard : guards) {
                guard(guard);
            }
            return this;
        }

        /** Sets a route-local metadata entry. Later keys override earlier ones. */
        public Builder metadata(String key, Object value) {
            metadata.put(
                    io.dejaview.core.internal.Preconditions.requireText(key, "key"),
                    Objects.requireNonNull(value, "value must not be null"));
            return this;
        }

        /** Sets the child routes, replacing any previously declared children. */
        public Builder children(Route... children) {
            this.children = List.of(children);
            return this;
        }

        /** Validates and builds the immutable route. */
        public Route build() {
            String normalized = normalizeAndValidate(path);
            if (redirect != null && componentFactory != null) {
                throw invalid("route '" + path + "' declares both a component and a redirect");
            }
            if (redirect == null && componentFactory == null) {
                throw invalid("route '" + path + "' declares neither a component nor a redirect");
            }
            if (redirect != null && !children.isEmpty()) {
                throw invalid("redirect route '" + path + "' must not declare children");
            }
            if (hasWildcard(normalized) && !children.isEmpty()) {
                throw invalid("wildcard route '" + path + "' must not declare children");
            }
            for (Route child : children) {
                if (child.path.startsWith("/")) {
                    throw invalid("child route '" + child.path + "' of '" + path
                            + "' must not start with '/'");
                }
            }
            String id = "route:" + IDS.incrementAndGet();
            return new Route(id, normalized, componentFactory, componentType, redirect,
                    guards, metadata, children);
        }

        private static String normalizeAndValidate(String raw) {
            String p = raw;
            if (p.equals("/")) {
                return "/";
            }
            if (p.indexOf('?') >= 0 || p.indexOf('#') >= 0) {
                throw invalid("route path '" + p + "' must not contain '?' or '#'");
            }
            while (p.endsWith("/") && p.length() > 1) {
                p = p.substring(0, p.length() - 1);
            }
            if (p.contains("//")) {
                throw invalid("route path '" + p + "' must not contain duplicate '/'");
            }
            String[] segments = p.split("/", -1);
            int start = (segments.length > 0 && segments[0].isEmpty()) ? 1 : 0;
            boolean wildcardSeen = false;
            java.util.Set<String> paramNames = new java.util.HashSet<>();
            for (int i = start; i < segments.length; i++) {
                String segment = segments[i];
                if (segment.isEmpty()) {
                    throw invalid("route path '" + p + "' contains an empty segment");
                }
                if (wildcardSeen) {
                    throw invalid("wildcard '**' must be the final segment in '" + p + "'");
                }
                if (segment.equals("**")) {
                    wildcardSeen = true;
                    continue;
                }
                if (segment.startsWith(":")) {
                    String name = segment.substring(1);
                    if (!PARAM_NAME.matcher(name).matches()) {
                        throw invalid("invalid parameter name '" + name + "' in '" + p + "'");
                    }
                    if (!paramNames.add(name)) {
                        throw invalid("duplicate parameter ':" + name + "' in '" + p + "'");
                    }
                    continue;
                }
                if (segment.startsWith("*")) {
                    throw invalid("unsupported wildcard '" + segment + "' in '" + p + "'; only '**' is supported");
                }
            }
            return p;
        }

        private static boolean hasWildcard(String normalized) {
            return normalized.equals("**") || normalized.endsWith("/**");
        }

        private static DejaViewException invalid(String message) {
            return new DejaViewException("invalid-route", message, null);
        }
    }
}
