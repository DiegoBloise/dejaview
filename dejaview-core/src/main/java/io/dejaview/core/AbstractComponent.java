package io.dejaview.core;

import java.util.Objects;

/**
 * Base class for components that own disposable resources.
 *
 * <p>Resources registered with {@link #own(AutoCloseable)} or
 * {@link #onDispose(Runnable)} are released in reverse registration order when
 * the renderer closes the component. This keeps component state ownership
 * explicit without making {@code dejaview-core} depend on the reactive
 * engine.</p>
 *
 * <pre>{@code
 * final class CounterComponent extends AbstractComponent {
 *     private final Signal<Integer> count = new Signal<>(0);
 *
 *     @Override
 *     public View render() {
 *         return Views.button("Count: " + count.get());
 *     }
 * }
 * }</pre>
 *
 * <p>Subclasses cannot override {@link #close()}. Register cleanup work with
 * {@link #onDispose(Runnable)} instead.</p>
 */
public abstract class AbstractComponent implements Component {

    private final ResourceRegistry resources = new ResourceRegistry();

    /**
     * Registers a resource that is closed with this component.
     *
     * @return the same resource, for chaining
     * @throws IllegalStateException if the component is already closed
     */
    protected final <T extends AutoCloseable> T own(T resource) {
        return resources.register(resource);
    }

    /**
     * Registers a cleanup action that runs when this component is closed.
     *
     * @throws IllegalStateException if the component is already closed
     */
    protected final void onDispose(Runnable action) {
        Objects.requireNonNull(action, "action");
        resources.register((AutoCloseable) action::run);
    }

    /**
     * Disposes owned resources in reverse registration order. Calling this
     * method more than once has no additional effect.
     */
    @Override
    public final void close() {
        resources.dispose();
    }
}
