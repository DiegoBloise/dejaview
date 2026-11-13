package io.dejaview.compiler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * An immutable index of the files in an assembled webapp: relative path, byte
 * size and SHA-256 for each entry. Written next to the bundle as
 * {@code dejaview-assets.json} so downstream tooling (cache busting, integrity
 * checks, release diffs) can consume it without re-walking the directory.
 *
 * <p>Entries are sorted by path, so equivalent builds produce byte-identical
 * manifests. The manifest excludes itself from the walk.</p>
 */
public record AssetManifest(List<AssetEntry> entries) {

    public static final String MANIFEST_NAME = "dejaview-assets.json";

    public record AssetEntry(String path, long bytes, String sha256) {
    }

    /** Generates the manifest for {@code root}, excluding {@link #MANIFEST_NAME}. */
    public static AssetManifest generate(Path root) throws IOException {
        Path normalized = root.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) {
            throw new IOException("Asset manifest root is not a directory: " + normalized);
        }
        List<AssetEntry> entries = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(normalized)) {
            for (Path file : walk.filter(Files::isRegularFile)
                    .filter(file -> !file.getFileName().toString().equals(MANIFEST_NAME))
                    .sorted(Comparator.comparing(file -> normalized.relativize(file).toString()))
                    .toList()) {
                String relative = normalized.relativize(file).toString().replace('\\', '/');
                entries.add(new AssetEntry(relative, Files.size(file), Hashing.sha256Hex(file)));
            }
        }
        return new AssetManifest(List.copyOf(entries));
    }

    public long totalBytes() {
        long total = 0;
        for (AssetEntry entry : entries) {
            total += entry.bytes();
        }
        return total;
    }

    /** Minimal, stable JSON rendering (no dependencies). */
    public String toJson() {
        StringBuilder json = new StringBuilder(entries.size() * 96 + 128);
        json.append("{\n  \"version\": 1,\n  \"entries\": [\n");
        for (int i = 0; i < entries.size(); i++) {
            AssetEntry entry = entries.get(i);
            json.append("    {\"path\": \"").append(escape(entry.path()))
                    .append("\", \"bytes\": ").append(entry.bytes())
                    .append(", \"sha256\": \"").append(entry.sha256()).append("\"}");
            json.append(i + 1 < entries.size() ? ",\n" : "\n");
        }
        json.append("  ],\n  \"totalBytes\": ").append(totalBytes()).append("\n}\n");
        return json.toString();
    }

    public void write(Path target) throws IOException {
        Files.writeString(target, toJson(), StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
