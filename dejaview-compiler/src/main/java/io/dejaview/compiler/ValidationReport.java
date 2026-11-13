package io.dejaview.compiler;

import java.util.List;

/**
 * Result of validating an assembled webapp directory. Errors fail the build;
 * warnings are reported but tolerated (for example, a missing source map on a
 * minified production bundle).
 */
public record ValidationReport(List<Finding> findings) {

    public enum Severity {
        ERROR, WARNING
    }

    public record Finding(Severity severity, String message) {
    }

    public boolean hasErrors() {
        return findings.stream().anyMatch(f -> f.severity() == Severity.ERROR);
    }

    public long errorCount() {
        return findings.stream().filter(f -> f.severity() == Severity.ERROR).count();
    }

    public long warningCount() {
        return findings.stream().filter(f -> f.severity() == Severity.WARNING).count();
    }

    /** Human-readable, stable rendering — one finding per line. */
    public String render() {
        StringBuilder text = new StringBuilder();
        text.append("DejaView webapp validation: ")
                .append(errorCount()).append(" error(s), ")
                .append(warningCount()).append(" warning(s)\n");
        for (Finding finding : findings) {
            text.append("  ")
                    .append(finding.severity() == Severity.ERROR ? "ERROR" : "WARN ")
                    .append("  ")
                    .append(finding.message())
                    .append('\n');
        }
        return text.toString();
    }
}
