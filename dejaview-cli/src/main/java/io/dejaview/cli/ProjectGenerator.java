package io.dejaview.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Renders the bundled {@code basic-app} template into a new project directory.
 * The set of template files is declared in {@code templates/basic-app/files.txt}
 * because classpath directories cannot be listed reliably from a jar.
 */
public final class ProjectGenerator {

    private static final String TEMPLATE_ROOT = "templates/basic-app";
    private static final String INDEX = TEMPLATE_ROOT + "/files.txt";
    private static final String ARTICLE_REGEX = "[^a-z0-9.-]+";

    private ProjectGenerator() {
    }

    public record NewProject(String name, String artifactId, String groupId,
                             String packageName, Path directory) {
    }

    public static String sanitizeArtifactId(String name) {
        String cleaned = name.trim().toLowerCase().replaceAll(ARTICLE_REGEX, "-");
        cleaned = cleaned.replaceAll("^-+|-+$", "").replaceAll("-{2,}", "-");
        if (cleaned.isBlank()) {
            cleaned = "app";
        }
        if (!Character.isLetterOrDigit(cleaned.charAt(0))) {
            cleaned = "app-" + cleaned;
        }
        return cleaned;
    }

    public static String defaultPackage(String artifactId) {
        String compact = artifactId.replaceAll("[^a-z0-9]", "");
        if (compact.isBlank() || !Character.isLetter(compact.charAt(0))) {
            compact = "app" + compact;
        }
        return "com.example." + compact;
    }

    public static String validatePackage(String packageName) throws CliException {
        String normalized = packageName.trim().toLowerCase();
        if (!normalized.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*")) {
            throw new CliException("Invalid package name \"" + packageName
                    + "\". Use dot-separated lower-case identifiers, e.g. com.example.myapp.");
        }
        return normalized;
    }

    /** Generates the project. Refuses to write into a non-empty directory. */
    public static NewProject generate(Context context, String name, String packageName,
                                      String groupId, Path directory) throws CliException {
        if (name == null || !name.trim().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")) {
            throw new CliException("Invalid project name \"" + name
                    + "\". Use 1-64 characters: letters, digits, '-', '_' or '.', starting with a letter or digit.");
        }
        String artifactId = sanitizeArtifactId(name);
        String resolvedPackage = validatePackage(packageName != null ? packageName : defaultPackage(artifactId));
        String resolvedGroupId = (groupId == null || groupId.isBlank()) ? resolvedPackage : groupId.trim();
        Path target = directory != null
                ? directory.toAbsolutePath().normalize()
                : context.cwd().resolve(name.trim()).toAbsolutePath().normalize();

        if (Files.exists(target)) {
            try (var stream = Files.list(target)) {
                if (stream.findAny().isPresent()) {
                    throw new CliException("Directory " + target + " is not empty. Choose another name or remove it first.");
                }
            } catch (IOException e) {
                throw new CliException("Could not inspect directory " + target + ": " + e.getMessage());
            }
        }

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("__NAME__", name.trim());
        placeholders.put("__ARTIFACT_ID__", artifactId);
        placeholders.put("__GROUP_ID__", resolvedGroupId);
        placeholders.put("__PACKAGE__", resolvedPackage);
        placeholders.put("__PACKAGE_PATH__", resolvedPackage.replace('.', '/'));
        placeholders.put("__DEJAVIEW_VERSION__", Version.current());
        placeholders.put("__TEAVM_VERSION__", "0.16.0");

        List<String> files = readIndex();
        try {
            for (String entry : files) {
                String renderedEntry = substitute(entry, placeholders);
                // Maven strips dotfiles from jars; gitignore is shipped without the dot.
                if (renderedEntry.equals("gitignore")) {
                    renderedEntry = ".gitignore";
                }
                Path destination = target.resolve(renderedEntry).normalize();
                if (!destination.startsWith(target)) {
                    throw new CliException("Template entry escapes the target directory: " + entry);
                }
                byte[] content = readTemplate(TEMPLATE_ROOT + "/" + entry);
                content = substitute(content, placeholders);
                Files.createDirectories(destination.getParent());
                Files.write(destination, content);
                if (entry.equals("mvnw") || entry.endsWith("/mvnw")) {
                    makeExecutable(destination);
                }
            }
        } catch (IOException e) {
            throw new CliException("Could not write the project into " + target + ": " + e.getMessage());
        }
        return new NewProject(name.trim(), artifactId, resolvedGroupId, resolvedPackage, target);
    }

    private static List<String> readIndex() throws CliException {
        try (InputStream in = ProjectGenerator.class.getResourceAsStream("/" + INDEX)) {
            if (in == null) {
                throw new CliException("Template index missing from the CLI jar: " + INDEX);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new CliException("Could not read the template index: " + e.getMessage());
        }
    }

    private static byte[] readTemplate(String resource) throws CliException {
        try (InputStream in = ProjectGenerator.class.getResourceAsStream("/" + resource)) {
            if (in == null) {
                throw new CliException("Template file missing from the CLI jar: " + resource);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new CliException("Could not read template file " + resource + ": " + e.getMessage());
        }
    }

    private static String substitute(String text, Map<String, String> placeholders) {
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static byte[] substitute(byte[] content, Map<String, String> placeholders) {
        // Placeholders are ASCII; byte-wise replacement is safe for UTF-8 content.
        String text = new String(content, StandardCharsets.UTF_8);
        String substituted = substitute(text, placeholders);
        return substituted.getBytes(StandardCharsets.UTF_8);
    }

    private static void makeExecutable(Path file) {
        try {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x");
            Files.setPosixFilePermissions(file, perms);
        } catch (UnsupportedOperationException | IOException e) {
            // Non-POSIX file system (or restricted): permissions are best effort.
            file.toFile().setExecutable(true, false);
        }
    }
}
