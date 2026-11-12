package io.dejaview.dom;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class BrowserEnvironmentTest {

    @Test
    void reportsNoBrowserOnTheJvm() {
        assertFalse(BrowserEnvironment.isBrowser());
    }
}
