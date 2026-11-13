package io.dejaview.compiler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates an assembled webapp directory: the host page exists, every local
 * reference in it resolves to a real file, the JavaScript bundle is present and
 * non-trivial, and the bundle stays within a size budget. Missing resources are
 * reported with the referencing file and the offending path.
 *
 * <p>The validator is deliberately dependency-free: the site is validated on the
 * build host before it can ship, which is exactly the failure mode it exists to
 * catch. It is not a JavaScript parser; it checks the contract of the build
 * output, not the semantics of the program.</p>
 */
public final class WebAppValidator {

    private static final Pattern REFERENCE =
            Pattern.compile("(?i)\\b(?:src|href)\\s*=\\s*[\"']([^\"']+)[\"']");

    private WebAppValidator() {
    }

    /** Validation against the DejaView defaults: {@code app.js} bundle, 512 KiB budget, source map expected. */
    public static ValidationReport validate(Path webDir) {
        return validate(webDir, ValidatorOptions.defaults());
    }

    public static ValidationReport validate(Path webDir, ValidatorOptions options) {
        List<ValidationReport.Finding> findings = new ArrayList<>();
        Path root = webDir.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            findings.add(error("webapp directory does not exist: " + root
                    + " — run the build before validating (./mvnw package)"));
            return new ValidationReport(List.copyOf(findings));
        }

        Path index = root.resolve("index.html");
        if (!Files.isRegularFile(index)) {
            if (options.requireIndex()) {
                findings.add(error("index.html is missing from " + root
                        + " — src/main/webapp should contain the host page"));
            }
        } else {
            validateReferences(root, index, findings);
        }

        Path mainScript = root.resolve(options.mainScript());
        if (!Files.isRegularFile(mainScript)) {
            findings.add(error(options.mainScript() + " is missing from " + root
                    + " — TeaVM did not produce the JavaScript bundle (check the teavm-maven-plugin execution)"));
        } else if (sizeQuiet(mainScript) == 0) {
            findings.add(error(options.mainScript() + " is empty — TeaVM produced no output; "
                    + "check strict mode and the configured main class"));
        } else if (options.sourceMapsExpected()
                && !Files.isRegularFile(root.resolve(options.mainScript() + ".map"))) {
            findings.add(warning("no source map found next to " + options.mainScript()
                    + " — enable sourceMapsGenerated for debugging support (docs/COMPILER.md)"));
        }

        long total = totalBytesQuiet(root);
        if (options.sizeBudgetBytes() > 0 && total > options.sizeBudgetBytes()) {
            findings.add(warning("webapp is " + total + " bytes (budget is " + options.sizeBudgetBytes()
                    + ") — enable the production profile for minification and dead-code elimination"));
        }

        return new ValidationReport(List.copyOf(findings));
    }

    private static void validateReferences(Path root, Path index, List<ValidationReport.Finding> findings) {
        String html;
        try {
            html = Files.readString(index, StandardCharsets.UTF_8);
        } catch (IOException e) {
            findings.add(error("could not read " + index + ": " + e.getMessage()));
            return;
        }

        if (html.contains("file:///")) {
            findings.add(warning("index.html contains an absolute file:// reference — "
                    + "the deployed site cannot resolve it"));
        }

        List<String> references = localReferences(html);
        if (references.isEmpty()) {
            findings.add(warning("index.html references no local asset — the TeaVM bundle should be loaded with <script src=\"app.js\">"));
        }
        for (String reference : references) {
            if (reference.startsWith("/")) {
                findings.add(warning("index.html reference is root-relative and cannot be validated locally: " + reference));
                continue;
            }
            Path target = root.resolve(stripQueryAndFragment(reference)).normalize();
            if (!target.startsWith(root)) {
                findings.add(error("index.html reference escapes the web root: " + reference));
                continue;
            }
            if (!Files.exists(target)) {
                findings.add(error("index.html references missing asset: " + reference
                        + " (expected at " + target + ")"));
            }
        }
    }

    /** Extracts the local references of an HTML document (src/href attributes), ordered and de-duplicated. */
    static List<String> localReferences(String html) {
        List<String> references = new ArrayList<>();
        Matcher matcher = REFERENCE.matcher(html);
        while (matcher.find()) {
            String reference = matcher.group(1).trim();
            String lower = reference.toLowerCase(Locale.ROOT);
            if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("//")
                    || lower.startsWith("data:") || lower.startsWith("mailto:") || lower.startsWith("tel:")
                    || lower.startsWith("javascript:") || lower.startsWith("#")) {
                continue;
            }
            if (reference.isEmpty() || references.contains(reference)) {
                continue;
            }
            references.add(reference);
        }
        return references;
    }

    static String stripQueryAndFragment(String reference) {
        int cut = reference.length();
        int query = reference.indexOf('?');
        int fragment = reference.indexOf('#');
        if (query >= 0) {
            cut = Math.min(cut, query);
        }
        if (fragment >= 0) {
            cut = Math.min(cut, fragment);
        }
        return reference.substring(0, cut);
    }

    private static long sizeQuiet(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }

    private static long totalBytesQuiet(Path root) {
        try (var walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .mapToLong(WebAppValidator::sizeQuiet)
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }

    private static ValidationReport.Finding error(String message) {
        return new ValidationReport.Finding(ValidationReport.Severity.ERROR, message);
    }

    private static ValidationReport.Finding warning(String message) {
        return new ValidationReport.Finding(ValidationReport.Severity.WARNING, message);
    }

    /** Options for a validation run. {@code requireIndex} is false for bundles consumed by external host pages (e.g. the website). */
    public record ValidatorOptions(String mainScript, long sizeBudgetBytes, boolean sourceMapsExpected,
                                   boolean requireIndex) {

        public ValidatorOptions(String mainScript, long sizeBudgetBytes, boolean sourceMapsExpected) {
            this(mainScript, sizeBudgetBytes, sourceMapsExpected, true);
        }

        public static ValidatorOptions defaults() {
            return new ValidatorOptions("app.js", 1024L * 1024, true);
        }
    }
}
