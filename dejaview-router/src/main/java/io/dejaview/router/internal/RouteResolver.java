package io.dejaview.router.internal;

import io.dejaview.core.DejaViewException;
import io.dejaview.router.NavigationContext;
import io.dejaview.router.NavigationDecision;
import io.dejaview.router.NavigationEvent;
import io.dejaview.router.Route;
import io.dejaview.router.RouteGuard;
import io.dejaview.router.RouteMatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Matches URLs against a route set, applies redirects and evaluates guards.
 *
 * <p>Algorithm:</p>
 * <ol>
 *   <li>Normalize the URL and match it against the configured roots in
 *       declaration order. Each root consumes segments literal- or
 *       parameter-wise; {@code **} consumes the rest. A route with children
 *       passes the remaining segments to its children; when the URL is
 *       exhausted, the index child (empty path) matches if present, otherwise
 *       the route itself activates with an empty child outlet.</li>
 *   <li>If nothing matched and a not-found route is configured, the
 *       not-found route is matched as a normal chain (guards included). If it
 *       matches, it activates; otherwise the resolution is NO_MATCH.</li>
 *   <li>Redirects — whether declared on a route or returned by a guard —
 *       discard the current chain and re-resolve the target from scratch. Up
 *       to ten hops are allowed; beyond that the resolution fails with
 *       {@code DejaViewException} code {@code redirect-loop}.</li>
 *   <li>Guards on the final chain run root-first with prefix-accumulated
 *       contexts. The first non-allow decision wins: {@code Deny} cancels the
 *       navigation; {@code Redirect} re-resolves the target.</li>
 * </ol>
 */
public final class RouteResolver {

    /** Maximum redirect hops tolerated in a single resolution. */
    public static final int MAX_REDIRECT_HOPS = 10;

    private final List<Route> roots;
    private final Route notFound;

    public RouteResolver(List<Route> roots, Route notFound) {
        this.roots = List.copyOf(roots);
        this.notFound = notFound;
    }

    /**
     * Resolves {@code rawUrl}. Emits {@link NavigationEvent.Redirected} per
     * redirect hop through {@code events}.
     *
     * @throws DejaViewException with code {@code redirect-loop} when the hop
     *         budget is exhausted, or {@code guard-error} when a guard returns
     *         {@code null}, or {@code invalid-route} when a redirect produces
     *         no target
     */
    public Resolution resolve(String rawUrl, Consumer<NavigationEvent> events) {
        String current = UrlParser.parse(rawUrl).url();
        int hops = 0;

        hopLoop:
        while (true) {
            if (hops > MAX_REDIRECT_HOPS) {
                throw new DejaViewException("redirect-loop",
                        "redirect limit of " + MAX_REDIRECT_HOPS + " exceeded while resolving '" + rawUrl + "'", null);
            }
            UrlParser.NormalizedUrl norm = UrlParser.parse(current);

            ChainMatch match = matchChain(roots, norm);
            if (match == null && notFound != null) {
                match = matchChain(List.of(notFound), norm);
            }
            if (match == null) {
                return Resolution.noMatch(norm.url());
            }

            // 1) route-declared redirects, root-first on the matched chain
            for (int level = 0; level < match.chain.size(); level++) {
                Route route = match.chain.get(level);
                if (!route.isRedirect()) {
                    continue;
                }
                NavigationContext ctx = prefixContext(norm, match, level);
                String target = route.redirect().apply(ctx);
                if (target == null || target.isBlank()) {
                    throw new DejaViewException("invalid-route",
                            "redirect of route '" + route.path() + "' returned a null or blank target", null);
                }
                events.accept(new NavigationEvent.Redirected(norm.url(), target));
                current = target;
                hops++;
                continue hopLoop;
            }

            // 2) guards on the final chain, root-first
            for (int level = 0; level < match.chain.size(); level++) {
                Route route = match.chain.get(level);
                for (RouteGuard guard : route.guards()) {
                    NavigationContext ctx = prefixContext(norm, match, level);
                    NavigationDecision decision = guard.canActivate(ctx);
                    if (decision == null) {
                        throw new DejaViewException("guard-error",
                                "guard of route '" + route.path() + "' returned null", null);
                    }
                    if (decision instanceof NavigationDecision.Deny deny) {
                        return Resolution.cancelled(norm.url(), deny.reason());
                    }
                    if (decision instanceof NavigationDecision.Redirect redirect) {
                        events.accept(new NavigationEvent.Redirected(norm.url(), redirect.url()));
                        current = redirect.url();
                        hops++;
                        continue hopLoop;
                    }
                }
            }

            return Resolution.matched(toMatch(norm, match));
        }
    }

    // ------------------------------------------------------------------
    // Matching
    // ------------------------------------------------------------------

    private ChainMatch matchChain(List<Route> candidates, UrlParser.NormalizedUrl norm) {
        for (Route root : candidates) {
            ChainMatch match = tryMatch(root, norm.segments(), 0);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private ChainMatch tryMatch(Route route, List<String> segments, int from) {
        List<String> pattern = splitPattern(route.path());
        int i = from;
        Map<String, String> nodeParams = new LinkedHashMap<>();
        for (String part : pattern) {
            if (part.equals("**")) {
                i = segments.size();
                break;
            }
            if (i >= segments.size()) {
                return null;
            }
            if (part.startsWith(":")) {
                nodeParams.put(part.substring(1), segments.get(i));
                i++;
                continue;
            }
            if (!part.equals(segments.get(i))) {
                return null;
            }
            i++;
        }

        if (!route.children().isEmpty()) {
            if (i < segments.size()) {
                ChainMatch childMatch = matchChildren(route.children(), segments, i);
                if (childMatch == null) {
                    return null;
                }
                return prepend(route, nodeParams, childMatch);
            }
            // URL exhausted: the index child matches if present, otherwise the route itself activates.
            for (Route child : route.children()) {
                if (isIndex(child)) {
                    ChainMatch childMatch = tryMatch(child, segments, i);
                    if (childMatch != null) {
                        return prepend(route, nodeParams, childMatch);
                    }
                }
            }
            return single(route, nodeParams);
        }

        if (i != segments.size()) {
            return null; // a leaf must consume the whole URL (or use **)
        }
        return single(route, nodeParams);
    }

    private ChainMatch matchChildren(List<Route> children, List<String> segments, int from) {
        for (Route child : children) {
            ChainMatch match = tryMatch(child, segments, from);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private static boolean isIndex(Route route) {
        return route.path().isEmpty() || route.path().equals("/");
    }

    private static List<String> splitPattern(String path) {
        if (path.isEmpty() || path.equals("/") || path.equals("**")) {
            return path.equals("**") ? List.of("**") : List.of();
        }
        String stripped = path.startsWith("/") ? path.substring(1) : path;
        return List.of(stripped.split("/", -1));
    }

    // ------------------------------------------------------------------
    // Match assembly
    // ------------------------------------------------------------------

    private ChainMatch single(Route route, Map<String, String> nodeParams) {
        List<Route> chain = List.of(route);
        List<Map<String, String>> pps =
                List.of(Collections.unmodifiableMap(new LinkedHashMap<>(nodeParams)));
        List<Map<String, Object>> mps =
                List.of(Collections.unmodifiableMap(new LinkedHashMap<>(route.metadata())));
        return new ChainMatch(chain, pps, mps);
    }

    private ChainMatch prepend(Route route, Map<String, String> nodeParams, ChainMatch child) {
        List<Route> chain = new ArrayList<>(child.chain.size() + 1);
        chain.add(route);
        chain.addAll(child.chain);

        LinkedHashMap<String, String> params = new LinkedHashMap<>(nodeParams);
        List<Map<String, String>> pps = new ArrayList<>();
        pps.add(Collections.unmodifiableMap(new LinkedHashMap<>(params)));
        for (Map<String, String> childLevel : child.paramsPerLevel) {
            params.putAll(childLevel); // children win on key clashes
            pps.add(Collections.unmodifiableMap(new LinkedHashMap<>(params)));
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(route.metadata());
        List<Map<String, Object>> mps = new ArrayList<>();
        mps.add(Collections.unmodifiableMap(new LinkedHashMap<>(metadata)));
        for (Map<String, Object> childLevel : child.metadataPerLevel) {
            metadata.putAll(childLevel); // children win on key clashes
            mps.add(Collections.unmodifiableMap(new LinkedHashMap<>(metadata)));
        }
        return new ChainMatch(chain, pps, mps);
    }

    private NavigationContext prefixContext(UrlParser.NormalizedUrl norm, ChainMatch match, int level) {
        return new NavigationContext(
                norm.url(),
                norm.path(),
                match.paramsPerLevel.get(level),
                norm.queryParams(),
                match.metadataPerLevel.get(level),
                Collections.unmodifiableList(match.chain.subList(0, level + 1)));
    }

    private RouteMatch toMatch(UrlParser.NormalizedUrl norm, ChainMatch match) {
        int last = match.chain.size() - 1;
        return new RouteMatch(norm.url(), norm.path(), match.chain,
                match.paramsPerLevel.get(last), norm.queryParams(), match.metadataPerLevel.get(last));
    }

    private record ChainMatch(List<Route> chain,
                              List<Map<String, String>> paramsPerLevel,
                              List<Map<String, Object>> metadataPerLevel) {
    }
}
