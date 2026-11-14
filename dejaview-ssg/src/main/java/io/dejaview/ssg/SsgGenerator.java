package io.dejaview.ssg;

import io.dejaview.core.View;
import io.dejaview.di.Injector;
import io.dejaview.server.ServerContext;
import io.dejaview.server.ServerRequest;
import io.dejaview.ssr.PageMeta;
import io.dejaview.ssr.SsrFinding;
import io.dejaview.ssr.SsrRenderer;
import io.dejaview.ssr.SsrResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Renders a {@link SsgSite} into a directory that can be uploaded to any static
 * host.
 *
 * <p>The pipeline is deliberately simple and deterministic:</p>
 * <ol>
 *   <li>every route is expanded and validated (duplicate paths and duplicate
 *       output files are build failures);</li>
 *   <li>every page is rendered through {@link SsrRenderer} with its own
 *       injector, {@link ServerContext} and {@link PageMeta}, so pages cannot
 *       share state;</li>
 *   <li>assets are planned, checked for collisions and copied;</li>
 *   <li>the output directory is prepared (cleaned by default) and the pages
 *       are written;</li>
 *   <li>a {@code sitemap.xml} is written when the site declares a base URL;
 *       and</li>
 *   <li>every local {@code href}/{@code src} in every page is checked against
 *       the files that were written.</li>
 * </ol>
 *
 * <p>Failures are {@link SsgException}s whose message names the route or asset
 * that caused them. Pages are rendered before anything is written, but asset
 * copying and page writing happen while the build runs: a failure during the
 * final link check leaves a partial (but already written) output directory.
 * Enable {@link SsgSite.Builder#clean(boolean)} (the default) so the next run
 * starts from a clean tree.</p>
 */
public final class SsgGenerator {

    private SsgGenerator() {
    }

    /**
     * Generates {@code site}.
     *
     * @param site the site description; never {@code null}
     * @return what was written
     * @throws SsgException when a route, a render, an asset or a local
     *                      reference cannot be produced
     */
    public static SsgResult generate(SsgSite site) {
        Objects.requireNonNull(site, "site");
        List<PlannedPage> planned = plan(site);
        List<RenderedPage> rendered = render(site, planned);
        List<AssetPlan> assetPlan = planAssets(site, planned);
        prepareOutput(site);
        List<SsgAssetCopy> assets = copyAssets(site, assetPlan);
        List<SsgPage> pages = writePages(site, rendered);
        Path sitemap = writeSitemap(site, rendered);
        validateReferences(rendered, assetPlan);
        return new SsgResult(site.outputDir(), pages, assets, warnings(rendered), sitemap);
    }

    // --- planning ---------------------------------------------------------

    private record PlannedPage(SsgRoute route, SsgRoute.Expanded expanded, String relativeFile) {
    }

    private static List<PlannedPage> plan(SsgSite site) {
        List<PlannedPage> planned = new ArrayList<>();
        Map<String, String> paths = new LinkedHashMap<>();
        Map<String, String> files = new LinkedHashMap<>();
        for (SsgRoute route : site.routes()) {
            for (SsgRoute.Expanded expanded : route.expand()) {
                String path = expanded.path();
                String previousPath = paths.putIfAbsent(path, route.pattern());
                if (previousPath != null) {
                    throw new SsgException("duplicate route path \"" + path
                            + "\" (declared by \"" + previousPath + "\" and \"" + route.pattern() + "\")");
                }
                String file = SsgPaths.outputFile(path);
                String previousFile = files.putIfAbsent(file, path);
                if (previousFile != null) {
                    throw new SsgException("routes \"" + previousFile + "\" and \"" + path
                            + "\" both write " + file);
                }
                planned.add(new PlannedPage(route, expanded, file));
            }
        }
        return List.copyOf(planned);
    }

    // --- rendering --------------------------------------------------------

    private record RenderedPage(String path, String html, String title, List<SsrFinding> findings) {
    }

    private static List<RenderedPage> render(SsgSite site, List<PlannedPage> planned) {
        List<RenderedPage> rendered = new ArrayList<>(planned.size());
        for (PlannedPage page : planned) {
            String path = page.expanded().path();
            PageMeta meta = new PageMeta().lang(page.route().lang());
            if (!site.baseUrl().isEmpty()) {
                meta.canonical(SsgPaths.canonical(site.baseUrl(), path));
            }
            Injector root = Injector.builder().name("ssg").build();
            try (ServerContext context = ServerContext.create(root, ServerRequest.of("GET", path))) {
                SsgPageContext pageContext = new SsgPageContext(
                        path, page.route().lang(), page.expanded().params(), meta);
                View view;
                try {
                    view = page.route().render(pageContext);
                } catch (RuntimeException failure) {
                    throw failure(path, failure);
                }
                SsrResult result = SsrRenderer.renderPage(view, context, meta);
                if (!result.isSuccess()) {
                    throw failure(path, result.error());
                }
                rendered.add(new RenderedPage(path, result.html(), meta.title(), result.warnings()));
            } finally {
                root.close();
            }
        }
        return List.copyOf(rendered);
    }

    private static SsgException failure(String path, Throwable error) {
        String detail = error == null ? "unknown error"
                : error.getClass().getSimpleName() + (error.getMessage() == null ? "" : ": " + error.getMessage());
        return new SsgException("static generation failed for route \"" + path + "\" (" + detail + ")", error);
    }

    private static List<SsgWarning> warnings(List<RenderedPage> rendered) {
        List<SsgWarning> warnings = new ArrayList<>();
        for (RenderedPage page : rendered) {
            for (SsrFinding finding : page.findings()) {
                warnings.add(new SsgWarning(page.path(), finding));
            }
        }
        return List.copyOf(warnings);
    }

    // --- output -----------------------------------------------------------

    private static void prepareOutput(SsgSite site) {
        Path output = site.outputDir();
        try {
            if (site.clean() && Files.exists(output)) {
                refuseDangerousClean(output);
                deleteRecursively(output);
            }
            Files.createDirectories(output);
        } catch (IOException e) {
            throw new SsgException("could not prepare output directory " + output + ": " + e.getMessage(), e);
        }
    }

    private static void refuseDangerousClean(Path output) {
        Path absolute = output.toAbsolutePath().normalize();
        if (absolute.getParent() == null) {
            throw new SsgException("refusing to clean filesystem root " + absolute);
        }
        if (Files.exists(absolute.resolve("pom.xml"))) {
            throw new SsgException("refusing to clean " + absolute
                    + ": it contains pom.xml, so it looks like a project directory");
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // --- assets -----------------------------------------------------------

    private record AssetPlan(Path source, String relativeTarget) {
    }

    private static List<AssetPlan> planAssets(SsgSite site, List<PlannedPage> pages) {
        List<AssetPlan> plan = new ArrayList<>();
        Map<String, Path> targets = new LinkedHashMap<>();
        for (SsgAsset asset : site.assets()) {
            Path source = asset.source();
            if (!Files.exists(source)) {
                throw new SsgException("asset source does not exist: " + source);
            }
            if (Files.isRegularFile(source)) {
                if (asset.target().isEmpty()) {
                    throw new SsgException("asset file " + source + " needs a target path");
                }
                addAsset(plan, targets, source, asset.target());
            } else if (Files.isDirectory(source)) {
                try (var stream = Files.walk(source)) {
                    for (Path file : stream.filter(Files::isRegularFile).toList()) {
                        Path relative = source.relativize(file);
                        String target = asset.target().isEmpty()
                                ? slash(relative)
                                : asset.target() + "/" + slash(relative);
                        addAsset(plan, targets, file, target);
                    }
                } catch (IOException e) {
                    throw new SsgException("could not read asset directory " + source + ": " + e.getMessage(), e);
                }
            } else {
                throw new SsgException("asset source is neither a file nor a directory: " + source);
            }
        }
        for (PlannedPage page : pages) {
            Path collision = targets.get(page.relativeFile());
            if (collision != null) {
                throw new SsgException("asset " + collision + " would overwrite generated page "
                        + page.expanded().path() + " (" + page.relativeFile() + ")");
            }
        }
        return List.copyOf(plan);
    }

    private static void addAsset(List<AssetPlan> plan, Map<String, Path> targets,
                                 Path source, String target) {
        String normalized = SsgPaths.assetTarget(target);
        Path previous = targets.putIfAbsent(normalized, source);
        if (previous != null) {
            throw new SsgException("duplicate asset target \"" + normalized + "\" ("
                    + previous + " and " + source + ")");
        }
        plan.add(new AssetPlan(source, normalized));
    }

    private static List<SsgAssetCopy> copyAssets(SsgSite site, List<AssetPlan> plan) {
        List<SsgAssetCopy> copies = new ArrayList<>(plan.size());
        for (AssetPlan asset : plan) {
            Path target = site.outputDir().resolve(asset.relativeTarget()).normalize();
            if (!target.startsWith(site.outputDir())) {
                throw new SsgException("asset target escapes the output directory: " + asset.relativeTarget());
            }
            try {
                Files.createDirectories(target.getParent());
                Files.copy(asset.source(), target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new SsgException("could not copy asset " + asset.source() + " to " + target
                        + ": " + e.getMessage(), e);
            }
            copies.add(new SsgAssetCopy(asset.source(), target));
        }
        return List.copyOf(copies);
    }

    // --- pages and sitemap ------------------------------------------------

    private static List<SsgPage> writePages(SsgSite site, List<RenderedPage> rendered) {
        List<SsgPage> pages = new ArrayList<>(rendered.size());
        for (RenderedPage page : rendered) {
            Path file = site.outputDir().resolve(SsgPaths.outputFile(page.path())).normalize();
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, page.html(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new SsgException("could not write page \"" + page.path() + "\" to " + file
                        + ": " + e.getMessage(), e);
            }
            pages.add(new SsgPage(page.path(), file, page.title()));
        }
        return List.copyOf(pages);
    }

    private static Path writeSitemap(SsgSite site, List<RenderedPage> rendered) {
        if (site.baseUrl().isEmpty()) {
            return null;
        }
        StringBuilder xml = new StringBuilder(256 + rendered.size() * 96);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (RenderedPage page : rendered) {
            // A 404 document is an error page, not an indexable page.
            if (page.path().endsWith("/404.html")) {
                continue;
            }
            xml.append("  <url><loc>")
                    .append(escapeXml(SsgPaths.canonical(site.baseUrl(), page.path())))
                    .append("</loc></url>\n");
        }
        xml.append("</urlset>\n");
        Path file = site.outputDir().resolve("sitemap.xml");
        try {
            Files.writeString(file, xml.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SsgException("could not write sitemap to " + file + ": " + e.getMessage(), e);
        }
        return file;
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // --- link validation --------------------------------------------------

    private static void validateReferences(List<RenderedPage> rendered, List<AssetPlan> assets) {
        Set<String> knownFiles = new LinkedHashSet<>();
        for (RenderedPage page : rendered) {
            knownFiles.add(SsgPaths.outputFile(page.path()));
        }
        for (AssetPlan asset : assets) {
            knownFiles.add(asset.relativeTarget());
        }
        List<String> problems = new ArrayList<>();
        for (RenderedPage page : rendered) {
            for (StaticReferences.Problem problem : StaticReferences.validate(
                    page.path(), page.html(), knownFiles)) {
                problems.add(problem.page() + " -> " + problem.attribute()
                        + "=\"" + problem.url() + "\" does not resolve to a generated file");
            }
        }
        if (!problems.isEmpty()) {
            throw new SsgException("static reference validation failed: " + String.join("; ", problems));
        }
    }

    private static String slash(Path path) {
        return path.toString().replace('\\', '/');
    }
}
