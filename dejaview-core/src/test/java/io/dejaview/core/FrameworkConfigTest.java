package io.dejaview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FrameworkConfigTest {

    @Test
    void defaultsAreUsable() {
        FrameworkConfig config = FrameworkConfig.defaults();

        assertEquals("DejaView", config.applicationName());
        assertFalse(config.development());
        assertTrue(config.properties().isEmpty());
    }

    @Test
    void builderCollectsSettingsAndProperties() {
        FrameworkConfig config = FrameworkConfig.builder()
                .applicationName("Counter")
                .development(true)
                .property("locale", "pt-BR")
                .build();

        assertEquals("Counter", config.applicationName());
        assertTrue(config.development());
        assertEquals("pt-BR", config.property("locale").orElseThrow());
        assertTrue(config.property("missing").isEmpty());
    }

    @Test
    void copiesPropertiesDefensively() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("mode", "test");

        FrameworkConfig config = new FrameworkConfig("App", false, properties);
        properties.clear();

        assertEquals(Map.of("mode", "test"), config.properties());
    }

    @Test
    void rejectsInvalidInputs() {
        assertThrows(NullPointerException.class, () -> new FrameworkConfig(null, false, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new FrameworkConfig(" ", false, Map.of()));
        assertThrows(NullPointerException.class, () -> new FrameworkConfig("App", false, null));
        assertThrows(IllegalArgumentException.class,
                () -> FrameworkConfig.builder().property(" ", "value"));
        assertThrows(NullPointerException.class,
                () -> FrameworkConfig.builder().property("name", null));
    }
}
