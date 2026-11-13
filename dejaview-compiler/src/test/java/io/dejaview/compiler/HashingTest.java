package io.dejaview.compiler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashingTest {

    @Test
    void sha256_of_known_vector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                Hashing.sha256Hex("abc".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @TempDir
    Path tempDir;

    @Test
    void sha256_of_file() throws Exception {
        Path file = tempDir.resolve("data.bin");
        Files.writeString(file, "dejaview");
        String expected = Hashing.sha256Hex(Files.readAllBytes(file));
        assertEquals(expected, Hashing.sha256Hex(file));
        assertTrue(expected.matches("[0-9a-f]{64}"));
    }
}
