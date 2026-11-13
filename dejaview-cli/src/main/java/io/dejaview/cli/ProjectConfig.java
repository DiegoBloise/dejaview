package io.dejaview.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration loaded from {@code dejaview.properties} at the project root.
 * Supported keys: {@code port}, {@code host}, {@code webappDir}. Missing files
 * yield defaults; malformed files fail with the offending line number.
 */
public record ProjectConfig(int port, String host, Path webappDir) {

    public static final String FILE_NAME = "dejaview.properties";
    public static final int DEFAULT_PORT = 4321;
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final String DEFAULT_WEBAPP_DIR = "target/webapp";

    public static ProjectConfig defaults() {
        return new ProjectConfig(DEFAULT_PORT, DEFAULT_HOST, Path.of(DEFAULT_WEBAPP_DIR));
    }

    public static ProjectConfig load(Path projectDir) throws CliException {
        Path file = projectDir.resolve(FILE_NAME);
        if (!Files.exists(file)) {
            return defaults();
        }
        Map<String, String> values = new LinkedHashMap<>();
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            throw new CliException("Could not read " + file + ": " + e.getMessage());
        }
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i).trim();
            if (raw.isEmpty() || raw.startsWith("#") || raw.startsWith("!")) {
                continue;
            }
            int eq = raw.indexOf('=');
            if (eq <= 0) {
                throw new CliException(file + ": line " + (i + 1) + " is not a key=value pair: \"" + raw + "\"");
            }
            values.put(raw.substring(0, eq).trim(), raw.substring(eq + 1).trim());
        }
        ProjectConfig defaults = defaults();
        int port = defaults.port();
        String host = defaults.host();
        Path webappDir = defaults.webappDir();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            switch (entry.getKey()) {
                case "port" -> {
                    try {
                        port = Integer.parseInt(entry.getValue());
                    } catch (NumberFormatException e) {
                        throw new CliException(file + ": port must be a number, got \"" + entry.getValue() + "\"");
                    }
                    if (port < 1 || port > 65535) {
                        throw new CliException(file + ": port must be between 1 and 65535, got " + port);
                    }
                }
                case "host" -> {
                    if (entry.getValue().isBlank()) {
                        throw new CliException(file + ": host must not be blank");
                    }
                    host = entry.getValue();
                }
                case "webappDir" -> {
                    if (entry.getValue().isBlank()) {
                        throw new CliException(file + ": webappDir must not be blank");
                    }
                    webappDir = Path.of(entry.getValue());
                }
                default -> throw new CliException(file + ": unknown key \"" + entry.getKey()
                        + "\" (supported: port, host, webappDir)");
            }
        }
        return new ProjectConfig(port, host, webappDir);
    }
}
