package io.dejaview.ssg;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The declarative description of a static site: where to write it, which routes
 * to render, which assets to copy and whether to generate a sitemap.
 *
 * <pre>{@code
 * SsgSite site = SsgSite.builder(Path.of("target/site"))
 *         .baseUrl("https://example.com")
 *         .route(SsgRoute.page("/", "en", HomePage::new))
 *         .asset(Path.of("src/main/webapp/assets"), "assets")
 *         .build();
 * }</pre>
 *
 * <p>Generation is deterministic and request-free: every page gets a fresh
 * injector and {@code ServerContext}, so routes cannot leak state into each
 * other. The output directory is cleaned before writing unless
 * {@link Builder#clean(boolean)} disables it.</p>
 */
public final class SsgSite {

    private final Path outputDir;
    private final List<SsgRoute> routes;
    private final List<SsgAsset> assets;
    private final String baseUrl;
    private final boolean clean;

    private SsgSite(Path outputDir, List<SsgRoute> routes, List<SsgAsset> assets,
                    String baseUrl, boolean clean) {
        this.outputDir = outputDir;
        this.routes = routes;
        this.assets = assets;
        this.baseUrl = baseUrl;
        this.clean = clean;
    }

    /**
     * Starts a site description.
     *
     * @param outputDir the directory that will contain the deployable site; it
     *                  may be relative and is resolved against the working
     *                  directory when generation starts
     */
    public static Builder builder(Path outputDir) {
        return new Builder(outputDir);
    }

    /** Returns the output directory, absolute and normalized. */
    public Path outputDir() {
        return outputDir;
    }

    /** Returns the declared routes in declaration order; immutable. */
    public List<SsgRoute> routes() {
        return routes;
    }

    /** Returns the declared assets in declaration order; immutable. */
    public List<SsgAsset> assets() {
        return assets;
    }

    /**
     * Returns the canonical base URL, without a trailing slash, or an empty
     * string when no sitemap/canonical URLs should be generated.
     */
    public String baseUrl() {
        return baseUrl;
    }

    /** Returns whether the output directory is emptied before generation. */
    public boolean clean() {
        return clean;
    }

    /** Builder for {@link SsgSite}; not thread-safe. */
    public static final class Builder {

        private final Path outputDir;
        private final List<SsgRoute> routes = new ArrayList<>();
        private final List<SsgAsset> assets = new ArrayList<>();
        private String baseUrl = "";
        private boolean clean = true;

        private Builder(Path outputDir) {
            Objects.requireNonNull(outputDir, "outputDir");
            Path absolute = outputDir.toAbsolutePath().normalize();
            if (absolute.getParent() == null) {
                throw new IllegalArgumentException("outputDir must not be a filesystem root: " + outputDir);
            }
            this.outputDir = absolute;
        }

        /** Adds one route. */
        public Builder route(SsgRoute route) {
            routes.add(Objects.requireNonNull(route, "route"));
            return this;
        }

        /** Adds every route of {@code routes}, in order. */
        public Builder routes(List<SsgRoute> routes) {
            Objects.requireNonNull(routes, "routes").forEach(this::route);
            return this;
        }

        /**
         * Copies {@code source} to {@code target} under the output directory.
         *
         * @param source a file or directory that exists at generation time
         * @param target the destination path relative to the output directory
         */
        public Builder asset(Path source, String target) {
            assets.add(new SsgAsset(source, target));
            return this;
        }

        /**
         * Enables canonical links and {@code sitemap.xml} for the site.
         *
         * @param baseUrl an absolute {@code http://} or {@code https://} URL,
         *                for example {@code https://example.com}; a trailing
         *                slash is optional
         */
        public Builder baseUrl(String baseUrl) {
            Objects.requireNonNull(baseUrl, "baseUrl");
            String cleaned = baseUrl.trim();
            while (cleaned.endsWith("/")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }
            if (!cleaned.isEmpty()
                    && !cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
                throw new IllegalArgumentException(
                        "baseUrl must start with http:// or https://, got \"" + baseUrl + "\"");
            }
            if (cleaned.indexOf('?') >= 0 || cleaned.indexOf('#') >= 0) {
                throw new IllegalArgumentException(
                        "baseUrl must not contain a query or fragment, got \"" + baseUrl + "\"");
            }
            this.baseUrl = cleaned;
            return this;
        }

        /**
         * Controls whether the output directory is emptied first. Enabled by
         * default so stale pages never survive a build.
         */
        public Builder clean(boolean clean) {
            this.clean = clean;
            return this;
        }

        /** Validates and returns the site. */
        public SsgSite build() {
            if (routes.isEmpty()) {
                throw new IllegalArgumentException("a site needs at least one route");
            }
            return new SsgSite(outputDir, List.copyOf(routes), List.copyOf(assets), baseUrl, clean);
        }
    }
}
