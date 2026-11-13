package io.dejaview.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Extracts the actionable parts of a failed Maven build: the {@code [ERROR]}
 * lines with their location hints, or a bounded tail when no error lines are
 * present; plus known-cause hints mapping common TeaVM/Maven failure
 * signatures to likely fixes. Pure string processing so it can be
 * unit-tested without a build.
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

    /** Returns actionable hints for a single output line; empty when no known failure signature matches. */
    public static List<String> hintsFor(String line) {
        if (line == null || line.isBlank()) {
            return List.of();
        }
        String lower = line.toLowerCase(Locale.ROOT);
        List<String> hints = new ArrayList<>();

        if (lower.contains("unsupported class file major version")) {
            hints.add("The JDK class file version is too new for the compiler. "
                    + "DejaView builds with JDK 25 and TeaVM 0.16.0 — check java -version "
                    + "and the teavm.version property (docs/COMPILER.md).");
        }
        if (lower.contains("jsclassprocessor") || lower.contains("getobjectclass()")) {
            hints.add("TeaVM needs teavm-classlib on the project class path. Declare it with "
                    + "provided scope in the browser module (docs/COMPILER.md, section 4).");
        }
        if (lower.contains("can't find class") || lower.contains("cannot find class")
                || lower.contains("cannot find symbol") || lower.contains("unresolved reference")) {
            hints.add("A referenced class or symbol is missing. Check that the dependency is on "
                    + "the compile classpath and that the API exists in TeaVM's class library "
                    + "(docs/COMPILER.md, section 6).");
        }
        if (lower.contains("strict")) {
            hints.add("TeaVM strict mode is enabled: missing classes fail the build instead of "
                    + "surfacing later in the browser. Add the missing dependency, or see "
                    + "docs/COMPILER.md section 6 for unsupported APIs.");
        }
        return List.copyOf(hints);
    }

    /** Collects the hints for a whole output; de-duplicated, order of first appearance. */
    public static List<String> hintsForOutput(List<String> outputLines) {
        List<String> hints = new ArrayList<>();
        for (String line : outputLines) {
            for (String hint : hintsFor(line)) {
                if (!hints.contains(hint)) {
                    hints.add(hint);
                }
            }
        }
        return List.copyOf(hints);
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
