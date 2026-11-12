package io.dejaview.router.internal;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits and normalizes raw URLs for the matcher.
 *
 * <ul>
 *   <li>The fragment ({@code #…}) is dropped entirely.</li>
 *   <li>The path is normalized: blank becomes {@code /}, duplicate slashes are
 *       collapsed, a missing leading slash is added, and a trailing slash is
 *       stripped except for the root.</li>
 *   <li>Path segments are split on the <em>raw</em> path before
 *       percent-decoding, so an encoded {@code %2F} never acts as a segment
 *       separator. In the path a {@code +} is a literal plus.</li>
 *   <li>The query string is split on {@code &}, then on the first
 *       {@code =}; keys and values are decoded with {@code +} meaning space.
 *       Repeated keys keep the first value. Empty parts are skipped.
 *       Malformed {@code %XX} sequences are kept raw.</li>
 * </ul>
 */
public final class UrlParser {

    private UrlParser() {
    }

    /** The normalized pieces of a raw URL. All collections are unmodifiable. */
    public record NormalizedUrl(String path,
                                String url,
                                List<String> segments,
                                Map<String, String> queryParams) {
    }

    /** Normalizes {@code raw}. The fragment is dropped. {@code raw} must not be null. */
    public static NormalizedUrl parse(String raw) {
        if (raw == null) {
            throw new NullPointerException("raw must not be null");
        }
        int fragment = raw.indexOf('#');
        String withoutFragment = fragment >= 0 ? raw.substring(0, fragment) : raw;
        int question = withoutFragment.indexOf('?');
        String rawPath = question >= 0 ? withoutFragment.substring(0, question) : withoutFragment;
        String rawQuery = question >= 0 ? withoutFragment.substring(question + 1) : null;

        String path = normalizePath(rawPath);
        List<String> segments = splitSegments(path);
        Map<String, String> queryParams = parseQuery(rawQuery);
        String url = rawQuery == null ? path : path + '?' + rawQuery;
        return new NormalizedUrl(path, url, segments, queryParams);
    }

    private static String normalizePath(String rawPath) {
        String p = rawPath;
        if (p.isEmpty()) {
            return "/";
        }
        StringBuilder collapsed = new StringBuilder(p.length());
        boolean lastWasSlash = false;
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            if (c == '/') {
                if (lastWasSlash) {
                    continue;
                }
                lastWasSlash = true;
            } else {
                lastWasSlash = false;
            }
            collapsed.append(c);
        }
        p = collapsed.toString();
        if (p.charAt(0) != '/') {
            p = '/' + p;
        }
        while (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private static List<String> splitSegments(String path) {
        if (path.equals("/")) {
            return List.of();
        }
        String[] raw = path.substring(1).split("/", -1);
        List<String> segments = new ArrayList<>(raw.length);
        for (String segment : raw) {
            segments.add(percentDecode(segment, false));
        }
        return List.copyOf(segments);
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return Map.of();
        }
        Map<String, String> params = new LinkedHashMap<>();
        for (String part : rawQuery.split("&", -1)) {
            if (part.isEmpty()) {
                continue;
            }
            int equals = part.indexOf('=');
            String rawKey = equals >= 0 ? part.substring(0, equals) : part;
            String rawValue = equals >= 0 ? part.substring(equals + 1) : "";
            String key = percentDecode(rawKey, true);
            if (key.isEmpty()) {
                continue;
            }
            String value = percentDecode(rawValue, true);
            params.putIfAbsent(key, value); // first-wins for repeated keys
        }
        return java.util.Collections.unmodifiableMap(params);
    }

    /**
     * Percent-decodes {@code value}. Malformed sequences ({@code %} not
     * followed by two hex digits) are kept raw. With {@code plusAsSpace},
     * {@code '+'} decodes to a space.
     */
    public static String percentDecode(String value, boolean plusAsSpace) {
        if (value == null || (value.indexOf('%') < 0 && (!plusAsSpace || value.indexOf('+') < 0))) {
            return value;
        }
        // Collect raw bytes: percent-decoded bytes are UTF-8; everything else is encoded as UTF-8.
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream(value.length());
        for (int i = 0; i < value.length();) {
            char c = value.charAt(i);
            if (c == '+' && plusAsSpace) {
                bytes.write(' ');
                i++;
                continue;
            }
            if (c == '%') {
                int hi = hexAt(value, i + 1);
                int lo = hexAt(value, i + 2);
                if (hi >= 0 && lo >= 0) {
                    bytes.write((hi << 4) | lo);
                    i += 3;
                    continue;
                }
                bytes.write('%'); // keep raw
                i++;
                continue;
            }
            if (c < 0x80) {
                bytes.write(c);
                i++;
                continue;
            }
            String ch = String.valueOf(c);
            if (Character.isHighSurrogate(c) && i + 1 < value.length() && Character.isLowSurrogate(value.charAt(i + 1))) {
                ch = value.substring(i, i + 2);
                i += 2;
            } else {
                i++;
            }
            byte[] encoded = ch.getBytes(StandardCharsets.UTF_8);
            bytes.write(encoded, 0, encoded.length);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static int hexAt(String value, int index) {
        if (index >= value.length()) {
            return -1;
        }
        char c = value.charAt(index);
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }
}
