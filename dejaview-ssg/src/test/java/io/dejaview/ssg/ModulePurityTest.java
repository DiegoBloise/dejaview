package io.dejaview.ssg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Guards the module boundary: {@code dejaview-ssg} is a build-time JVM module
 * and must never gain a transitive dependency on the browser adapter or on
 * TeaVM. Static generation exists to produce HTML without a browser.
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
