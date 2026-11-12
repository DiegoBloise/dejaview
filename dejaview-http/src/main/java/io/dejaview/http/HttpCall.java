package io.dejaview.http;

/**
 * A handle to an in-flight HTTP call. Returned by {@link HttpClient#send};
 * cancelling is best-effort and asynchronous — the consumer is still notified
 * once the transport settles the call (with {@code cancelled} when the
 * cancellation won the race).
 */
public interface HttpCall {

    /** Cancels the call. Safe to call multiple times and after completion. */
    void cancel();

    /** {@code true} once the outcome has been delivered to the consumer. */
    boolean isDone();
}
