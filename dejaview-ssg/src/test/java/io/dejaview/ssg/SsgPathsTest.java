package io.dejaview.ssg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsgPathsTest {

    @Test
    void urlPathsMapToTheExpectedFiles() {
        assertEquals("index.html", SsgPaths.outputFile("/"));
        assertEquals("about/index.html", SsgPaths.outputFile("/about"));
        assertEquals("about/index.html", SsgPaths.outputFile("/about/"));
        assertEquals("products/keyboard/index.html", SsgPaths.outputFile("/products/keyboard"));
        assertEquals("404.html", SsgPaths.outputFile("/404.html"));
    }

    @Test
    void canonicalUrlsAreDirectoryShapedUnlessTheRouteIsAFile() {
        assertEquals("", SsgPaths.canonical("", "/about"));
        assertEquals("https://example.com/", SsgPaths.canonical("https://example.com", "/"));
        assertEquals("https://example.com/about/", SsgPaths.canonical("https://example.com", "/about"));
        assertEquals("https://example.com/about/", SsgPaths.canonical("https://example.com", "/about/"));
        assertEquals("https://example.com/404.html", SsgPaths.canonical("https://example.com", "/404.html"));
    }

    @Test
    void assetTargetsAreRelativeAndCannotEscapeTheOutput() {
        assertEquals("", SsgPaths.assetTarget("/"));
        assertEquals("assets", SsgPaths.assetTarget("/assets/"));
        assertEquals("assets/site.css", SsgPaths.assetTarget("assets/site.css"));

        assertThrows(IllegalArgumentException.class, () -> SsgPaths.assetTarget("../secret"));
        assertThrows(IllegalArgumentException.class, () -> SsgPaths.assetTarget("assets/../secret"));
        assertThrows(IllegalArgumentException.class, () -> SsgPaths.assetTarget("assets\\site.css"));
        assertThrows(IllegalArgumentException.class, () -> SsgPaths.assetTarget("assets//site.css"));
    }

    @Test
    void externalReferencesAreRecognized() {
        assertTrue(SsgPaths.isExternal("https://example.com/x"));
        assertTrue(SsgPaths.isExternal("http://example.com"));
        assertTrue(SsgPaths.isExternal("//cdn.example.com/x.js"));
        assertTrue(SsgPaths.isExternal("mailto:dev@example.com"));
        assertTrue(SsgPaths.isExternal("tel:+551199999999"));
        assertTrue(SsgPaths.isExternal("data:text/plain,hello"));
        assertFalse(SsgPaths.isExternal("/about"));
        assertFalse(SsgPaths.isExternal("about/"));
        assertFalse(SsgPaths.isExternal("#top"));
    }

    @Test
    void aRoutePathCannotContainASchemeOrDotSegments() {
        assertThrows(IllegalArgumentException.class,
                () -> SsgPaths.requireRoutePath("/https://example.com", "route path"));
        assertThrows(IllegalArgumentException.class,
                () -> SsgPaths.requireRoutePath("/.", "route path"));
        assertEquals("/a.b-c_d~e", SsgPaths.requireRoutePath("/a.b-c_d~e", "route path"));
    }
}
