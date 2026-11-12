package io.dejaview.core;

import java.util.Objects;

/**
 * Application-scoped container for configuration, logging and owned resources.
 *
 * <p>The context is deliberately small. Dependency injection, component
 * ownership and platform adapters arrive in later milestones; this type only
 * provides the shared lifetime that those features will need.</p>
 *
 * <p>Contexts are not thread-safe. A context must be disposed when the
 * application stops.</p>
 */
public final class ApplicationContext implements AutoCloseable {

    private final FrameworkConfig config;
    private final Logger logger;
    private final ResourceRegistry resources = new ResourceRegistry();

    private boolean disposed;

    private ApplicationContext(FrameworkConfig config, Logger logger) {
        this.config = Objects.requireNonNull(config, "config");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Creates a context with the system logger.
     */
    public static ApplicationContext create(FrameworkConfig config) {
        return new ApplicationContext(config, Loggers.system());
    }

    /**
     * Creates a context with an explicit logger.
     */
    public static ApplicationContext create(FrameworkConfig config, Logger logger) {
        return new ApplicationContext(config, logger);
    }

    public FrameworkConfig config() {
        return config;
    }

    public Logger logger() {
        return logger;
    }

    public ResourceRegistry resources() {
        return resources;
    }

    /**
     * Registers a resource that is disposed with this context.
     *
     * @return the same resource, for chaining
     * @throws IllegalStateException if the context is disposed
     */
    public <T extends AutoCloseable> T register(T resource) {
        ensureActive();
        return resources.register(resource);
    }

    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Disposes owned resources. Calling this method more than once has no
     * additional effect.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        resources.dispose();
    }

    @Override
    public void close() {
        dispose();
    }

    private void ensureActive() {
        if (disposed) {
            throw new IllegalStateException("Application context is disposed");
        }
    }
}
