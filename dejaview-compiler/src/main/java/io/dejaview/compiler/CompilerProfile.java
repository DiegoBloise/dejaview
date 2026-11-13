package io.dejaview.compiler;

/**
 * The two build profiles the production pipeline supports. Mirrors the Maven
 * {@code production} profile in the parent POM — the POM properties remain the
 * source of truth when the plugin runs; this enum exists so the build tooling
 * (and its tests) can reason about profiles without parsing Maven models.
 *
 * <p>Keep the values here in sync with {@code dejaview.compiler.*}:
 * development — {@code SIMPLE}, unminified; production — {@code FULL}
 * (dead-code elimination), minified. Source maps stay on in both: they cost
 * nothing at runtime and are required for field diagnostics.</p>
 */
public enum CompilerProfile {

    DEVELOPMENT("development", "SIMPLE", false, true),
    PRODUCTION("production", "FULL", true, true);

    private final String id;
    private final String optimizationLevel;
    private final boolean minified;
    private final boolean sourceMaps;

    CompilerProfile(String id, String optimizationLevel, boolean minified, boolean sourceMaps) {
        this.id = id;
        this.optimizationLevel = optimizationLevel;
        this.minified = minified;
        this.sourceMaps = sourceMaps;
    }

    /** The profile identifier used by the Maven property {@code dejaview.compiler.profile}. */
    public String id() {
        return id;
    }

    /** TeaVM optimization level name ({@code SIMPLE}, {@code ADVANCED} or {@code FULL}). */
    public String optimizationLevel() {
        return optimizationLevel;
    }

    /** Whether the emitted JavaScript should be minified. */
    public boolean minified() {
        return minified;
    }

    /** Whether the emitted JavaScript should carry a source map. */
    public boolean sourceMaps() {
        return sourceMaps;
    }

    /** Case-insensitive lookup by profile id; defaults to {@link #DEVELOPMENT} for unknown values. */
    public static CompilerProfile of(String name) {
        if (name == null) {
            return DEVELOPMENT;
        }
        String normalized = name.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.equals(PRODUCTION.id)) {
            return PRODUCTION;
        }
        return DEVELOPMENT;
    }
}
