package io.dejaview.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ViewKeyTest {

    @Test
    void viewsAreUnkeyedByDefault() {
        assertNull(Views.text("x").key());
        assertNull(Views.fragment().key());
        assertNull(Views.div().key());
        assertNull(Views.componentFactory(CounterProbe::new).key());
    }

    @Test
    void keyBuildersCarryTheKey() {
        assertEquals("t", Views.text("x").key("t").key());
        assertEquals("f", Views.fragment().key("f").key());
        assertEquals("e", Views.div().key("e").key());
        assertEquals("c", Views.componentFactory(CounterProbe::new).key("c").key());
    }

    @Test
    void keyBuildersPreserveTheContent() {
        ElementView keyed = Views.button("Go").id("go").key("btn");

        assertEquals("btn", keyed.key());
        assertEquals("go", keyed.attributes().get(0).value());
        assertEquals("Go", ((TextView) keyed.children().get(0)).value());
    }

    @Test
    void blankKeysAreRejectedEverywhere() {
        assertThrows(IllegalArgumentException.class, () -> Views.text("x").key(" "));
        assertThrows(IllegalArgumentException.class, () -> Views.fragment().key(" "));
        assertThrows(IllegalArgumentException.class, () -> Views.div().key(" "));
        assertThrows(IllegalArgumentException.class,
                () -> Views.componentFactory(CounterProbe::new).key(" "));
    }

    private static final class CounterProbe extends AbstractComponent {

        @Override
        public View render() {
            return Views.div();
        }
    }
}
