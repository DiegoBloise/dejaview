package io.dejaview.router;

import io.dejaview.core.DejaViewException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * An in-memory {@link RouterHistory}, used by default and in tests.
 *
 * <p>The stack is seeded with a single entry ({@code "/"} unless another seed
 * URL is given at construction). {@link #push(String)} truncates any forward
 * entries, {@link #replace(String)} rewrites the current entry, and
 * {@link #back()}/{@link #forward()} move the cursor and notify listeners.
 * Listeners are not notified of {@link #push(String)}/{@link #replace(String)};
 * those are invoked by the router only after resolution has been committed.</p>
 */
public final class MemoryRouterHistory implements RouterHistory {

    private final List<String> entries = new ArrayList<>();
    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private int index;

    /** Creates a stack seeded with {@code "/"}. */
    public MemoryRouterHistory() {
        this("/");
    }

    /** Creates a stack seeded with the given URL. Blank URLs are rejected. */
    public MemoryRouterHistory(String seed) {
        if (seed == null || seed.isBlank()) {
            throw new DejaViewException("invalid-route", "memory history seed must not be blank", null);
        }
        entries.add(seed);
        index = 0;
    }

    @Override
    public String current() {
        return entries.get(index);
    }

    @Override
    public void push(String url) {
        Objects.requireNonNull(url, "url must not be null");
        if (index < entries.size() - 1) {
            entries.subList(index + 1, entries.size()).clear();
        }
        entries.add(url);
        index = entries.size() - 1;
    }

    @Override
    public void replace(String url) {
        Objects.requireNonNull(url, "url must not be null");
        entries.set(index, url);
    }

    @Override
    public void back() {
        if (index > 0) {
            index--;
            notify(entries.get(index));
        }
    }

    @Override
    public void forward() {
        if (index < entries.size() - 1) {
            index++;
            notify(entries.get(index));
        }
    }

    @Override
    public AutoCloseable listen(Consumer<String> listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /** Read-only view of the stack, for diagnostics and tests. */
    public List<String> entries() {
        return Collections.unmodifiableList(entries);
    }

    /** The current cursor position in the stack. */
    public int index() {
        return index;
    }

    private void notify(String url) {
        for (Consumer<String> listener : listeners) {
            listener.accept(url);
        }
    }
}
