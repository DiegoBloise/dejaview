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
 * written as a lambda returning a view, for example
 * {@code () -> Views.h1("Hello")}. Inputs, outputs, slots and lifecycle hooks
 * are supported only by base classes that implement them — extend
 * {@link AbstractComponent} to receive {@link ComponentBindings} and to
 * override {@link #onInit()}, {@link #onUpdate()} and {@link #onDestroy()}.</p>
 *
 * <h2>Lifecycle</h2>
 * <p>The renderer calls the hooks in this order:</p>
 * <ol>
 *   <li>{@link #bind(ComponentBindings)} and {@link #onInit()} once, right
 *       after the instance is created and before the first render.</li>
 *   <li>{@link #bind(ComponentBindings)} before every subsequent render
 *       pass, followed by {@link #onUpdate()} after the rendered tree has
 *       been reconciled.</li>
 *   <li>{@link #onDestroy()} exactly once when the component node is
 *       permanently removed, before {@link #close()} and before the rendered
 *       subtree is released.</li>
 * </ol>
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
     * Receives the bindings attached to the component view for the current
     * render pass.
     *
     * <p>Called once after instantiation and before every render pass,
     * including the first. The default implementation ignores the bindings,
     * which keeps lambda-based components compiling unchanged; extend
     * {@link AbstractComponent} to consume inputs, outputs and slots.</p>
     */
    default void bind(ComponentBindings bindings) {
    }

    /**
     * Called once after the component is created and bound, before the first
     * render.
     */
    default void onInit() {
    }

    /**
     * Called after every re-render pass, once the rendered tree has been
     * reconciled. It is not called for the initial mount; use
     * {@link #onInit()} for that.
     */
    default void onUpdate() {
    }

    /**
     * Called exactly once when the component node is permanently removed,
     * before {@link #close()} and before the rendered subtree is released.
     */
    default void onDestroy() {
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
