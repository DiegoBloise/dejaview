package io.dejaview.router;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The result of a successful resolution: the normalized URL, the matched
 * route chain, and the merged parameters, query parameters and metadata.
 *
 * <p>Instances are immutable and value-based: two matches are equal when
 * their URL, chain, parameters, query parameters and metadata are equal. The
 * renderer uses equality to skip redundant activations.</p>
 */
public final class RouteMatch {

    private final String url;
    private final String path;
    private final List<Route> chain;
    private final Map<String, String> params;
    private final Map<String, String> queryParams;
    private final Map<String, Object> metadata;

    /**
     * Framework use only. Applications receive matches from the router, they
     * never construct them.
     */
    public RouteMatch(String url,
               String path,
               List<Route> chain,
               Map<String, String> params,
               Map<String, String> queryParams,
               Map<String, Object> metadata) {
        this.url = url;
        this.path = path;
        this.chain = List.copyOf(chain);
        this.params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
        this.queryParams = Collections.unmodifiableMap(new LinkedHashMap<>(queryParams));
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    /** The full normalized URL, including the query string, without any fragment. */
    public String url() {
        return url;
    }

    /** The normalized path component of {@link #url()}. */
    public String path() {
        return path;
    }

    /** The matched chain, root to leaf. Never empty on a match. Unmodifiable. */
    public List<Route> chain() {
        return chain;
    }

    /** Path parameters merged child-wins along the chain. Unmodifiable. */
    public Map<String, String> params() {
        return params;
    }

    /** Query parameters, first-wins for repeated keys. Unmodifiable. */
    public Map<String, String> queryParams() {
        return queryParams;
    }

    /** Route metadata merged root-to-leaf (children override parents). Unmodifiable. */
    public Map<String, Object> metadata() {
        return metadata;
    }

    /** Value of a single path parameter, or {@code null} when absent. */
    public String param(String name) {
        return params.get(name);
    }

    /** Value of a single query parameter, or {@code null} when absent. */
    public String queryParam(String name) {
        return queryParams.get(name);
    }

    /** Value of a single metadata key, or {@code null} when absent. */
    public Object metadata(String key) {
        return metadata.get(key);
    }

    /** The number of routes in the matched chain. */
    public int depth() {
        return chain.size();
    }

    /** The leaf route of the chain — the deepest matched route. */
    public Route leaf() {
        return chain.get(chain.size() - 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RouteMatch other)) {
            return false;
        }
        if (!url.equals(other.url) || chain.size() != other.chain.size()) {
            return false;
        }
        for (int i = 0; i < chain.size(); i++) {
            if (!chain.get(i).id().equals(other.chain.get(i).id())) {
                return false;
            }
        }
        return params.equals(other.params)
                && queryParams.equals(other.queryParams)
                && metadata.equals(other.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, params, queryParams, metadata);
    }

    @Override
    public String toString() {
        return "RouteMatch[" + url + ']';
    }
}
