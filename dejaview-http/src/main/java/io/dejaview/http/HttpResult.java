package io.dejaview.http;

/**
 * The outcome of an HTTP call. Sealed with two variants so callers switch
 * exhaustively instead of catching exceptions for control flow.
 *
 * <p>Transport-level failures (network, timeout, cancellation) and HTTP
 * error statuses are both reported here; HTTP error statuses are
 * {@link Success} with the failing status code, matching the transport's
 * semantics (a completed round-trip is a success at the transport layer).
 * Use {@link HttpResponse#ok()} to branch on the status.</p>
 */
public sealed interface HttpResult {

    /** The call completed with a response (any HTTP status). */
    record Success(HttpResponse response) implements HttpResult {
        public Success {
            if (response == null) {
                throw new NullPointerException("response must not be null");
            }
        }
    }

    /** The call failed before a response was available. */
    record Failure(HttpFailure failure) implements HttpResult {
        public Failure {
            if (failure == null) {
                throw new NullPointerException("failure must not be null");
            }
        }

        /** Convenience factory matching {@link HttpFailure}. */
        public static Failure of(String code, String message, Throwable cause) {
            return new Failure(new HttpFailure(code, message, cause));
        }
    }

    /** The successful response, or {@code null} when this is a failure. */
    default HttpResponse response() {
        return this instanceof Success success ? success.response() : null;
    }

    /** The failure, or {@code null} when this is a success. */
    default HttpFailure failure() {
        return this instanceof Failure failure ? failure.failure() : null;
    }

    /** {@code true} when this is a {@link Success}. */
    default boolean isSuccess() {
        return this instanceof Success;
    }
}
