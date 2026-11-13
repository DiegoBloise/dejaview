package io.dejaview.server;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Per-context classification of components by platform compatibility.
 *
 * <p>Resolution order, highest precedence first:</p>
 * <ol>
 *   <li>An explicit registration made with {@link #register(Class, PlatformCompatibility)}
 *       for the exact class.</li>
 *   <li>The {@link BrowserOnlyComponent} marker interface, applied by
 *       assignability — a subclass of a browser-only component is browser-only.</li>
 *   <li>{@link PlatformCompatibility#UNIVERSAL}, the default.</li>
 * </ol>
 *
 * <p>The registry is deliberately an instance, not global state: two requests
 * served by the same process may legitimately classify the same component
 * differently, and tests must not leak classifications between each other.
 * Every {@link ServerContext} owns one.</p>
 */
public final class PlatformCompatibilityRegistry {

    private final Map<Class<?>, PlatformCompatibility> overrides = new LinkedHashMap<>();

    /**
     * Registers an explicit compatibility for {@code type}, replacing any
     * previous registration for the same class.
     */
    public PlatformCompatibilityRegistry register(Class<?> type, PlatformCompatibility compatibility) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(compatibility, "compatibility");
        overrides.put(type, compatibility);
        return this;
    }

    /**
     * Returns the compatibility for {@code type} following the resolution
     * order described in the class documentation.
     */
    public PlatformCompatibility classify(Class<?> type) {
        Objects.requireNonNull(type, "type");
        PlatformCompatibility override = overrides.get(type);
        if (override != null) {
            return override;
        }
        if (BrowserOnlyComponent.class.isAssignableFrom(type)) {
            return PlatformCompatibility.BROWSER_ONLY;
        }
        return PlatformCompatibility.UNIVERSAL;
    }

    /**
     * Returns the number of explicit registrations.
     */
    public int size() {
        return overrides.size();
    }

    /**
     * Removes every explicit registration. Marker-based classification is not
     * affected.
     */
    public void clear() {
        overrides.clear();
    }
}
