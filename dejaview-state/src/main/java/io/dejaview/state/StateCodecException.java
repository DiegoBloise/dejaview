package io.dejaview.state;

/**
 * Failure raised by a {@link StateCodec} when a value cannot be encoded or
 * decoded. {@link PersistedStore} treats a decoding failure as
 * {@code "decode-error"}: the corrupt entry is removed and the store keeps
 * its initial state.
 */
public final class StateCodecException extends RuntimeException {

    public StateCodecException(String message) {
        super(message);
    }

    public StateCodecException(String message, Throwable cause) {
        super(message, cause);
    }
}
