package io.dejaview.di;

import io.dejaview.core.DejaViewException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Shared binding store used by {@link InjectorBuilder} and live {@link Injector}
 * instances.
 *
 * <p>Duplicate detection is local to the registry: binding a token that is also
 * bound on an ancestor injector is allowed (the nearer binding shadows the
 * farther one), but binding the same token twice in the same registry fails
 * fast with {@code duplicate-binding}.</p>
 */
final class Registry {

    private final Map<InjectionToken<?>, TokenRegistration<?>> registrations = new LinkedHashMap<>();
    private String name;

    Registry(String name) {
        this.name = name;
    }

    String name() {
        return name;
    }

    void name(String name) {
        this.name = name;
    }

    <T> TokenBinding<T> bind(InjectionToken<T> token) {
        Objects.requireNonNull(token, "token");
        return new TokenBinding<>(this, token);
    }

    <T> void register(InjectionToken<T> token, Scope scope, Provider<T> provider) {
        register(token, scope, provider, true);
    }

    <T> void register(InjectionToken<T> token, Scope scope, Provider<T> provider, boolean owned) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(provider, "provider");
        if (registrations.containsKey(token)) {
            throw new DejaViewException(
                    "duplicate-binding",
                    "Token '" + token.name() + "' is already bound in injector '" + name + "'",
                    null);
        }
        registrations.put(token, new TokenRegistration<>(token, scope, provider, owned));
    }

    TokenRegistration<?> get(InjectionToken<?> token) {
        return registrations.get(token);
    }

    boolean contains(InjectionToken<?> token) {
        return registrations.containsKey(token);
    }

    /**
     * Returns an isolated copy of this registry with the same name.
     */
    Registry copy() {
        Registry copy = new Registry(name);
        copy.registrations.putAll(registrations);
        return copy;
    }

    record TokenRegistration<T>(InjectionToken<T> token, Scope scope, Provider<T> provider, boolean owned) {
    }
}
