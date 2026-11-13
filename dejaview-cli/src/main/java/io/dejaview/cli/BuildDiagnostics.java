package io.dejaview.cli;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts the actionable parts of a failed Maven build: the {@code [ERROR]}
 * lines with their location hints, or a bounded tail when no error lines are
 * present. Pure string processing so it can be unit-tested without a build.
 */
public final class BuildDiagnostics {

    private static final int MAX_ERRORS = 20;
    private static final int TAIL_LINES = 25;

    private BuildDiagnostics() {
    }

    /** Returns human-readable diagnostic lines, one per list element. */
    public static List<String> diagnose(List<String> outputTail) {
        List<String> errors = new ArrayList<>();
        for (String line : outputTail) {
            String trimmed = line.trim();
            if (trimmed.contains("[ERROR]")) {
                String cleaned = cleanup(trimmed);
                if (!cleaned.isBlank() && !isNoise(cleaned) && !errors.contains(cleaned)) {
                    errors.add(cleaned);
                }
            }
        }
        if (!errors.isEmpty()) {
            if (errors.size() > MAX_ERRORS) {
                int total = errors.size();
                List<String> truncated = new ArrayList<>(errors.subList(0, MAX_ERRORS));
                truncated.add("... and " + (total - MAX_ERRORS) + " more error(s).");
                return truncated;
            }
            return errors;
        }
        int from = Math.max(0, outputTail.size() - TAIL_LINES);
        return new ArrayList<>(outputTail.subList(from, outputTail.size()));
    }

    private static String cleanup(String line) {
        String cleaned = line;
        int idx = cleaned.indexOf("[ERROR]");
        cleaned = cleaned.substring(idx + "[ERROR]".length()).trim();
        // Maven prefixes continuation hints; normalize the common shapes.
        if (cleaned.startsWith("The goal you specified requires a project to execute but there is no POM in this directory")) {
            return "No pom.xml in this directory. Run the command from the project root.";
        }
        return cleaned;
    }

    private static boolean isNoise(String line) {
        // Maven repeats the same failure header at several levels; keep the summary lines only.
        return line.startsWith("BUILD FAILURE")
                || line.startsWith("BUILD ERROR")
                || line.startsWith("-> [Help")
                || line.startsWith("To see the full stack trace")
                || line.startsWith("Re-run Maven using")
                || line.startsWith("Add a <dependencies>")
                || line.startsWith("mvn <args>");
    }
}
