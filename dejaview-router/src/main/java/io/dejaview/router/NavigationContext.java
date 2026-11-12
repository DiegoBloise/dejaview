package io.dejaview.router;

import java.util.List;
import java.util.Map;

/**
 * Read-only view of a navigation as it exists at one point of the matched
 * route chain, handed to {@link RouteGuard}s and redirect functions.
 *
 * <p>Accumulation is prefix-based: evaluating guards for the route at level
 * <em>k</em> shows the parameters and metadata collected for levels
 * <code>0..k</code>, and the chain up to <code>k</code>. This lets a guard
 * rely on its own route's parameters without seeing later levels.</p>
 *
 * <p>Parameters and query parameters are already URL-decoded. Query parameter
 * names collapse repeated keys with first-wins semantics.</p>
 */
public final class NavigationContext {

    private final String url;
    private final String path;
    private final Map<String, String> params;
    private final Map<String, String> queryParams;
    private final Map<String, Object> metadata;
    private final List<Route> chain;

    /**
     * Framework use only. Applications receive matches from the router, they
     * never construct them.
     */
    public NavigationContext(String url,
                      String path,
                      Map<String, String> params,
                      Map<String, String> queryParams,
                      Map<String, Object> metadata,
                      List<Route> chain) {
        this.url = url;
        this.path = path;
        this.params = params;
        this.queryParams = queryParams;
        this.metadata = metadata;
        this.chain = chain;
    }

    /** The full normalized URL being resolved, including the query string, without any fragment. */
    public String url() {
        return url;
    }

    /** The normalized path component of {@link #url()}. */
    public String path() {
        return path;
    }

    /** Path parameters accumulated down the prefix. Never {@code null}. Unmodifiable. */
    public Map<String, String> params() {
        return params;
    }

    /** Query parameters, first-wins for repeated keys. Never {@code null}. Unmodifiable. */
    public Map<String, String> queryParams() {
        return queryParams;
    }

    /** The matched chain prefix, root to the current route. Never {@code null}. Unmodifiable. */
    public List<Route> chain() {
        return chain;
    }

    /** Value of a single path parameter, or {@code null} when absent. */
    public String param(String name) {
        return params.get(name);
    }

    /** Value of a single query parameter, or {@code null} when absent. */
    public String queryParam(String name) {
        return queryParams.get(name);
    }

    /** Value of a single metadata key from the accumulated prefix, or {@code null} when absent. */
    public Object metadata(String key) {
        return metadata.get(key);
    }

    /** Merged metadata of the accumulated prefix (children override parents). Unmodifiable. */
    public Map<String, Object> metadata() {
        return metadata;
    }
}
