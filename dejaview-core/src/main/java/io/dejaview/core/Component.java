package io.dejaview.core;

/**
 * A reusable unit of view composition.
 *
 * <p>A component produces a {@link View} through {@link #render()}. The
 * renderer creates one component instance per mounted component node, calls
 * {@link #render()} on initial mount and on every reactive update, and closes
 * the instance when the node is removed or the mounted tree is disposed.</p>
 *
 * <p>Because the instance survives re-renders, component fields are the
 * component's own state. Components that create reactive resources should
 * dispose them in {@link #close()} or extend {@link AbstractComponent}, which
 * owns a {@link ResourceRegistry}.</p>
 *
 * <p>{@code Component} is a functional interface: a stateless component can be
 * written as a lambda returning a view.</p>
 *
 * <pre>{@code
 * Component greeting = () -> Views.h1("Hello");
 * }</pre>
 */
@FunctionalInterface
public interface Component extends AutoCloseable {

    /**
     * Returns the view this component currently represents.
     *
     * @throws NullPointerException if the returned view is {@code null}
     */
    View render();

    /**
     * Returns the component name used in diagnostics.
     *
     * <p>The default is the simple class name. Lambda-based components report
     * {@code "Component"}. Override this method when a clearer name helps.</p>
     */
    default String name() {
        String simpleName = getClass().getSimpleName();
        return simpleName.isEmpty() ? "Component" : simpleName;
    }

    /**
     * Releases resources owned by this component.
     *
     * <p>The default implementation does nothing. The renderer calls this
     * method exactly once per mounted component instance. Implementations must
     * not throw checked exceptions.</p>
     */
    @Override
    default void close() {
    }
}
