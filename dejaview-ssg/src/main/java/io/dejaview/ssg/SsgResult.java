package io.dejaview.ssg;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The outcome of one static site generation.
 *
 * @param outputDir the directory written, absolute
 * @param pages     the generated pages in route declaration order
 * @param assets    the copied asset files in copy order
 * @param warnings  non-fatal findings, each tagged with its route
 * @param sitemap   the generated {@code sitemap.xml}, or {@code null} when the
 *                  site has no base URL
 */
public record SsgResult(
        Path outputDir,
        List<SsgPage> pages,
        List<SsgAssetCopy> assets,
        List<SsgWarning> warnings,
        Path sitemap) {

    public SsgResult {
        Objects.requireNonNull(outputDir, "outputDir");
        pages = List.copyOf(pages);
        assets = List.copyOf(assets);
        warnings = List.copyOf(warnings);
    }

    /** Returns the generated sitemap, when one was written. */
    public Optional<Path> sitemapFile() {
        return Optional.ofNullable(sitemap);
    }

    /** Returns the number of generated pages. */
    public int pageCount() {
        return pages.size();
    }

    /** Returns the number of copied asset files. */
    public int assetCount() {
        return assets.size();
    }
}
