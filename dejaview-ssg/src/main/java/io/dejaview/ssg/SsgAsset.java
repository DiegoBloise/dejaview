package io.dejaview.ssg;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A file or directory copied into the generated site.
 *
 * <p>{@code target} is relative to the output directory. A file source is
 * copied to exactly that path; a directory source is copied recursively under
 * that prefix, keeping its internal layout. Leading slashes are tolerated, but
 * {@code ..}, empty segments and Windows-style separators are rejected so that
 * an asset can never escape the output directory.</p>
 *
 * @param source the file or directory to copy; must exist when the site is generated
 * @param target the destination path under the output directory, for example
 *               {@code assets} or {@code assets/site.css}
 */
public record SsgAsset(Path source, String target) {

    public SsgAsset {
        source = Objects.requireNonNull(source, "source").normalize();
        target = SsgPaths.assetTarget(target);
        if (source.toString().isBlank()) {
            throw new IllegalArgumentException("asset source must not be blank");
        }
    }
}
