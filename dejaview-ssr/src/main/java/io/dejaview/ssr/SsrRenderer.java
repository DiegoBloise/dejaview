package io.dejaview.ssr;

import io.dejaview.core.View;
import io.dejaview.server.ServerContext;
import io.dejaview.server.ServerRenderer;
import io.dejaview.server.ServerTree;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;
import java.util.Objects;

/**
 * Renders a {@link View} to a server response on the JVM.
 *
 * <p>This is the entry point of the SSR pipeline:</p>
 * <ol>
 *   <li>{@link ServerRenderer} mounts the view, running the ordinary component
 *       lifecycle once and dropping event handlers;</li>
 *   <li>{@link SsrValidator} rejects trees that cannot be serialized safely and
 *       collects the warnings;</li>
 *   <li>{@link HtmlPage} / {@link HtmlSerializer} write escaped HTML.</li>
 * </ol>
 *
 * <p>The pipeline is fail-safe: an unhandled render failure or a validation
 * failure yields status {@code 500} with a minimal generic document that leaks
 * nothing about the exception. The failure is attached to the
 * {@link SsrResult} so the host can log it. A render that contained
 * {@link io.dejaview.server.ServerErrorBoundary} failures or serialization
 * warnings still returns {@code 200}.</p>
 *
 * <p>The writer variants render and validate the whole document before writing
 * anything, so a failure never leaves a partial document in the sink. They are
 * not progressive streaming: the HTML is flushed once. True progressive
 * streaming remains a later milestone.</p>
 */
public final class SsrRenderer {

    /**
     * The page returned for an unhandled render or validation failure. It names
     * the status only: the exception stays in the host's logs.
     */
    static final String GENERIC_ERROR_HTML = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>500 Internal Server Error</title>
            </head>
            <body><h1>500 Internal Server Error</h1></body>
            </html>
            """;

    private SsrRenderer() {
    }

    /**
     * Renders {@code view} as a complete HTML document.
     *
     * @param view    the page view; never {@code null}
     * @param context the request context; never {@code null}, must not be closed
     * @param meta    the request-scoped document metadata; never {@code null}
     */
    public static SsrResult renderPage(View view, ServerContext context, PageMeta meta) {
        Objects.requireNonNull(meta, "meta");
        return render(view, context, meta, true);
    }

    /**
     * Renders {@code view} as an HTML fragment with no document wrapper.
     */
    public static SsrResult renderFragment(View view, ServerContext context) {
        return render(view, context, null, false);
    }

    /**
     * Renders a complete document and writes it to {@code out}.
     *
     * @return the same result the sink received; inspect it for warnings,
     *         boundary failures or the attached render error
     */
    public static SsrResult writePage(
            View view, ServerContext context, PageMeta meta, Writer out) throws IOException {
        Objects.requireNonNull(out, "out");
        SsrResult result = renderPage(view, context, meta);
        out.write(result.html());
        return result;
    }

    /**
     * Renders a fragment and writes it to {@code out}.
     *
     * @return the same result the sink received
     */
    public static SsrResult writeFragment(
            View view, ServerContext context, Writer out) throws IOException {
        Objects.requireNonNull(out, "out");
        SsrResult result = renderFragment(view, context);
        out.write(result.html());
        return result;
    }

    private static SsrResult render(View view, ServerContext context, PageMeta meta, boolean page) {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(context, "context");
        ServerTree tree;
        try {
            tree = ServerRenderer.render(view, context);
        } catch (RuntimeException failure) {
            return failure(failure);
        }
        List<SsrFinding> warnings;
        try {
            warnings = SsrValidator.validate(tree.root());
        } catch (RuntimeException failure) {
            return failure(failure);
        }
        String html;
        try {
            StringBuilder out = new StringBuilder(4096);
            if (page) {
                HtmlPage.writeValidated(meta, tree.root(), out);
            } else {
                HtmlSerializer.writeValidated(tree.root(), out);
            }
            html = out.toString();
        } catch (IOException impossible) {
            // StringBuilder never throws; the Appendable-based writers are honest.
            throw new UncheckedIOException(impossible);
        } catch (RuntimeException failure) {
            return failure(failure);
        }
        return new SsrResult(
                200, html, tree.droppedEvents(), tree.boundaryErrors(), warnings, null);
    }

    private static SsrResult failure(Throwable error) {
        return new SsrResult(500, GENERIC_ERROR_HTML, List.of(), List.of(), List.of(), error);
    }
}
