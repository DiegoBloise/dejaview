package io.dejaview.dom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RuntimeConfigTest {

    @Test
    void defaultsUseTheAppContainer() {
        RuntimeConfig config = RuntimeConfig.defaults();

        assertEquals("app", config.containerId());
        assertFalse(config.debug());
    }

    @Test
    void builderOverridesDefaults() {
        RuntimeConfig config = RuntimeConfig.builder()
                .containerId("root")
                .debug(true)
                .build();

        assertEquals("root", config.containerId());
        assertTrue(config.debug());
    }

    @Test
    void rejectsBlankContainerId() {
        assertThrows(IllegalArgumentException.class, () -> new RuntimeConfig(" ", false));
    }

    @Test
    void rejectsNullContainerId() {
        assertThrows(NullPointerException.class, () -> new RuntimeConfig(null, false));
    }
}
