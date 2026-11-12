package io.dejaview.di;

/**
 * How long a provider-created instance lives and who owns it.
 *
 * <ul>
 *   <li>{@link #SINGLETON} — one instance per injector where the provider is
 *       registered; shared by the whole subtree and disposed when that injector
 *       closes. This is the default.</li>
 *   <li>{@link #TRANSIENT} — a new instance on every resolution; never cached and
 *       never disposed by the injector.</li>
 *   <li>{@link #SCOPED} — one instance per injector that resolves it (the
 *       "request scope"); disposed when that injector closes.</li>
 * </ul>
 */
public enum Scope {

    /**
     * One instance per injector where the provider is registered, cached and
     * shared by the whole subtree. Default scope.
     */
    SINGLETON,

    /**
     * A new instance on every resolution; never cached, never disposed by the
     * injector.
     */
    TRANSIENT,

    /**
     * One instance per injector that resolves the provider, cached by that
     * injector and disposed when it closes.
     */
    SCOPED
}
