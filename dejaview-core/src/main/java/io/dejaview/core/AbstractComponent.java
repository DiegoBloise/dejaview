package io.dejaview.core;

import io.dejaview.core.internal.Preconditions;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Base class for components that own disposable resources, inputs, outputs,
 * slots and lifecycle hooks.
 *
 * <p>Resources registered with {@link #own(AutoCloseable)} or
 * {@link #onDispose(Runnable)} are released in reverse registration order when
 * the renderer closes the component. This keeps component state ownership
 * explicit without making {@code dejaview-core} depend on the reactive
 * engine.</p>
 *
 * <h2>Inputs</h2>
 * <p>Parents push values with {@link ComponentView#input(String, Object)};
 * read them with {@link #input(String, Class)} or {@link #inputOr(String, Class, Object)}.
 * Reading an input that was never bound, or bound to a value of an
 * incompatible type, fails fast with a {@link DejaViewException} that names
 * the component, the input and the expected type.</p>
 *
 * <h2>Outputs</h2>
 * <p>Parents register listeners with {@link ComponentView#on(String, Consumer)};
 * emit values with {@link #emit(String, Object)}. Every registered listener is
 * invoked in registration order. Emission with no registered listener is a
 * no-op.</p>
 *
 * <h2>Slots</h2>
 * <p>Parents project content with {@link ComponentView#slot(View...)} (default
 * slot) or {@link ComponentView#slot(String, View...)} (named slot); splice it
 * into the rendered tree with {@link #slot()} or {@link #slot(String)}.</p>
 *
 * <pre>{@code
 * final class UserCard extends AbstractComponent {
 *     @Override
 *     public View render() {
 *         return Views.div()
 *                 .addChildren(slot("header"))
 *                 .child(Views.h2(input("username", String.class)))
 *                 .onClick(() -> emit("select", input("username", String.class)));
 *     }
 * }
 * }</pre>
 *
 * <p>Subclasses cannot override {@link #close()}. Register cleanup work with
 * {@link #onDispose(Runnable)} instead, or override the {@link #onDestroy()}
 * lifecycle hook when cleanup must run before resources are released.</p>
 */
public abstract class AbstractComponent implements Component {

    private final ResourceRegistry resources = new ResourceRegistry();

    private ComponentBindings bindings = ComponentBindings.empty();

    /**
     * Receives the bindings attached to the component view for the current
     * render pass. Called by the renderer; not intended for application use.
     */
    @Override
    public void bind(ComponentBindings newBindings) {
        bindings = Objects.requireNonNull(newBindings, "bindings");
    }

    /**
     * Returns the bindings received for the current render pass. Mainly
     * useful for tests and diagnostics.
     */
    protected final ComponentBindings bindings() {
        return bindings;
    }

    /**
     * Reads a required input.
     *
     * @param name the input name, must not be blank
     * @param type the expected type, must not be {@code null}
     * @param <T>  the expected type of the value
     * @return the bound value, never {@code null}
     * @throws DejaViewException with code {@code "missing-input"} when the input
     *                           was not bound or was bound to {@code null}
     * @throws DejaViewException with code {@code "invalid-input"} when the bound
     *                           value is not an instance of {@code type}
     */
    protected final <T> T input(String name, Class<T> type) {
        String checked = Preconditions.requireText(name, "input name");
        Objects.requireNonNull(type, "type");
        Object value = bindings.inputs().get(checked);
        if (value == null) {
            throw new DejaViewException(
                    "missing-input",
                    "Component '" + name() + "' is missing required input '" + checked + "'",
                    null);
        }
        if (!type.isInstance(value)) {
            throw new DejaViewException(
                    "invalid-input",
                    "Component '" + name() + "' input '" + checked + "' must be a " + type.getName()
                            + " but was " + value.getClass().getName(),
                    null);
        }
        return type.cast(value);
    }

    /**
     * Reads an optional input, falling back to {@code defaultValue} when the
     * input was not bound. A bound value of the wrong type still fails fast.
     *
     * @param name         the input name, must not be blank
     * @param type         the expected type, must not be {@code null}
     * @param defaultValue value returned when the input is absent, may be
     *                     {@code null}
     * @param <T>          the expected type of the value
     */
    protected final <T> T inputOr(String name, Class<T> type, T defaultValue) {
        String checked = Preconditions.requireText(name, "input name");
        Objects.requireNonNull(type, "type");
        Object value = bindings.inputs().get(checked);
        if (value == null) {
            return defaultValue;
        }
        if (!type.isInstance(value)) {
            throw new DejaViewException(
                    "invalid-input",
                    "Component '" + name() + "' input '" + checked + "' must be a " + type.getName()
                            + " but was " + value.getClass().getName(),
                    null);
        }
        return type.cast(value);
    }

    /**
     * Returns the views projected into the default slot, in projection order.
     * Returns an empty list when no content was projected.
     */
    protected final List<View> slot() {
        return slot(ComponentBindings.DEFAULT_SLOT);
    }

    /**
     * Returns the views projected into the named slot, in projection order.
     * Returns an empty list when no content was projected under that name.
     *
     * @param name slot name, must not be blank unless it is the default slot
     */
    protected final List<View> slot(String name) {
        if (!ComponentBindings.DEFAULT_SLOT.equals(name)) {
            name = Preconditions.requireText(name, "slot name");
        }
        return bindings.slots().getOrDefault(name, List.of());
    }

    /**
     * Emits an output to every listener registered by the parent, in
     * registration order. Emitting an output with no listeners is a no-op.
     *
     * @param name  output name, must not be blank
     * @param value the emitted value, may be {@code null}
     * @param <T>   the value type, for call-site convenience
     */
    protected final <T> void emit(String name, T value) {
        String checked = Preconditions.requireText(name, "output name");
        List<Consumer<?>> consumers = bindings.outputs().get(checked);
        if (consumers == null) {
            return;
        }
        for (Consumer<?> consumer : consumers) {
            consumerAccept(consumer, value);
        }
    }

    @SuppressWarnings("unchecked")
    private static void consumerAccept(Consumer<?> consumer, Object value) {
        // The parent registers listeners with the type it expects; the type
        // system cannot see that across the view/binding boundary. A mismatch
        // surfaces as a ClassCastException at the listener, which is the
        // earliest observable point.
        ((Consumer<Object>) consumer).accept(value);
    }

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
