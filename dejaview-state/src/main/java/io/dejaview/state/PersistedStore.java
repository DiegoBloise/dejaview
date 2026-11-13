package io.dejaview.state;

import io.dejaview.reactivity.Effect;

import java.util.Objects;
import java.util.Optional;

/**
 * A {@link StateStore} whose value survives page reloads, backed by a
 * {@link StateStorage} and a {@link StateCodec}.
 *
 * <p>Construction order is deterministic:</p>
 * <ol>
 *   <li>the stored value under {@code key} is read and decoded, and — when
 *       present and valid — replaces the delegate store's initial state
 *       before any observer exists (so no write-through happens during
 *       restore);</li>
 *   <li>a write-through effect is then registered: every subsequent change to
 *       the delegate store is encoded and written to {@code key}.</li>
 * </ol>
 *
 * <p>Failures are values, not crashes — installing a broken or full storage
 * must not break the application:</p>
 * <ul>
 *   <li>{@code decode-error} — the stored payload could not be decoded. The
 *       corrupt entry is removed; the store keeps its initial state;</li>
 *   <li>{@code storage-error} — the storage could not be read at startup.
 *       The store keeps its initial state;</li>
 *   <li>{@code storage-write-error} — a write-through failed. The in-memory
 *       state is unaffected; the next change retries.</li>
 * </ul>
 *
 * <p>Disposing detaches the write-through effect; the stored value is kept.
 * Single-threaded, like the rest of the reactivity primitives.</p>
 *
 * @param <T> state type
 */
public final class PersistedStore<T> implements AutoCloseable {

    /** Stable restore-failure code for undecodable payloads. */
    public static final String DECODE_ERROR = "decode-error";

    /** Stable restore-failure code for unreadable storage. */
    public static final String STORAGE_ERROR = "storage-error";

    /** Stable write-failure code for failed write-throughs. */
    public static final String WRITE_ERROR = "storage-write-error";

    private final StateStore<T> delegate;
    private final StateStorage storage;
    private final StateCodec<T> codec;
    private final String key;
    private final Effect writeEffect;
    private boolean writeFirstRun = true;
    private boolean restored;
    private String restoreError;
    private String writeError;
    private boolean disposed;

    /**
     * Restores {@code delegate} from {@code storage} under {@code key} and
     * attaches write-through persistence.
     *
     * @throws IllegalArgumentException if {@code key} is blank
     * @throws NullPointerException if any argument is null
     */
    public PersistedStore(String key, StateStore<T> delegate, StateStorage storage, StateCodec<T> codec) {
        Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.key = key;

        // Restore phase. No observers exist yet, so this cannot echo back.
        try {
            Optional<String> stored = storage.read(key);
            if (stored.isPresent()) {
                delegate.set(codec.decode(stored.get()));
                this.restored = true;
            }
        } catch (StateCodecException decodeFailure) {
            this.restoreError = DECODE_ERROR;
            try {
                storage.remove(key);
            } catch (RuntimeException ignored) {
                // Best effort: a storage that cannot read cannot remove either.
            }
        } catch (RuntimeException storageFailure) {
            this.restoreError = STORAGE_ERROR;
        }

        // Write-through phase. The first effect run only subscribes; it does
        // not write, so an unreadable-but-present entry is never clobbered.
        this.writeEffect = new Effect(this::writeThrough);
    }

    /**
     * The underlying store — reads, writes and selectors go through it.
     */
    public StateStore<T> store() {
        return delegate;
    }

    /**
     * The storage key this store persists under.
     */
    public String key() {
        return key;
    }

    /**
     * Whether a persisted value was restored at construction.
     */
    public boolean restored() {
        return restored;
    }

    /**
     * The restore failure code, if any: {@link #DECODE_ERROR} or
     * {@link #STORAGE_ERROR}.
     */
    public Optional<String> restoreError() {
        return Optional.ofNullable(restoreError);
    }

    /**
     * The latest write failure code, if any: {@link #WRITE_ERROR}. Empty again
     * after a successful write.
     */
    public Optional<String> writeError() {
        return Optional.ofNullable(writeError);
    }

    /**
     * Detaches the write-through effect. The persisted value is left alone.
     * Calling this method more than once has no additional effect.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        writeEffect.dispose();
    }

    /**
     * Returns whether {@link #dispose()} has been called.
     */
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void close() {
        dispose();
    }

    private void writeThrough() {
        if (disposed || delegate.isDisposed()) {
            return;
        }
        // Always read: the effect must register its dependency on the first
        // run even though the first write is skipped.
        T current = delegate.state();
        if (writeFirstRun) {
            writeFirstRun = false;
            return;
        }
        try {
            storage.write(key, codec.encode(current));
            this.writeError = null;
        } catch (RuntimeException failure) {
            this.writeError = WRITE_ERROR;
        }
    }
}
