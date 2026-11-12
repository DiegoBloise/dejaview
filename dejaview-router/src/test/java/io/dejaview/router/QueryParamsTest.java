package io.dejaview.router;

import io.dejaview.router.internal.UrlParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryParamsTest {

    @Test
    void parsesSimpleQuery() {
        UrlParser.NormalizedUrl url = UrlParser.parse("/users?page=2&sort=name");
        assertEquals(Map.of("page", "2", "sort", "name"), url.queryParams());
        assertEquals("/users?page=2&sort=name", url.url());
        assertEquals("/users", url.path());
    }

    @Test
    void emptyQueryProducesEmptyMap() {
        UrlParser.NormalizedUrl url = UrlParser.parse("/users?");
        assertTrue(url.queryParams().isEmpty());
        assertEquals("/users?", url.url());
    }

    @Test
    void plusMeansSpaceInQuery() {
        UrlParser.NormalizedUrl url = UrlParser.parse("/search?q=hello+world");
        assertEquals("hello world", url.queryParams().get("q"));
    }

    @Test
    void repeatedKeysKeepFirstValue() {
        UrlParser.NormalizedUrl url = UrlParser.parse("/users?tag=a&tag=b&tag=c");
        assertEquals("a", url.queryParams().get("tag"));
    }

    @Test
    void equalsInsideValueIsKept() {
        UrlParser.NormalizedUrl url = UrlParser.parse("/users?q=a=b=c");
        assertEquals("a=b=c", url.queryParams().get("q"));
    }

    @Test
    void keyWithoutValueGetsEmptyValue() {
        UrlParser.NormalizedUrl url = UrlParser.parse("/users?debug");
        assertEquals("", url.queryParams().get("debug"));
    }

    @Test
    void emptyPartsAreSkipped() {
        UrlParser.NormalizedUrl url = UrlParser.parse("/users?a=1&&b=2&");
        assertEquals(Map.of("a", "1", "b", "2"), url.queryParams());
    }

    @Test
    void percentDecodesKeysAndValues() {
        UrlParser.NormalizedUrl url = UrlParser.parse("/users?na%20me=%C3%A7&100%25=done");
        assertEquals("ç", url.queryParams().get("na me"));
        assertEquals("done", url.queryParams().get("100%"));
    }

    @Test
    void malformedPercentSequencesAreKeptRaw() {
        UrlParser.NormalizedUrl url = UrlParser.parse("/users?q=%zz%1%");
        assertEquals("%zz%1%", url.queryParams().get("q"));
    }

    @Test
    void fragmentIsDroppedButQueryRemains() {
        UrlParser.NormalizedUrl url = UrlParser.parse("/users?page=2#section");
        assertEquals("/users?page=2", url.url());
        assertEquals("2", url.queryParams().get("page"));
    }

    @Test
    void utf8PercentDecoding() {
        // "ação" in UTF-8 percent-encoded
        UrlParser.NormalizedUrl url = UrlParser.parse("/users?name=a%C3%A7%C3%A3o");
        assertEquals("ação", url.queryParams().get("name"));
    }

    @Test
    void plusInPathIsNotDecodedToSpace() {
        UrlParser.NormalizedUrl url = UrlParser.parse("/c++/lang");
        assertEquals(List.of("c++", "lang"), url.segments());
    }

    @Test
    void pathSegmentDecoding() {
        UrlParser.NormalizedUrl url = UrlParser.parse("/users/ana%20maria");
        assertEquals(List.of("users", "ana maria"), url.segments());
    }
}
