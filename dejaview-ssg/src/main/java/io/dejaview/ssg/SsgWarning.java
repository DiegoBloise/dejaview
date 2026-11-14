package io.dejaview.ssg;

import io.dejaview.ssr.SsrFinding;

import java.util.Objects;

/**
 * A non-fatal serialization finding attached to the page that produced it.
 *
 * @param path    the route path the finding belongs to
 * @param finding the SSR warning (for example {@code skipped-property})
 */
public record SsgWarning(String path, SsrFinding finding) {

    public SsgWarning {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(finding, "finding");
    }
}
