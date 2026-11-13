package io.dejaview.state;

/**
 * Lifecycle of an asynchronous state: {@link #IDLE} (never loaded),
 * {@link #LOADING} (a load is in flight), {@link #SUCCESS} (last load
 * produced a value) or {@link #FAILURE} (last load failed).
 */
public enum AsyncStatus {
    IDLE,
    LOADING,
    SUCCESS,
    FAILURE
}
