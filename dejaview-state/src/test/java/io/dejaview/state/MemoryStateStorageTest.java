package io.dejaview.state;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryStateStorageTest {

    @Test
    void readWriteRemoveSemantics() {
        MemoryStateStorage storage = new MemoryStateStorage();

        assertEquals(Optional.empty(), storage.read("missing"));

        storage.write("key", "value");
        assertEquals(Optional.of("value"), storage.read("key"));
        assertEquals(1, storage.size());

        storage.write("key", "updated");
        assertEquals(Optional.of("updated"), storage.read("key"));
        assertEquals(1, storage.size());

        storage.remove("key");
        assertEquals(Optional.empty(), storage.read("key"));
        assertEquals(0, storage.size());

        storage.remove("never-existed"); // not an error
    }

    @Test
    void integerCodecRoundTripsAndRejectsGarbage() {
        StateCodec<Integer> codec = StateCodec.integer();

        assertEquals(42, codec.decode("42"));
        assertEquals("42", codec.encode(42));
        assertEquals("0", codec.encode(0));
        assertEquals("-7", codec.encode(-7));

        assertThrows(StateCodecException.class, () -> codec.decode("not-a-number"));
        assertThrows(StateCodecException.class, () -> codec.decode(null));
    }

    @Test
    void stringCodecIsIdentity() {
        StateCodec<String> codec = StateCodec.string();
        assertEquals("anything", codec.decode("anything"));
        assertEquals("anything", codec.encode("anything"));
        assertTrue(true);
    }

    @Test
    void asyncErrorRejectsBlankCodes() {
        assertThrows(IllegalArgumentException.class, () -> AsyncError.of("  ", "message"));
        assertThrows(NullPointerException.class, () -> AsyncError.of("code", null));
        assertEquals("code", AsyncError.of("code", "message").code());
    }
}
