package io.dejaview.state;

import java.util.Objects;

/**
 * Encodes application state to and from the strings a {@link StateStorage}
 * holds. Codecs are deliberately dumb: no schema negotiation, no silent
 * defaults. A payload that cannot be decoded raises
 * {@link StateCodecException}, and {@link PersistedStore} handles that case
 * explicitly.
 *
 * @param <T> state type
 */
public interface StateCodec<T> {

    /**
     * Decodes {@code stored} back to state.
     *
     * @throws StateCodecException if {@code stored} is not a valid encoding
     */
    T decode(String stored);

    /**
     * Encodes {@code state} for persistence. Encoding is expected to be total;
     * failures throw {@link StateCodecException}.
     */
    String encode(T state);

    /**
     * Creates a codec from decode and encode functions. Exceptions thrown by
     * either side are wrapped into {@link StateCodecException}.
     *
     * @throws NullPointerException if either function is null
     */
    static <T> StateCodec<T> of(ThrowingFunction<String, T> decode, ThrowingFunction<T, String> encode) {
        Objects.requireNonNull(decode, "decode");
        Objects.requireNonNull(encode, "encode");
        return new StateCodec<>() {
            @Override
            public T decode(String stored) {
                try {
                    T value = decode.apply(stored);
                    if (value == null) {
                        throw new StateCodecException("decode returned null");
                    }
                    return value;
                } catch (StateCodecException failure) {
                    throw failure;
                } catch (Exception failure) {
                    throw new StateCodecException("cannot decode: " + failure.getMessage(), failure);
                }
            }

            @Override
            public String encode(T state) {
                try {
                    String encoded = encode.apply(Objects.requireNonNull(state, "state"));
                    if (encoded == null) {
                        throw new StateCodecException("encode returned null");
                    }
                    return encoded;
                } catch (StateCodecException failure) {
                    throw failure;
                } catch (Exception failure) {
                    throw new StateCodecException("cannot encode: " + failure.getMessage(), failure);
                }
            }
        };
    }

    /**
     * The identity codec for plain strings.
     */
    static StateCodec<String> string() {
        return StateCodec.of(value -> value, value -> value);
    }

    /**
     * A codec for boxed integers. A stored payload that is not a base-10
     * integer throws {@link StateCodecException}.
     */
    static StateCodec<Integer> integer() {
        return StateCodec.of(
                value -> Integer.valueOf(value.trim()),
                value -> Integer.toString(value));
    }

    /**
     * Function that may fail; any exception other than
     * {@link StateCodecException} is wrapped into one by
     * {@link #of(ThrowingFunction, ThrowingFunction)}.
     */
    @FunctionalInterface
    interface ThrowingFunction<A, B> {
        B apply(A value) throws Exception;
    }
}
