package io.dejaview.router.internal;

import io.dejaview.router.RouterHistory;
import org.teavm.jso.browser.Location;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Browser {@link RouterHistory} backed by {@code window.history}.
 *
 * <p>{@link #current()} derives the URL from {@code location.pathname} plus
 * {@code location.search}; the fragment is dropped. {@link #push(String)} and
 * {@link #replace(String)} call {@code pushState}/{@code replaceState} and
 * never notify listeners. Browser back/forward navigations surface as
 * {@code popstate} and are forwarded to listeners by {@link #listen(Consumer)}.</p>
 *
 * <p>This class only works under TeaVM on the browser. The JVM test suite
 * uses {@code MemoryRouterHistory}; never reference this class from
 * platform-neutral code.</p>
 */
public final class BrowserHistory implements RouterHistory {

    private static final String POPSTATE = "popstate";

    private final Window window = Window.current();
    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private final EventListener<Event> popListener = event -> fire();

    /** Registers the {@code popstate} bridge. */
    public BrowserHistory() {
        window.addEventListener(POPSTATE, popListener);
    }

    @Override
    public String current() {
        Location location = window.getLocation();
        String path = location.getPathName();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        String search = location.getSearch();
        if (search == null || search.isEmpty()) {
            return path;
        }
        return search.charAt(0) == '?' ? path + search : path + '?' + search;
    }

    @Override
    public void push(String url) {
        Objects.requireNonNull(url, "url must not be null");
        window.getHistory().pushState(null, "", url);
    }

    @Override
    public void replace(String url) {
        Objects.requireNonNull(url, "url must not be null");
        window.getHistory().replaceState(null, "", url);
    }

    @Override
    public void back() {
        window.getHistory().back();
    }

    @Override
    public void forward() {
        window.getHistory().forward();
    }

    @Override
    public AutoCloseable listen(Consumer<String> listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    private void fire() {
        String url = current();
        for (Consumer<String> listener : listeners) {
            listener.accept(url);
        }
    }
}
