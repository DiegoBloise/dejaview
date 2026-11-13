package io.dejaview.state;

import java.util.Optional;

/**
 * A minimal string-keyed persistence contract — the seam behind which browser
 * and JVM storages hide their details. Adapters must not throw for routine
 * failures; unexpected exceptions are caught by {@link PersistedStore} and
 * surfaced as error codes.
 *
 * <p>Implementations are single-threaded, like the rest of the reactivity
 * primitives.</p>
 */
public interface StateStorage {

    /**
     * Returns the stored value for {@code key}, or {@link Optional#empty()}
     * when nothing is stored under that key.
     *
     * @throws RuntimeException if the storage cannot be read; {@link PersistedStore}
     *         converts this to the {@code "storage-error"} restore failure
     */
    Optional<String> read(String key);

    /**
     * Stores {@code value} under {@code key}, replacing any existing entry.
     *
     * @throws RuntimeException if the storage cannot be written; {@link PersistedStore}
     *         converts this to the {@code "storage-write-error"} write failure
     */
    void write(String key, String value);

    /**
     * Removes the entry stored under {@code key}, if any. Missing keys are not
     * an error.
     */
    void remove(String key);
}
