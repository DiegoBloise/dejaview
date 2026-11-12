package io.dejaview.di;

/**
 * Factory for a dependency.
 *
 * <p>Providers are explicit: the framework performs no classpath scanning and
 * no reflective construction. A provider receives the injector that owns it (see
 * the per-scope rules in {@link Scope}) and returns the instance.</p>
 *
 * <p>If the instance is {@link AutoCloseable} and the scope is singleton or
 * scoped, the injector that caches it registers it for disposal when that
 * injector closes. Transient instances are never disposed by the injector.</p>
 *
 * @param <T> the type of the produced instance
 */
@FunctionalInterface
public interface Provider<T> {

    /**
     * Creates the instance.
     *
     * <p>May call {@code injector.inject(...)} to resolve further dependencies;
     * cycles introduced this way are detected and reported as
     * {@code circular-dependency} failures.</p>
     *
     * @throws RuntimeException anything thrown by user code; wrapped in
     *                          {@code DejaViewException} with code
     *                          {@code provider-error}
     */
    T provide(Injector injector);
}
