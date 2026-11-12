package io.dejaview.di;

import io.dejaview.core.internal.Preconditions;

import java.util.Objects;

/**
 * Collects bindings and builds a root injector.
 *
 * <p>The builder keeps its registrations until {@link #build()} is called; the
 * built injector owns an isolated copy, so later calls made on the builder do
 * not affect previously built injectors, and vice versa. The same token bound
 * twice in one registry fails fast with {@code duplicate-binding}; a child
 * injector can also shadow an ancestor's binding because lookups walk the
 * hierarchy from the resolving injector upwards.</p>
 */
public final class InjectorBuilder {

    private static final String DEFAULT_NAME = "root";

    private final Registry registry = new Registry(DEFAULT_NAME);

    InjectorBuilder() {
    }

    /**
     * Sets the diagnostic name used in error messages. Defaults to
     * {@code "root"}.
     */
    public InjectorBuilder name(String name) {
        registry.name(Preconditions.requireText(name, "injector name"));
        return this;
    }

    /**
     * Starts a binding for {@code token}. See {@link TokenBinding}.
     */
    public <T> TokenBinding<T> bind(InjectionToken<T> token) {
        return registry.bind(Objects.requireNonNull(token, "token"));
    }

    /**
     * Starts a binding for the canonical token of {@code type}.
     */
    public <T> TokenBinding<T> bind(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return bind(InjectionToken.of(type));
    }

    /**
     * Builds the root injector. The builder can be reused to build another
     * root injector with the same registrations.
     */
    public Injector build() {
        return new Injector(registry.copy(), null);
    }
}
