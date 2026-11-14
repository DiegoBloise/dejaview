package io.dejaview.ssg;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates the local {@code href}/{@code src} references of generated pages
 * against the files that were actually written.
 *
 * <p>Static hosting has no server-side routing to catch a broken link: a page
 * that points at a page or asset the build did not produce is a 404 in
 * production. The generator therefore walks every generated document, ignores
 * external URLs and fragments, resolves local references as a static file
 * server would (including directory {@code index.html} fallbacks) and fails the
 * build with the page and URL that cannot be satisfied.</p>
 */
final class StaticReferences {

    private static final Pattern ATTRIBUTE = Pattern.compile(
            "(?i)\\b(href|src)\\s*=\\s*(\"([^\"]*)\"|'([^']*)')");

    private StaticReferences() {
    }

    /** One local reference found in a document. */
    record Reference(String attribute, String url) {
    }

    /** A reference that resolves outside the generated file set. */
    record Problem(String page, String attribute, String url) {
    }

    static List<Reference> extract(String html) {
        List<Reference> references = new ArrayList<>();
        Matcher matcher = ATTRIBUTE.matcher(html);
        while (matcher.find()) {
            String value = matcher.group(3) != null ? matcher.group(3) : matcher.group(4);
            references.add(new Reference(matcher.group(1).toLowerCase(), value));
        }
        return List.copyOf(references);
    }

    /**
     * Resolves and checks every local reference of one page.
     *
     * @param pagePath   the URL path of the page
     * @param html       the generated document
     * @param knownFiles output-relative paths (with {@code /}) that exist
     * @return every reference that does not resolve; empty when the page is valid
     */
    static List<Problem> validate(String pagePath, String html, Set<String> knownFiles) {
        String baseDir = directoryOf(pagePath);
        Map<String, Problem> problems = new LinkedHashMap<>();
        for (Reference reference : extract(html)) {
            String url = reference.url().trim();
            if (url.isEmpty() || url.startsWith("#") || url.startsWith("?")
                    || SsgPaths.isExternal(url)) {
                continue;
            }
            String resolved = resolve(baseDir, url);
            if (resolved == null) {
                problems.putIfAbsent(url, new Problem(pagePath, reference.attribute(), url));
                continue;
            }
            if (knownFiles.contains(resolved)) {
                continue;
            }
            if (knownFiles.contains(resolved + "/index.html")) {
                continue;
            }
            problems.putIfAbsent(url, new Problem(pagePath, reference.attribute(), url));
        }
        return List.copyOf(problems.values());
    }

    private static String directoryOf(String pagePath) {
        String file = SsgPaths.outputFile(pagePath);
        int slash = file.lastIndexOf('/');
        return slash < 0 ? "" : file.substring(0, slash);
    }

    /**
     * Resolves a local URL to an output-relative path, or {@code null} when it
     * would escape the output directory.
     */
    private static String resolve(String baseDir, String url) {
        String path = percentDecode(url);
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        int fragment = path.indexOf('#');
        if (fragment >= 0) {
            path = path.substring(0, fragment);
        }
        if (path.isEmpty()) {
            return null;
        }
        String combined;
        if (path.startsWith("/")) {
            combined = path.substring(1);
        } else {
            combined = baseDir.isEmpty() ? path : baseDir + "/" + path;
        }
        boolean directory = combined.endsWith("/");
        Path normalized;
        try {
            normalized = Path.of(combined);
        } catch (RuntimeException invalid) {
            return null;
        }
        String result = normalized.normalize().toString().replace('\\', '/');
        if (result.isEmpty()) {
            return "index.html";
        }
        if (result.equals("..") || result.startsWith("../")) {
            return null;
        }
        result = stripLeadingDotSlash(result);
        if (directory || result.endsWith("/")) {
            return result.endsWith("/") ? result + "index.html" : result + "/index.html";
        }
        return result;
    }

    private static String stripLeadingDotSlash(String value) {
        String result = value;
        while (result.startsWith("./")) {
            result = result.substring(2);
        }
        return result;
    }

    /**
     * Decodes {@code %XX} sequences without the {@code +}-to-space rule of
     * form encoding: a plus sign is a valid path character.
     */
    private static String percentDecode(String value) {
        if (value.indexOf('%') < 0) {
            return value;
        }
        StringBuilder decoded = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '%' && i + 2 < value.length()) {
                int high = Character.digit(value.charAt(i + 1), 16);
                int low = Character.digit(value.charAt(i + 2), 16);
                if (high >= 0 && low >= 0) {
                    decoded.append((char) ((high << 4) + low));
                    i += 2;
                    continue;
                }
            }
            decoded.append(current);
        }
        return decoded.toString();
    }
}
