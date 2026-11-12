package io.dejaview.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InjectionTokenTest {

    @Test
    void namedTokensAreDistinctEvenWithTheSameName() {
        InjectionToken<String> first = InjectionToken.of("database");
        InjectionToken<String> second = InjectionToken.of("database");

        assertNotEquals(first, second);
        assertEquals(first.name(), second.name());
    }

    @Test
    void classTokensAreCachedPerClass() {
        assertSame(InjectionToken.of(String.class), InjectionToken.of(String.class));
        assertSame(InjectionToken.of(Integer.class), InjectionToken.of(Integer.class));
        assertNotEquals(InjectionToken.of(String.class), InjectionToken.of(Integer.class));
    }

    @Test
    void classTokenNameForDiagnostics() {
        InjectionToken<String> token = InjectionToken.of(String.class);

        assertEquals(String.class.getName(), token.name());
    }

    @Test
    void rejectsBlankNames() {
        assertThrows(IllegalArgumentException.class, () -> new InjectionToken<>(""));
        assertThrows(IllegalArgumentException.class, () -> new InjectionToken<>("   "));
        assertThrows(NullPointerException.class, () -> new InjectionToken<>(null));
    }

    @Test
    void toStringIncludesTheName() {
        assertTrue(new InjectionToken<>("database").toString().contains("database"));
    }
}
