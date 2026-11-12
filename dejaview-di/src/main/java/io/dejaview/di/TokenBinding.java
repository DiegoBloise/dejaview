package io.dejaview.di;

import java.util.Objects;

/**
 * Terminal configuration for one binding, returned by the {@code bind(...)}
 * methods on {@link InjectorBuilder} and live {@link Injector} instances.
 *
 * <p>A binding starts as a singleton unless {@link #as(Scope)} says otherwise.
 * The terminal {@code to(...)} methods register the binding immediately.</p>
 *
 * @param <T> the type of the produced dependency
 */
public final class TokenBinding<T> {

    private final Registry registry;
    private final InjectionToken<T> token;
    private Scope scope = Scope.SINGLETON;

    TokenBinding(Registry registry, InjectionToken<T> token) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.token = Objects.requireNonNull(token, "token");
    }

    /**
     * Sets the scope for this binding. Defaults to {@link Scope#SINGLETON}.
     */
    public TokenBinding<T> as(Scope scope) {
        this.scope = Objects.requireNonNull(scope, "scope");
        return this;
    }

    /**
     * Binds the token to a provider. The provider is called at most
     * once for {@link Scope#SINGLETON}, once per resolving injector for
     * {@link Scope#SCOPED}, and once per resolution for {@link Scope#TRANSIENT}.
     *
     * <p>{@code Provider} is a functional interface, so lambdas and method
     * references work directly:</p>
     *
     * <pre>{@code
     * .bind(UserService.class)
     *     .to(injector -> new UserService(injector.inject(HttpTransport.class)))
     * }</pre>
     *
     * @throws io.dejaview.core.DejaViewException with code {@code duplicate-binding}
     *         when this token is already bound in the same injector
     */
    public TokenBinding<T> to(Provider<T> provider) {
        Objects.requireNonNull(provider, "provider");
        registry.register(token, scope, provider);
        return this;
    }

    /**
     * Binds the token to a constant instance. The instance is returned as-is on
     * every resolution and is never disposed by the injector, regardless of
     * scope.
     *
     * @throws io.dejaview.core.DejaViewException with code {@code duplicate-binding}
     *         when this token is already bound in the same injector
     */
    public TokenBinding<T> toInstance(T instance) {
        Objects.requireNonNull(instance, "instance");
        registry.register(token, Scope.SINGLETON, injector -> instance, false);
        return this;
    }
}
