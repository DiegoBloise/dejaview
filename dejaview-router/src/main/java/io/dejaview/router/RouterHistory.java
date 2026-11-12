package io.dejaview.router;

import java.util.function.Consumer;

/**
 * The history abstraction the router is built on.
 *
 * <p>{@link #push(String)} and {@link #replace(String)} are invoked by the
 * router only after resolution has been committed, so implementations do not
 * need to notify listeners for them. {@link #back()} and {@link #forward()}
 * are invoked by the application (usually through {@code Router#back()} /
 * {@code Router#forward()}) and MUST notify registered listeners with the
 * restored URL, exactly like a browser would fire {@code popstate}.</p>
 *
 * <p>URLs passed to and returned by this interface are normalized by the
 * router before reaching the history; implementations may assume paths with a
 * leading slash and no fragment.</p>
 */
public interface RouterHistory {

    /** The current URL (path plus query, without fragment). */
    String current();

    /** Moves the current entry to the end of the stack. */
    void push(String url);

    /** Rewrites the current entry. */
    void replace(String url);

    /** Moves one entry back, firing listeners with the restored URL. */
    void back();

    /** Moves one entry forward, firing listeners with the restored URL. */
    void forward();

    /**
     * Registers a listener for history-initiated navigations (browser
     * {@code popstate}, or {@link #back()}/{@link #forward()}). Router-initiated
     * {@link #push(String)}/{@link #replace(String)} do not notify listeners.
     *
     * @return a handle that unregisters the listener
     */
    AutoCloseable listen(Consumer<String> listener);

    /**
     * Returns the browser {@link RouterHistory} backed by
     * {@code window.history} and {@code popstate}. Only callable in code
     * compiled for the browser (TeaVM); on the JVM this fails with
     * {@link NoClassDefFoundError}. Tests should use
     * {@link MemoryRouterHistory}.
     */
    static RouterHistory browser() {
        return new io.dejaview.router.internal.BrowserHistory();
    }
}
