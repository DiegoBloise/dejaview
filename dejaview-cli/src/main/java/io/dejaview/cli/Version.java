package io.dejaview.cli;

import java.io.InputStream;
import java.util.Properties;

/**
 * Resolves the DejaView version reported by the CLI. The value comes from the
 * jar manifest, which is stamped by Maven from {@code project.version}; a plain
 * checkout without packaging falls back to the development version.
 */
final class Version {

    static final String FALLBACK = "0.1.0-SNAPSHOT";

    private Version() {
    }

    static String current() {
        try (InputStream in = Version.class.getResourceAsStream("/io/dejaview/cli/version.properties")) {
            if (in != null) {
                Properties properties = new Properties();
                properties.load(in);
                String value = properties.getProperty("dejaview.version");
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }
        } catch (Exception ignored) {
            // Fall through to the package implementation version.
        }
        String impl = Version.class.getPackage().getImplementationVersion();
        if (impl != null && !impl.isBlank()) {
            return impl;
        }
        return FALLBACK;
    }
}
