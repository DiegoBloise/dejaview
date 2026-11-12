package io.dejaview.core;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The bindings a renderer pushes into a mounted component before each render.
 *
 * <p>Bindings carry everything the parent scope attached to a component
 * view: raw input values, the projected content for each slot and the
 * consumers registered for each output. They are rebuilt from the view on
 * every render pass, so a component always sees the bindings of the view
 * that triggered the current pass.</p>
 *
 * <p>Instances are immutable. {@link #empty()} returns a shared instance
 * suitable for components rendered with no bindings.</p>
 *
 * @param inputs  input name to value, never contains {@code null} values
 * @param slots   slot name to projected views; the empty string is the default slot
 * @param outputs output name to consumers in registration order
 */
public record ComponentBindings(
        Map<String, Object> inputs,
        Map<String, List<View>> slots,
        Map<String, List<Consumer<?>>> outputs) {

    /**
     * Slot name used for projected content that is not assigned to a named
     * slot.
     */
    public static final String DEFAULT_SLOT = "";

    private static final ComponentBindings EMPTY =
            new ComponentBindings(Map.of(), Map.of(), Map.of());

    public ComponentBindings {
        inputs = copyInputs(inputs);
        slots = copySlots(slots);
        outputs = copyOutputs(outputs);
    }

    /**
     * Returns a bindings instance with no inputs, slots or outputs.
     */
    public static ComponentBindings empty() {
        return EMPTY;
    }

    private static Map<String, Object> copyInputs(Map<String, Object> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return Map.of();
        }
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new NullPointerException("inputs must not contain null keys or values");
            }
        }
        return Map.copyOf(inputs);
    }

    private static Map<String, List<View>> copySlots(Map<String, List<View>> slots) {
        if (slots == null || slots.isEmpty()) {
            return Map.of();
        }
        Map<String, List<View>> copy = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, List<View>> entry : slots.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new NullPointerException("slots must not contain null keys or values");
            }
            for (View view : entry.getValue()) {
                if (view == null) {
                    throw new NullPointerException("slots must not contain null views");
                }
            }
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    private static Map<String, List<Consumer<?>>> copyOutputs(Map<String, List<Consumer<?>>> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return Map.of();
        }
        Map<String, List<Consumer<?>>> copy = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, List<Consumer<?>>> entry : outputs.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new NullPointerException("outputs must not contain null keys or values");
            }
            for (Consumer<?> consumer : entry.getValue()) {
                if (consumer == null) {
                    throw new NullPointerException("outputs must not contain null consumers");
                }
            }
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }
}
