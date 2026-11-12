package io.dejaview.di;

import io.dejaview.core.internal.Preconditions;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A typed key for a dependency.
 *
 * <p>Injection tokens are identity-based: two tokens are equal only when they are
 * the same object. Tokens created with {@link #of(String)} are distinct even with
 * the same name, which is deliberate — it forces a single canonical token per
 * dependency. {@link #of(Class)} is the exception: it returns one cached token per
 * class so {@code bind(Foo.class)} and {@code inject(Foo.class)} always agree.</p>
 *
 * <p>Tokens are immutable and safe to share between threads.</p>
 *
 * @param <T> the type of the dependency the token stands for
 */
public final class InjectionToken<T> {

    private static final Map<Class<?>, InjectionToken<?>> TOKENS_BY_CLASS =
            new ConcurrentHashMap<>();

    private final String name;

    /**
     * Creates a new, distinct token with the given diagnostic name.
     *
     * @param name diagnostic name used in error messages, must not be blank
     */
    public InjectionToken(String name) {
        this.name = Preconditions.requireText(name, "token name");
    }

    /**
     * Creates a new, distinct token with the given diagnostic name. Equivalent
     * to {@code new InjectionToken<>(name)}.
     */
    public static <T> InjectionToken<T> of(String name) {
        return new InjectionToken<>(name);
    }

    /**
     * Returns the canonical token for {@code type}, creating it on first use.
     *
     * <p>Two calls with the same class return the identical token, so class
     * tokens can be used interchangeably with explicitly declared tokens.</p>
     */
    @SuppressWarnings("unchecked")
    public static <T> InjectionToken<T> of(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return (InjectionToken<T>) TOKENS_BY_CLASS.computeIfAbsent(type,
                key -> new InjectionToken<>(key.getName()));
    }

    /**
     * Diagnostic name used in error messages. Not unique — identity is what
     * matters for resolution.
     */
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return "InjectionToken[" + name + ']';
    }
}
