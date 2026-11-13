package io.dejaview.state;

import java.util.Optional;

/**
 * The state of an asynchronous operation: a status plus, when available, the
 * produced value and — on failure — the structured error.
 *
 * <p>Values are immutable. A {@link #loading(Optional)} state may carry the
 * previous value so views can keep showing stale data while refreshing; a
 * {@link #failure(AsyncError, Optional)} state does the same. Fresh states
 * created after a successful load always carry the value.</p>
 *
 * @param <T> value type; {@code null} is not allowed as a data value
 */
public final class AsyncState<T> {

    private static final AsyncState<?> IDLE = new AsyncState<>(AsyncStatus.IDLE, Optional.empty(), Optional.empty());

    private final AsyncStatus status;
    private final Optional<T> data;
    private final Optional<AsyncError> error;

    private AsyncState(AsyncStatus status, Optional<T> data, Optional<AsyncError> error) {
        this.status = status;
        this.data = data;
        this.error = error;
    }

    /**
     * The never-loaded state.
     */
    public static <T> AsyncState<T> idle() {
        @SuppressWarnings("unchecked")
        AsyncState<T> idle = (AsyncState<T>) IDLE;
        return idle;
    }

    /**
     * A load is in flight. {@code previous} keeps the last successful value
     * visible to views that prefer stale-while-revalidate rendering.
     */
    public static <T> AsyncState<T> loading(Optional<T> previous) {
        return new AsyncState<>(AsyncStatus.LOADING, previous, Optional.empty());
    }

    /**
     * The load completed and produced a value.
     */
    public static <T> AsyncState<T> success(T data) {
        java.util.Objects.requireNonNull(data, "data");
        return new AsyncState<>(AsyncStatus.SUCCESS, Optional.of(data), Optional.empty());
    }

    /**
     * The load failed. {@code previous} keeps the last successful value
     * visible, if any.
     */
    public static <T> AsyncState<T> failure(AsyncError error, Optional<T> previous) {
        java.util.Objects.requireNonNull(error, "error");
        return new AsyncState<>(AsyncStatus.FAILURE, previous, Optional.of(error));
    }

    /**
     * The current status.
     */
    public AsyncStatus status() {
        return status;
    }

    /**
     * Whether a load has never been started.
     */
    public boolean isIdle() {
        return status == AsyncStatus.IDLE;
    }

    /**
     * Whether a load is in flight.
     */
    public boolean isLoading() {
        return status == AsyncStatus.LOADING;
    }

    /**
     * Whether the last load produced a value.
     */
    public boolean isSuccess() {
        return status == AsyncStatus.SUCCESS;
    }

    /**
     * Whether the last load failed.
     */
    public boolean isFailure() {
        return status == AsyncStatus.FAILURE;
    }

    /**
     * The last produced value, if any — including while a refresh is in
     * flight or the last refresh failed.
     */
    public Optional<T> data() {
        return data;
    }

    /**
     * The structured error of the last load, present only on failure.
     */
    public Optional<AsyncError> error() {
        return error;
    }

    @Override
    public String toString() {
        return "AsyncState[" + status + (data.map(d -> ", data=present").orElse(""))
                + (error.map(e -> ", code=" + e.code()).orElse("")) + "]";
    }
}
