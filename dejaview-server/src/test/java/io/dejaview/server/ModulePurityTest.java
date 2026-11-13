package io.dejaview.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Guards the module boundary: {@code dejaview-server} must never gain a
 * transitive dependency on the browser adapter or on TeaVM. The module's whole
 * reason to exist is JVM-side rendering with no browser APIs; a leaked import
 * would defeat it silently, because compilation would still succeed.
 */
class ModulePurityTest {

    @Test
    void browserDomModuleIsNotOnTheClasspath() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("io.dejaview.dom.DejaView"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("io.dejaview.dom.DomRenderer"));
    }

    @Test
    void teavmIsNotOnTheClasspath() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("org.teavm.jso.JSBody"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("org.teavm.jso.dom.html.HTMLDocument"));
    }
}
