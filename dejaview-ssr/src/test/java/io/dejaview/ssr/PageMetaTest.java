package io.dejaview.ssr;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageMetaTest {

    @Test
    void defaultsToEnglishAndNoOptionalMetadata() {
        PageMeta meta = new PageMeta();

        assertEquals("en", meta.lang());
        assertEquals("", meta.title());
        assertEquals("", meta.description());
        assertEquals("", meta.canonical());
        assertEquals(List.of(), meta.metas());
    }

    @Test
    void settersAreFluent() {
        PageMeta meta = new PageMeta();

        assertSame(meta, meta.lang("pt-BR"));
        assertSame(meta, meta.title("T"));
        assertSame(meta, meta.description("D"));
        assertSame(meta, meta.canonical("https://example.com/"));
        assertSame(meta, meta.meta("robots", "noindex"));
        assertSame(meta, meta.metaProperty("og:type", "website"));
    }

    @Test
    void nullOptionalValuesBecomeEmpty() {
        PageMeta meta = new PageMeta()
                .title(null)
                .description(null)
                .canonical(null);

        assertEquals("", meta.title());
        assertEquals("", meta.description());
        assertEquals("", meta.canonical());
    }

    @Test
    void metasKeepInsertionOrderAndAreDefensivelyCopied() {
        PageMeta meta = new PageMeta()
                .meta("robots", "noindex")
                .metaProperty("og:title", "Title");

        assertEquals(List.of(
                        new PageMeta.Meta("name", "robots", "noindex"),
                        new PageMeta.Meta("property", "og:title", "Title")),
                meta.metas());
        assertEquals(List.of(), new PageMeta().metas());
    }

    @Test
    void rejectsBlankLanguageAndInvalidMetaEntries() {
        assertThrows(IllegalArgumentException.class, () -> new PageMeta().lang(" "));
        assertThrows(IllegalArgumentException.class, () -> new PageMeta().meta(" ", "x"));
        assertThrows(IllegalArgumentException.class, () -> new PageMeta().metaProperty("", "x"));
        assertThrows(IllegalArgumentException.class, () -> new PageMeta.Meta("itemprop", "x", "y"));
        assertThrows(NullPointerException.class, () -> new PageMeta().meta("robots", null));
    }
}
