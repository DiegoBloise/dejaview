package io.dejaview.cli;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildDiagnosticsTest {

    @Test
    void extracts_real_errors_and_ignores_maven_noise() {
        List<String> output = List.of(
                "[INFO] Building my-app 0.1.0-SNAPSHOT",
                "[ERROR] /src/main/java/com/example/App.java:[15,42] cannot find symbol",
                "[ERROR]   symbol:   variable coutn",
                "[ERROR] BUILD FAILURE",
                "[ERROR] -> [Help 1]",
                "[ERROR] To see the full stack trace, the errors and warnings were dumped.",
                "[ERROR] Re-run Maven using the -X switch to enable full debug logging.");

        List<String> diagnostics = BuildDiagnostics.diagnose(output);

        assertTrue(diagnostics.stream().anyMatch(line -> line.contains("cannot find symbol")));
        assertTrue(diagnostics.stream().anyMatch(line -> line.contains("variable coutn")));
        assertTrue(diagnostics.stream().noneMatch(line -> line.contains("BUILD FAILURE")));
        assertTrue(diagnostics.stream().noneMatch(line -> line.contains("Help 1")));
        assertTrue(diagnostics.stream().noneMatch(line -> line.contains("stack trace")));
    }

    @Test
    void caps_the_number_of_errors_reported() {
        List<String> output = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> "[ERROR] error number " + i)
                .toList();

        List<String> diagnostics = BuildDiagnostics.diagnose(output);

        assertEquals(21, diagnostics.size());
        assertTrue(diagnostics.get(diagnostics.size() - 1).contains("more error(s)"));
    }

    @Test
    void falls_back_to_the_tail_when_there_are_no_error_lines() {
        List<String> output = java.util.stream.IntStream.range(0, 80)
                .mapToObj(i -> "some log line " + i)
                .toList();

        List<String> diagnostics = BuildDiagnostics.diagnose(output);

        assertEquals(25, diagnostics.size());
        assertTrue(diagnostics.get(0).contains("some log line 55"));
    }
}
