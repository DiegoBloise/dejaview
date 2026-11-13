package io.dejaview.state;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistedStoreTest {

    /** A storage adapter whose every operation throws — simulates a broken browser store. */
    private static final class ThrowingStorage implements StateStorage {

        @Override
        public Optional<String> read(String key) {
            throw new IllegalStateException("storage is disabled");
        }

        @Override
        public void write(String key, String value) {
            throw new IllegalStateException("quota exceeded");
        }

        @Override
        public void remove(String key) {
            throw new IllegalStateException("storage is disabled");
        }
    }

    @Test
    void restoresAPersistedValueBeforeAnyObserverExists() {
        MemoryStateStorage storage = new MemoryStateStorage();
        storage.write("theme", "light");

        StateStore<String> delegate = new StateStore<>("dark");
        PersistedStore<String> persisted = new PersistedStore<>("theme", delegate, storage, StateCodec.string());

        assertTrue(persisted.restored());
        assertEquals("light", persisted.store().state());
        assertTrue(persisted.restoreError().isEmpty());
    }

    @Test
    void keepsInitialStateWhenNothingIsStored() {
        MemoryStateStorage storage = new MemoryStateStorage();

        PersistedStore<String> persisted =
                new PersistedStore<>("theme", new StateStore<>("dark"), storage, StateCodec.string());

        assertFalse(persisted.restored());
        assertEquals("dark", persisted.store().state());
        assertTrue(persisted.restoreError().isEmpty());
    }

    @Test
    void decodeFailureRemovesTheCorruptEntryAndKeepsInitialState() {
        MemoryStateStorage storage = new MemoryStateStorage();
        storage.write("count", "not-a-number");

        PersistedStore<Integer> persisted =
                new PersistedStore<>("count", new StateStore<>(7), storage, StateCodec.integer());

        assertFalse(persisted.restored());
        assertEquals(7, persisted.store().state());
        assertEquals(Optional.of(PersistedStore.DECODE_ERROR), persisted.restoreError());
        assertEquals(Optional.empty(), storage.read("count"));
    }

    @Test
    void unreadableStorageBecomesARestoreErrorValue() {
        PersistedStore<String> persisted =
                new PersistedStore<>("theme", new StateStore<>("dark"), new ThrowingStorage(), StateCodec.string());

        assertFalse(persisted.restored());
        assertEquals("dark", persisted.store().state());
        assertEquals(Optional.of(PersistedStore.STORAGE_ERROR), persisted.restoreError());
    }

    @Test
    void stateChangesAreWrittenThroughAfterRestore() {
        MemoryStateStorage storage = new MemoryStateStorage();
        storage.write("theme", "light");

        PersistedStore<String> persisted =
                new PersistedStore<>("theme", new StateStore<>("dark"), storage, StateCodec.string());
        assertEquals("light", storage.read("theme").orElse(""));

        persisted.store().set("solarized");
        assertEquals(Optional.of("solarized"), storage.read("theme"));
        assertTrue(persisted.writeError().isEmpty());
    }

    @Test
    void writeFailuresKeepInMemoryStateAndRecoverOnNextWrite() {
        StateStorage flaky = new StateStorage() {
            private int writes;

            @Override
            public Optional<String> read(String key) {
                return Optional.empty();
            }

            @Override
            public void write(String key, String value) {
                writes++;
                if (writes == 1) {
                    throw new IllegalStateException("quota exceeded");
                }
            }

            @Override
            public void remove(String key) {
            }
        };

        StateStore<String> delegate = new StateStore<>("dark");
        PersistedStore<String> persisted = new PersistedStore<>("theme", delegate, flaky, StateCodec.string());

        delegate.set("solarized");
        assertEquals(Optional.of(PersistedStore.WRITE_ERROR), persisted.writeError());
        assertEquals("solarized", delegate.state());

        delegate.set("light");
        assertTrue(persisted.writeError().isEmpty());
    }

    @Test
    void disposeDetachesWriteThroughAndKeepsTheStoredValue() {
        MemoryStateStorage storage = new MemoryStateStorage();

        PersistedStore<String> persisted =
                new PersistedStore<>("theme", new StateStore<>("dark"), storage, StateCodec.string());
        persisted.store().set("light");
        persisted.dispose();
        assertTrue(persisted.isDisposed());

        persisted.store().set("solarized");
        assertEquals(Optional.of("light"), storage.read("theme"));

        persisted.dispose(); // idempotent
    }

    @Test
    void constructorValidatesItsArguments() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new PersistedStore<>("  ", new StateStore<>("x"), new MemoryStateStorage(), StateCodec.string()));
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> new PersistedStore<>("k", null, new MemoryStateStorage(), StateCodec.string()));
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> new PersistedStore<>("k", new StateStore<>("x"), null, StateCodec.string()));
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> new PersistedStore<>("k", new StateStore<>("x"), new MemoryStateStorage(), null));
    }

    @Test
    void listsRoundTripThroughAnExplicitCodec() {
        StateCodec<List<String>> csv = StateCodec.of(
                value -> value.isEmpty() ? List.of() : java.util.Arrays.asList(value.split("\\|")),
                value -> String.join("|", value));

        MemoryStateStorage storage = new MemoryStateStorage();
        storage.write("items", "ui|java");

        PersistedStore<List<String>> persisted =
                new PersistedStore<>("items", new StateStore<>(List.of()), storage, csv);
        assertEquals(List.of("ui", "java"), persisted.store().state());

        persisted.store().set(List.of("a", "b", "c"));
        assertEquals(Optional.of("a|b|c"), storage.read("items"));
    }
}
