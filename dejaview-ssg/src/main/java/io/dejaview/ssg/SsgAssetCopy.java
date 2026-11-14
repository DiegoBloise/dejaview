package io.dejaview.ssg;

import java.nio.file.Path;
import java.util.Objects;

/**
 * One asset file copied into the generated site.
 *
 * @param source the original file
 * @param target the copied file inside the output directory, absolute
 */
public record SsgAssetCopy(Path source, Path target) {

    public SsgAssetCopy {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
    }
}
