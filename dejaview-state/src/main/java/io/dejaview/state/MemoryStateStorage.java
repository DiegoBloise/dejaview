package io.dejaview.state;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An in-memory {@link StateStorage}: process-lifetime map with copy-on-read
 * semantics. Useful for tests, JVM-only tooling and SSR; browser applications
 * normally pick {@code BrowserStorage.local()} or
 * {@code BrowserStorage.session()}.
 */
public final class MemoryStateStorage implements StateStorage {

    private final Map<String, String> entries = new LinkedHashMap<>();

    @Override
    public Optional<String> read(String key) {
        return Optional.ofNullable(entries.get(key));
    }

    @Override
    public void write(String key, String value) {
        entries.put(key, value);
    }

    @Override
    public void remove(String key) {
        entries.remove(key);
    }

    /**
     * Number of keys currently stored.
     */
    public int size() {
        return entries.size();
    }
}
