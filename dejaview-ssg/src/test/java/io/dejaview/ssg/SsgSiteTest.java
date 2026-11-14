package io.dejaview.ssg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsgSiteTest {

    @TempDir
    Path tempDir;

    private SsgSite.Builder builder() {
        return SsgSite.builder(tempDir.resolve("site"));
    }

    @Test
    void aSiteNeedsAtLeastOneRoute() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> builder().build());

        assertTrue(error.getMessage().contains("at least one route"));
    }

    @Test
    void theOutputDirectoryIsAbsoluteAndCannotBeARoot() {
        SsgSite site = builder()
                .route(SsgRoute.page("/", "en", meta -> null))
                .build();

        assertTrue(site.outputDir().isAbsolute());
        assertThrows(IllegalArgumentException.class, () -> SsgSite.builder(Path.of("/")));
    }

    @Test
    void theBaseUrlIsNormalizedAndMustBeAbsoluteHttp() {
        SsgSite site = builder()
                .baseUrl("https://example.com/")
                .route(SsgRoute.page("/", "en", meta -> null))
                .build();
        assertEquals("https://example.com", site.baseUrl());

        assertThrows(IllegalArgumentException.class, () -> builder().baseUrl("example.com"));
        assertThrows(IllegalArgumentException.class, () -> builder().baseUrl("ftp://example.com"));
        assertThrows(IllegalArgumentException.class, () -> builder().baseUrl("https://example.com/?q=1"));
    }

    @Test
    void cleaningIsEnabledByDefaultAndConfigurable() {
        SsgSite cleaned = builder()
                .route(SsgRoute.page("/", "en", meta -> null))
                .build();
        SsgSite kept = builder()
                .clean(false)
                .route(SsgRoute.page("/", "en", meta -> null))
                .build();

        assertTrue(cleaned.clean());
        assertFalse(kept.clean());
    }

    @Test
    void routesAndAssetsKeepDeclarationOrder() {
        SsgRoute home = SsgRoute.page("/", "en", meta -> null);
        SsgRoute about = SsgRoute.page("/about", "en", meta -> null);

        SsgSite site = builder()
                .route(home)
                .route(about)
                .asset(Path.of("one.css"), "one.css")
                .asset(Path.of("two.css"), "two.css")
                .build();

        assertEquals(java.util.List.of(home, about), site.routes());
        assertEquals(java.util.List.of("one.css", "two.css"),
                site.assets().stream().map(SsgAsset::target).toList());
    }
}
