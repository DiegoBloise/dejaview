package io.dejaview.dom;

import java.util.Objects;

/**
 * Configuration for the browser runtime.
 *
 * <p>Only options that affect the bootstrap are present. Framework-wide
 * configuration belongs to the platform-independent core.</p>
 *
 * @param containerId id of the element that receives the application
 * @param debug       whether the runtime should expose development diagnostics
 */
public record RuntimeConfig(String containerId, boolean debug) {

    /**
     * Creates a configuration.
     *
     * @throws NullPointerException     if {@code containerId} is {@code null}
     * @throws IllegalArgumentException if {@code containerId} is blank
     */
    public RuntimeConfig {
        Objects.requireNonNull(containerId, "containerId");
        if (containerId.isBlank()) {
            throw new IllegalArgumentException("containerId must not be blank");
        }
    }

    /**
     * Returns the default configuration: container {@code "app"} in production
     * mode.
     */
    public static RuntimeConfig defaults() {
        return new RuntimeConfig("app", false);
    }

    /**
     * Starts a configuration builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link RuntimeConfig}.
     */
    public static final class Builder {

        private String containerId = "app";
        private boolean debug;

        private Builder() {
        }

        public Builder containerId(String containerId) {
            this.containerId = containerId;
            return this;
        }

        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public RuntimeConfig build() {
            return new RuntimeConfig(containerId, debug);
        }
    }
}
