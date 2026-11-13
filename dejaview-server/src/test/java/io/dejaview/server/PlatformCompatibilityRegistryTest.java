package io.dejaview.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlatformCompatibilityRegistryTest {

    @Test
    void defaultsToUniversal() {
        PlatformCompatibilityRegistry registry = new PlatformCompatibilityRegistry();

        assertEquals(PlatformCompatibility.UNIVERSAL, registry.classify(String.class));
        assertEquals(PlatformCompatibility.UNIVERSAL, registry.classify(getClass()));
    }

    @Test
    void markerInterfaceMakesAComponentBrowserOnly() {
        PlatformCompatibilityRegistry registry = new PlatformCompatibilityRegistry();

        assertEquals(PlatformCompatibility.BROWSER_ONLY, registry.classify(MarkerComponent.class));
        assertEquals(PlatformCompatibility.BROWSER_ONLY, registry.classify(SubMarkerComponent.class));
    }

    @Test
    void exactClassOverrideWinsOverMarkerInterface() {
        PlatformCompatibilityRegistry registry = new PlatformCompatibilityRegistry();
        registry.register(MarkerComponent.class, PlatformCompatibility.UNIVERSAL);

        assertEquals(PlatformCompatibility.UNIVERSAL, registry.classify(MarkerComponent.class));
        // Subclasses are not covered by an exact-class override.
        assertEquals(PlatformCompatibility.BROWSER_ONLY, registry.classify(SubMarkerComponent.class));
    }

    @Test
    void overrideCanMarkAThirdPartyClassBrowserOnly() {
        PlatformCompatibilityRegistry registry = new PlatformCompatibilityRegistry();
        registry.register(String.class, PlatformCompatibility.BROWSER_ONLY);

        assertEquals(PlatformCompatibility.BROWSER_ONLY, registry.classify(String.class));
    }

    @Test
    void overrideCanMarkAComponentServerOnly() {
        PlatformCompatibilityRegistry registry = new PlatformCompatibilityRegistry();
        registry.register(MarkerComponent.class, PlatformCompatibility.SERVER_ONLY);

        assertEquals(PlatformCompatibility.SERVER_ONLY, registry.classify(MarkerComponent.class));
    }

    @Test
    void clearRemovesOverrides() {
        PlatformCompatibilityRegistry registry = new PlatformCompatibilityRegistry();
        registry.register(String.class, PlatformCompatibility.SERVER_ONLY);

        registry.clear();

        assertEquals(0, registry.size());
        assertEquals(PlatformCompatibility.UNIVERSAL, registry.classify(String.class));
    }

    @Test
    void rejectsNullArguments() {
        PlatformCompatibilityRegistry registry = new PlatformCompatibilityRegistry();

        assertThrows(NullPointerException.class, () -> registry.classify(null));
        assertThrows(NullPointerException.class, () -> registry.register(null, PlatformCompatibility.UNIVERSAL));
        assertThrows(NullPointerException.class, () -> registry.register(String.class, null));
    }

    @Test
    void registrationsAreReportedBySize() {
        PlatformCompatibilityRegistry registry = new PlatformCompatibilityRegistry();

        registry.register(String.class, PlatformCompatibility.SERVER_ONLY);
        registry.register(StringBuilder.class, PlatformCompatibility.BROWSER_ONLY);

        assertEquals(2, registry.size());
    }

    private static class MarkerComponent implements BrowserOnlyComponent {
    }

    private static final class SubMarkerComponent extends MarkerComponent {
    }
}
