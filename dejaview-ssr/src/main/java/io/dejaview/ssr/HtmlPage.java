package io.dejaview.ssr;

import io.dejaview.server.ServerNode;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Assembles a complete HTML document around a validated server tree.
 *
 * <p>The skeleton is fixed so that every SSR response is a stand-alone
 * document that works without JavaScript:</p>
 *
 * <pre>{@code
 * <!DOCTYPE html>
 * <html lang="en">
 * <head>
 * <meta charset="utf-8">
 * <meta name="viewport" content="width=device-width, initial-scale=1">
 * <title>...</title>
 * ...
 * </head>
 * <body>...</body>
 * </html>
 * }</pre>
 *
 * <p>Title, description and canonical URL are omitted when empty; every value
 * is escaped. The body is serialized by {@link HtmlSerializer}.</p>
 */
public final class HtmlPage {

    private HtmlPage() {
    }

    /**
     * Validates {@code body} and returns a complete HTML document.
     *
     * @param meta the document metadata, never {@code null}
     * @param body the rendered body, never {@code null}
     */
    public static String render(PageMeta meta, ServerNode body) {
        SsrValidator.validate(body);
        StringBuilder out = new StringBuilder(4096);
        try {
            writeValidated(meta, body, out);
        } catch (IOException impossible) {
            throw new UncheckedIOException(impossible);
        }
        return out.toString();
    }

    /**
     * Validates {@code body} and writes a complete HTML document to
     * {@code out}.
     */
    public static void write(PageMeta meta, ServerNode body, Appendable out) throws IOException {
        SsrValidator.validate(body);
        writeValidated(meta, body, out);
    }

    /**
     * Writes a document whose body the caller has already validated.
     */
    static void writeValidated(PageMeta meta, ServerNode body, Appendable out) throws IOException {
        out.append("<!DOCTYPE html>\n");
        out.append("<html lang=\"").append(HtmlSerializer.escapeAttribute(meta.lang())).append("\">\n");
        out.append("<head>\n");
        out.append("<meta charset=\"utf-8\">\n");
        out.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        if (!meta.title().isEmpty()) {
            out.append("<title>").append(HtmlSerializer.escapeText(meta.title())).append("</title>\n");
        }
        if (!meta.description().isEmpty()) {
            out.append("<meta name=\"description\" content=\"")
                    .append(HtmlSerializer.escapeAttribute(meta.description()))
                    .append("\">\n");
        }
        if (!meta.canonical().isEmpty()) {
            out.append("<link rel=\"canonical\" href=\"")
                    .append(HtmlSerializer.escapeAttribute(meta.canonical()))
                    .append("\">\n");
        }
        for (PageMeta.Meta metaTag : meta.metas()) {
            out.append("<meta ").append(metaTag.attribute()).append("=\"")
                    .append(HtmlSerializer.escapeAttribute(metaTag.name()))
                    .append("\" content=\"")
                    .append(HtmlSerializer.escapeAttribute(metaTag.content()))
                    .append("\">\n");
        }
        out.append("</head>\n");
        out.append("<body>");
        HtmlSerializer.writeValidated(body, out);
        out.append("</body>\n");
        out.append("</html>\n");
    }
}
