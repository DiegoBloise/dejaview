package io.dejaview.core;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.dejaview.core.internal.Preconditions;

/**
 * Framework-wide configuration shared by platform-independent code.
 *
 * <p>Browser bootstrap options such as the container id belong to the runtime
 * configuration. This type carries application identity and general settings
 * that do not depend on a platform.</p>
 *
 * @param applicationName application name used in diagnostics
 * @param development     whether development diagnostics are enabled
 * @param properties      immutable extra configuration values
 */
public record FrameworkConfig(
        String applicationName, boolean development, Map<String, String> properties) {

    /**
     * Creates a configuration and defensively copies the properties.
     */
    public FrameworkConfig {
        applicationName = Preconditions.requireText(applicationName, "applicationName");
        Objects.requireNonNull(properties, "properties");
        properties = Map.copyOf(properties);
    }

    /**
     * Returns the default configuration.
     */
    public static FrameworkConfig defaults() {
        return new FrameworkConfig("DejaView", false, Map.of());
    }

    /**
     * Returns a configuration property.
     */
    public Optional<String> property(String name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(properties.get(name));
    }

    /**
     * Starts a configuration builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link FrameworkConfig}.
     */
    public static final class Builder {

        private String applicationName = "DejaView";
        private boolean development;
        private final Map<String, String> properties = new java.util.LinkedHashMap<>();

        private Builder() {
        }

        public Builder applicationName(String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        public Builder development(boolean development) {
            this.development = development;
            return this;
        }

        public Builder property(String name, String value) {
            this.properties.put(
                    Preconditions.requireText(name, "name"),
                    Objects.requireNonNull(value, "value"));
            return this;
        }

        public FrameworkConfig build() {
            return new FrameworkConfig(applicationName, development, properties);
        }
    }
}
