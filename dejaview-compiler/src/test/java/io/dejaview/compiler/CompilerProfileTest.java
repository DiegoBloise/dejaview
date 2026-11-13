package io.dejaview.compiler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompilerProfileTest {

    @Test
    void development_and_production_profiles() {
        assertEquals("development", CompilerProfile.DEVELOPMENT.id());
        assertEquals("SIMPLE", CompilerProfile.DEVELOPMENT.optimizationLevel());
        assertEquals(false, CompilerProfile.DEVELOPMENT.minified());
        assertEquals(true, CompilerProfile.DEVELOPMENT.sourceMaps());

        assertEquals("production", CompilerProfile.PRODUCTION.id());
        assertEquals("FULL", CompilerProfile.PRODUCTION.optimizationLevel());
        assertEquals(true, CompilerProfile.PRODUCTION.minified());
        assertEquals(true, CompilerProfile.PRODUCTION.sourceMaps());
    }

    @Test
    void of_resolves_ids_case_insensitively() {
        assertEquals(CompilerProfile.PRODUCTION, CompilerProfile.of("production"));
        assertEquals(CompilerProfile.PRODUCTION, CompilerProfile.of("PRODUCTION"));
        assertEquals(CompilerProfile.DEVELOPMENT, CompilerProfile.of("development"));
        assertEquals(CompilerProfile.DEVELOPMENT, CompilerProfile.of("anything-else"));
        assertEquals(CompilerProfile.DEVELOPMENT, CompilerProfile.of(null));
    }
}
