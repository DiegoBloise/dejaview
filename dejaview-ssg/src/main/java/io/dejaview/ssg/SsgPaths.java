package io.dejaview.ssg;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Path rules shared by routes, assets and the generator.
 *
 * <p>Static generation writes files, so every path is validated before it is
 * turned into a file name: a route may only contain safe segments (letters,
 * digits, {@code .}, {@code _}, {@code ~} and {@code -}); a parameter value may
 * only be one such segment; and no path may escape the output directory through
 * {@code ..} or a separator. Malformed paths fail instead of being repaired.</p>
 */
final class SsgPaths {

    /** A literal route segment. */
    static final Pattern LITERAL_SEGMENT = Pattern.compile("[A-Za-z0-9._~-]+");

    /** A dynamic segment: the whole segment is a single {@code {name}} placeholder. */
    static final Pattern PLACEHOLDER_SEGMENT = Pattern.compile("\\{([A-Za-z][A-Za-z0-9_]*)}");

    private static final Pattern SCHEME = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:.*");

    private SsgPaths() {
    }

    /**
     * Validates a final route path (static or already expanded) and returns it.
     *
     * @throws IllegalArgumentException when the path cannot be written safely
     */
    static String requireRoutePath(String path, String description) {
        Objects.requireNonNull(path, description);
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException(description + " must start with '/', got \"" + path + "\"");
        }
        if (path.length() > 1024) {
            throw new IllegalArgumentException(description + " is longer than 1024 characters");
        }
        if (path.indexOf('?') >= 0 || path.indexOf('#') >= 0 || path.indexOf('\\') >= 0) {
            throw new IllegalArgumentException(
                    description + " must not contain '?', '#' or '\\', got \"" + path + "\"");
        }
        if (path.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(description + " must not contain whitespace, got \"" + path + "\"");
        }
        String body = path.substring(1);
        if (body.isEmpty()) {
            return path;
        }
        String[] segments = body.split("/", -1);
        int limit = segments.length;
        if (limit > 1 && segments[limit - 1].isEmpty() && body.endsWith("/")) {
            limit--;
        }
        for (int i = 0; i < limit; i++) {
            requireStaticSegment(segments[i], description, path);
        }
        return path;
    }

    /**
     * Validates a route pattern with optional whole-segment placeholders and
     * returns its placeholder names in first-seen order.
     */
    static List<String> placeholders(String pattern, String description) {
        Objects.requireNonNull(pattern, description);
        if (!pattern.startsWith("/")) {
            throw new IllegalArgumentException(description + " must start with '/', got \"" + pattern + "\"");
        }
        if (pattern.indexOf('?') >= 0 || pattern.indexOf('#') >= 0 || pattern.indexOf('\\') >= 0) {
            throw new IllegalArgumentException(
                    description + " must not contain '?', '#' or '\\', got \"" + pattern + "\"");
        }
        if (pattern.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(description + " must not contain whitespace, got \"" + pattern + "\"");
        }
        Set<String> names = new LinkedHashSet<>();
        String body = pattern.substring(1);
        if (body.isEmpty()) {
            return List.of();
        }
        String[] segments = body.split("/", -1);
        int limit = segments.length;
        if (limit > 1 && segments[limit - 1].isEmpty() && body.endsWith("/")) {
            limit--;
        }
        for (int i = 0; i < limit; i++) {
            String segment = segments[i];
            if (segment.isEmpty()) {
                throw new IllegalArgumentException(description + " contains an empty segment: \"" + pattern + "\"");
            }
            Matcher placeholder = PLACEHOLDER_SEGMENT.matcher(segment);
            if (placeholder.matches()) {
                String name = placeholder.group(1);
                if (!names.add(name)) {
                    throw new IllegalArgumentException(
                            description + " repeats the parameter {" + name + "}");
                }
                continue;
            }
            if (!LITERAL_SEGMENT.matcher(segment).matches()) {
                throw new IllegalArgumentException(description + " segment \"" + segment
                        + "\" must be literal [A-Za-z0-9._~-]+ or a whole-segment {name} placeholder");
            }
            requireStaticSegment(segment, description, pattern);
        }
        return List.copyOf(names);
    }

    /**
     * Expands {@code pattern} with {@code params}, validating every value as a
     * single safe segment.
     */
    static String expand(String pattern, Map<String, String> params, String description) {
        List<String> names = placeholders(pattern, description);
        Set<String> supplied = new LinkedHashSet<>(Objects.requireNonNull(params, "params").keySet());
        if (!supplied.equals(new LinkedHashSet<>(names))) {
            List<String> missing = new ArrayList<>(names);
            missing.removeAll(supplied);
            List<String> extra = new ArrayList<>(supplied);
            extra.removeAll(names);
            throw new IllegalArgumentException(description + " parameters mismatch for " + pattern
                    + ": missing " + missing + ", unexpected " + extra);
        }
        StringBuilder expanded = new StringBuilder(pattern.length());
        String body = pattern.substring(1);
        expanded.append('/');
        if (!body.isEmpty()) {
            String[] segments = body.split("/", -1);
            for (int i = 0; i < segments.length; i++) {
                if (i > 0) {
                    expanded.append('/');
                }
                Matcher placeholder = PLACEHOLDER_SEGMENT.matcher(segments[i]);
                if (placeholder.matches()) {
                    String name = placeholder.group(1);
                    String value = Objects.requireNonNull(params.get(name), "parameter " + name);
                    requireParameterValue(name, value, description);
                    expanded.append(value);
                } else {
                    expanded.append(segments[i]);
                }
            }
        }
        return requireRoutePath(expanded.toString(), description + " expanded path");
    }

    /**
     * Maps a URL path to the file that represents it inside the output
     * directory: {@code /} becomes {@code index.html}, a path with an extension
     * is written as-is, and anything else becomes {@code <path>/index.html}.
     */
    static String outputFile(String path) {
        if ("/".equals(path)) {
            return "index.html";
        }
        String body = path.substring(1);
        if (path.endsWith("/")) {
            return body + "index.html";
        }
        String last = body.substring(body.lastIndexOf('/') + 1);
        return last.contains(".") ? body : body + "/index.html";
    }

    /**
     * Builds the default canonical URL for a page. Directory-shaped routes end
     * with {@code /}; file routes (for example {@code /404.html}) are kept
     * as-is.
     */
    static String canonical(String baseUrl, String path) {
        if (baseUrl.isEmpty()) {
            return "";
        }
        if ("/".equals(path)) {
            return baseUrl + "/";
        }
        String last = path.substring(path.lastIndexOf('/') + 1);
        if (last.contains(".")) {
            return baseUrl + path;
        }
        return baseUrl + (path.endsWith("/") ? path : path + "/");
    }

    /**
     * Normalizes an asset target: a relative path under the output directory.
     * Leading slashes are tolerated; traversal, empty segments and separators
     * other than {@code /} are rejected.
     */
    static String assetTarget(String target) {
        Objects.requireNonNull(target, "target");
        String cleaned = target.trim();
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.isEmpty()) {
            return "";
        }
        if (cleaned.indexOf('\\') >= 0 || cleaned.indexOf('?') >= 0 || cleaned.indexOf('#') >= 0) {
            throw new IllegalArgumentException(
                    "asset target must be a plain relative path, got \"" + target + "\"");
        }
        for (String segment : cleaned.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException(
                        "asset target must not contain empty, '.' or '..' segments, got \"" + target + "\"");
            }
        }
        return cleaned;
    }

    /** Returns whether {@code value} looks like an absolute URL. */
    static boolean isExternal(String value) {
        return value.startsWith("//") || value.startsWith("mailto:") || value.startsWith("tel:")
                || value.startsWith("data:") || SCHEME.matcher(value).matches();
    }

    private static void requireStaticSegment(String segment, String description, String path) {
        if (segment.isEmpty()) {
            throw new IllegalArgumentException(description + " contains an empty segment: \"" + path + "\"");
        }
        if (".".equals(segment) || "..".equals(segment)) {
            throw new IllegalArgumentException(
                    description + " must not contain '.' or '..' segments: \"" + path + "\"");
        }
        if (!LITERAL_SEGMENT.matcher(segment).matches()) {
            throw new IllegalArgumentException(description + " segment \"" + segment
                    + "\" must match [A-Za-z0-9._~-]+: \"" + path + "\"");
        }
    }

    private static void requireParameterValue(String name, String value, String description) {
        if (!LITERAL_SEGMENT.matcher(value).matches() || ".".equals(value) || "..".equals(value)) {
            throw new IllegalArgumentException(description + " parameter {" + name
                    + "} must be a single safe path segment, got \"" + value + "\"");
        }
    }
}
