package io.dejaview.core;

import io.dejaview.core.internal.Preconditions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A view node that represents a component.
 *
 * <p>The node holds a component factory rather than a component instance so the
 * renderer can create exactly one instance per mounted node. That instance
 * survives re-renders, which is what makes component state and component-owned
 * resources stable.</p>
 *
 * <p>Use {@link Views#component(Supplier)} for stateless functional
 * components and {@link Views#componentFactory(Supplier)} when every mounted
 * node should get its own class-based instance. {@link #of(Component)} mounts an
 * existing instance.</p>
 *
 * <p>A component view may carry the inputs, output listeners and slot content
 * the parent wants to push into the mounted component:</p>
 *
 * <pre>{@code
 * Views.componentFactory(UserCard::new)
 *         .input("username", username)
 *         .on("select", user -> handleSelect(user))
 *         .slot(Views.p("Projected body"))            // default slot
 *         .slot("header", Views.h2("Members"));       // named slot
 * }</pre>
 *
 * <p>Input values are captured when the view is created and are pushed into
 * the component on every render pass. Re-evaluating them from a parent
 * component or signal is what makes input changes propagate: the parent
 * re-renders, a new view carrying fresh values is produced, and the
 * renderer binds them before calling {@link Component#render()}.</p>
 *
 * <p>When a component node is reconciled against a new view, the instance is
 * reused — its identity, state and resources survive re-renders. When the
 * same template position can produce a <em>different component class</em>
 * across renders (dynamic components), either give the views distinct keys
 * ({@code key("admin")} vs {@code key("user")}), or construct both with
 * {@link Views#componentFactory(Class, java.util.function.Supplier)} so the
 * reconciler can detect the class change and replace the node instead of
 * silently reusing the wrong instance.</p>
 *
 * @param factory       component factory, never {@code null}
 * @param componentType optional class token of the mounted component type.
 *                      When both the stored node token and the next view's
 *                      token are present and differ, the reconciler replaces
 *                      the node instead of reusing it. Pass {@code null} to
 *                      opt out.
 * @param inputs        input values in render order, never {@code null}
 * @param slots         projected views per slot name, never {@code null}
 * @param outputs       output listeners per output name, never {@code null}
 * @param key           optional reconciliation key, or {@code null} when unkeyed
 */
public record ComponentView(
        Supplier<? extends Component> factory,
        Class<? extends Component> componentType,
        Map<String, Object> inputs,
        Map<String, List<View>> slots,
        Map<String, List<Consumer<?>>> outputs,
        String key) implements View {

    public ComponentView {
        Objects.requireNonNull(factory, "factory");
        inputs = copyInputs(inputs);
        slots = copySlots(slots);
        outputs = copyOutputs(outputs);
        Preconditions.requireKey(key, "key");
    }

    /**
     * Creates an unbound view without a class token.
     */
    public ComponentView(Supplier<? extends Component> factory) {
        this(factory, null, Map.of(), Map.of(), Map.of(), null);
    }

    /**
     * Creates a view for an existing component instance. The class token
     * defaults to the instance's runtime class.
     *
     * <p>The mounted tree owns the instance and closes it on disposal. Do not
     * share one instance between mounted nodes.</p>
     */
    public static ComponentView of(Component component) {
        Objects.requireNonNull(component, "component");
        return new ComponentView(() -> component, component.getClass(),
                Map.of(), Map.of(), Map.of(), null);
    }

    /**
     * Creates a view that instantiates a component per mounted node, without
     * a class token.
     */
    public static ComponentView ofFactory(Supplier<? extends Component> factory) {
        return new ComponentView(factory);
    }

    /**
     * Creates a view that instantiates a component per mounted node, carrying
     * an explicit class token so the reconciler can detect component-type
     * swaps at the same template position.
     */
    public static ComponentView ofFactory(
            Class<? extends Component> componentType, Supplier<? extends Component> factory) {
        Objects.requireNonNull(componentType, "componentType");
        return new ComponentView(factory, componentType, Map.of(), Map.of(), Map.of(), null);
    }

    /**
     * Returns a copy of this view with the given input set to {@code value}.
     *
     * @param name  input name, must not be blank
     * @param value input value, must not be {@code null}
     * @return a new view; the receiver is unchanged
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public ComponentView input(String name, Object value) {
        name = Preconditions.requireText(name, "input name");
        Objects.requireNonNull(value, "input value for '" + name + "' must not be null");
        Map<String, Object> updated = new LinkedHashMap<>(inputs);
        updated.put(name, value);
        return new ComponentView(factory, componentType, updated, slots, outputs, key);
    }

    /**
     * Returns a copy of this view projecting the given views into the default
     * slot.
     */
    public ComponentView slot(View... views) {
        Objects.requireNonNull(views, "views");
        return slot(ComponentBindings.DEFAULT_SLOT, views);
    }

    /**
     * Returns a copy of this view projecting the given views into the named
     * slot.
     *
     * @param name slot name, must not be blank unless it is the default slot
     */
    public ComponentView slot(String name, View... views) {
        if (!ComponentBindings.DEFAULT_SLOT.equals(name)) {
            Preconditions.requireText(name, "slot name");
        }
        Objects.requireNonNull(views, "views");
        return slot(name, List.of(views));
    }

    /**
     * Returns a copy of this view projecting the given views into the named
     * slot.
     */
    public ComponentView slot(String name, List<View> views) {
        if (!ComponentBindings.DEFAULT_SLOT.equals(name)) {
            Preconditions.requireText(name, "slot name");
        }
        Objects.requireNonNull(views, "views");
        Map<String, List<View>> updated = new LinkedHashMap<>(slots);
        List<View> merged = new ArrayList<>(updated.getOrDefault(name, List.of()));
        for (View view : views) {
            merged.add(Objects.requireNonNull(view, "view"));
        }
        updated.put(name, List.copyOf(merged));
        return new ComponentView(factory, componentType, inputs, updated, outputs, key);
    }

    /**
     * Returns a copy of this view with an additional listener for the given
     * output.
     *
     * <p>Multiple listeners per output are invoked in registration order when
     * the component emits the output. An output with no listeners is simply
     * not delivered.</p>
     *
     * @param name     output name, must not be blank
     * @param consumer listener, must not be {@code null}
     */
    public ComponentView on(String name, Consumer<?> consumer) {
        name = Preconditions.requireText(name, "output name");
        Objects.requireNonNull(consumer, "output consumer for '" + name + "' must not be null");
        Map<String, List<Consumer<?>>> updated = new LinkedHashMap<>(outputs);
        List<Consumer<?>> listeners = new ArrayList<>(updated.getOrDefault(name, List.of()));
        listeners.add(consumer);
        updated.put(name, List.copyOf(listeners));
        return new ComponentView(factory, componentType, inputs, slots, updated, key);
    }

    /**
     * Returns a copy of this view with the given reconciliation key.
     */
    public ComponentView key(String key) {
        return new ComponentView(factory, componentType, inputs, slots, outputs, key);
    }

    /**
     * Returns the bindings to push into the mounted component for this view.
     */
    public ComponentBindings bindings() {
        return new ComponentBindings(inputs, slots, outputs);
    }

    /**
     * Creates the component instance for a mounted node.
     *
     * @throws NullPointerException if the factory returns {@code null}
     */
    public Component create() {
        return Objects.requireNonNull(factory.get(), "component");
    }

    private static Map<String, Object> copyInputs(Map<String, Object> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("input names must not be null or blank");
            }
            if (entry.getValue() == null) {
                throw new NullPointerException(
                        "input '" + entry.getKey() + "' must not have a null value");
            }
            copy.put(entry.getKey(), entry.getValue());
        }
        return Map.copyOf(copy);
    }

    private static Map<String, List<View>> copySlots(Map<String, List<View>> slots) {
        if (slots == null || slots.isEmpty()) {
            return Map.of();
        }
        Map<String, List<View>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<View>> entry : slots.entrySet()) {
            if (entry.getKey() == null) {
                throw new NullPointerException("slot names must not be null");
            }
            if (!ComponentBindings.DEFAULT_SLOT.equals(entry.getKey())
                    && entry.getKey().isBlank()) {
                throw new IllegalArgumentException("slot names must not be blank; use the default slot instead");
            }
            List<View> views = entry.getValue();
            if (views == null) {
                throw new NullPointerException("slot '" + entry.getKey() + "' must not have null content");
            }
            for (View view : views) {
                Objects.requireNonNull(view, "slot views must not be null");
            }
            copy.put(entry.getKey(), List.copyOf(views));
        }
        return Map.copyOf(copy);
    }

    private static Map<String, List<Consumer<?>>> copyOutputs(Map<String, List<Consumer<?>>> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return Map.of();
        }
        Map<String, List<Consumer<?>>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<Consumer<?>>> entry : outputs.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("output names must not be null or blank");
            }
            List<Consumer<?>> consumers = entry.getValue();
            if (consumers == null) {
                throw new NullPointerException("output '" + entry.getKey() + "' must not have null listeners");
            }
            for (Consumer<?> consumer : consumers) {
                Objects.requireNonNull(consumer, "output listeners must not be null");
            }
            copy.put(entry.getKey(), List.copyOf(consumers));
        }
        return Map.copyOf(copy);
    }
}
