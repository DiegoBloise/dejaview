package io.dejaview.ssg;

import java.nio.file.Path;
import java.util.Objects;

/**
 * One page written by a static site generation.
 *
 * @param path  the URL path the page answers, for example {@code /about}
 * @param file  the written file, absolute
 * @param title the document title the page metadata produced; empty when none
 */
public record SsgPage(String path, Path file, String title) {

    public SsgPage {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(file, "file");
        title = title == null ? "" : title;
    }
}
