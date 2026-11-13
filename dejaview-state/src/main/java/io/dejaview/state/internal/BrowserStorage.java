package io.dejaview.state.internal;

import io.dejaview.state.StateStorage;
import org.teavm.jso.browser.Storage;
import org.teavm.jso.browser.Window;

import java.util.Optional;

/**
 * Browser {@link StateStorage} backed by {@code localStorage} or
 * {@code sessionStorage}.
 *
 * <p>Storage access in browsers is allowed to fail: private browsing modes
 * may throw when {@code localStorage} is first touched, quota may be
 * exceeded mid-session, and a site may run with storage disabled by
 * policy. Every operation of this adapter propagates those exceptions
 * unchecked so that {@code PersistedStore} can convert them into
 * {@code storage-error} / {@code storage-write-error} values instead of
 * crashing the application.</p>
 *
 * <p>When {@code localStorage}/{@code sessionStorage} cannot even be
 * reached, the adapter is still returned: each operation throws
 * {@code IllegalStateException}, which the caller handles as described
 * above.</p>
 *
 * <p>This class only works under TeaVM on the browser. The JVM test suite
 * uses {@code MemoryStateStorage}; never reference this class from
 * platform-neutral code paths that must run on the JVM.</p>
 */
public final class BrowserStorage implements StateStorage {

    private final Storage delegate;
    private final String kind;

    private BrowserStorage(Storage delegate, String kind) {
        this.delegate = delegate;
        this.kind = kind;
    }

    /**
     * An adapter over {@code window.localStorage}.
     */
    public static BrowserStorage local() {
        return new BrowserStorage(tryGet(true), "localStorage");
    }

    /**
     * An adapter over {@code window.sessionStorage}.
     */
    public static BrowserStorage session() {
        return new BrowserStorage(tryGet(false), "sessionStorage");
    }

    @Override
    public Optional<String> read(String key) {
        ensureUsable();
        return Optional.ofNullable(delegate.getItem(key));
    }

    @Override
    public void write(String key, String value) {
        ensureUsable();
        delegate.setItem(key, value);
    }

    @Override
    public void remove(String key) {
        ensureUsable();
        delegate.removeItem(key);
    }

    private void ensureUsable() {
        if (delegate == null) {
            throw new IllegalStateException(kind + " is unavailable in this browser context");
        }
    }

    private static Storage tryGet(boolean local) {
        try {
            Window window = Window.current();
            return local ? window.getLocalStorage() : window.getSessionStorage();
        } catch (Throwable unavailable) {
            return null;
        }
    }
}
