package io.dejaview.forms;

/**
 * Asynchronous validation rule for a control value.
 *
 * <p>Asynchronous validators typically call a remote endpoint. They receive a
 * {@link ValidationCallback} and must call it exactly once; the framework
 * discards callbacks for values that have already been superseded. There is no
 * thread or promise model in the browser, so this callback contract is the
 * whole contract.</p>
 *
 * <p>Synchronous validators run first. Asynchronous validators only run when
 * the synchronous result is valid, so the user is not asked to wait for a
 * request that cannot succeed.</p>
 *
 * @param <V> value type
 */
@FunctionalInterface
public interface AsyncValidator<V> {

    /**
     * Starts validation and eventually reports through {@code callback}.
     *
     * @param value    value to validate
     * @param callback completion callback, never {@code null}
     */
    void validate(V value, ValidationCallback callback);
}
