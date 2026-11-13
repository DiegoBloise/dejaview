package io.dejaview.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @TempDir
    Path tempDir;

    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void saveStreams() {
        originalOut = System.out;
        originalErr = System.err;
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private Capture capture() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Capture(out, err);
    }

    private record Capture(ByteArrayOutputStream out, ByteArrayOutputStream err) {
        String outText() {
            return out.toString(StandardCharsets.UTF_8);
        }

        String errText() {
            return err.toString(StandardCharsets.UTF_8);
        }
    }

    @Test
    void with_no_arguments_prints_usage_and_exits_two() {
        Capture capture = capture();
        assertEquals(2, Main.execute(new String[]{}, tempDir));
        assertTrue(capture.outText().contains("Usage:"));
    }

    @Test
    void help_prints_usage_and_exits_zero() {
        Capture capture = capture();
        assertEquals(0, Main.execute(new String[]{"help"}, tempDir));
        assertTrue(capture.outText().contains("dejaview new"));
    }

    @Test
    void version_prints_the_cli_version() {
        Capture capture = capture();
        assertEquals(0, Main.execute(new String[]{"--version"}, tempDir));
        assertTrue(capture.outText().contains("dejaview "));
    }

    @Test
    void unknown_command_exits_two() {
        Capture capture = capture();
        assertEquals(2, Main.execute(new String[]{"frobnicate"}, tempDir));
        assertTrue(capture.errText().contains("Unknown command: frobnicate"));
    }

    @Test
    void new_without_a_name_fails_with_a_usage_hint() {
        Capture capture = capture();
        assertEquals(1, Main.execute(new String[]{"new"}, tempDir));
        assertTrue(capture.errText().contains("Missing project name"));
    }

    @Test
    void new_generates_a_project_that_doctor_accepts() {
        Capture capture = capture();
        Path target = tempDir.resolve("doctor-app");

        assertEquals(0, Main.execute(new String[]{"new", "doctor-app", "--directory", target.toString()}, tempDir));
        assertTrue(Files.isDirectory(target));

        assertEquals(0, Main.execute(new String[]{"doctor", "--no-port-check"}, target));
        assertTrue(capture.outText().contains("PASS"));
    }

    @Test
    void doctor_fails_in_an_empty_directory() {
        Capture capture = capture();
        assertEquals(1, Main.execute(new String[]{"doctor", "--no-port-check"}, tempDir));
        assertTrue(capture.outText().contains("FAIL"));
        assertTrue(capture.outText().contains("passed"));
    }
}
