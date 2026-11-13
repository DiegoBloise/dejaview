package io.dejaview.forms;

import io.dejaview.reactivity.Reactive;
import io.dejaview.reactivity.Signal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A named group of controls.
 *
 * <p>The group value is an immutable, ordered map from control name to control
 * value. Disabled children are omitted, matching the usual HTML form rule that
 * a disabled input is not submitted.</p>
 *
 * <p>Children can be added or removed at runtime. Groups expose aggregated
 * errors prefixed with the child name ({@code "email.required"}) and aggregate
 * the status, touched and dirty flags of their children.</p>
 *
 * <pre>{@code
 * FormGroup form = new FormGroup(Map.of(
 *     "email", new FormControl<>("", Validators.required(), Validators.email()),
 *     "age", new FormControl<>(0, Validators.min(18))));
 * }</pre>
 */
public final class FormGroup extends AbstractControl<Map<String, Object>> {

    private final Map<String, AbstractControl<?>> controls = new LinkedHashMap<>();
    private final Signal<List<String>> structure = new Signal<>(List.of());

    /**
     * Creates an empty group.
     */
    public FormGroup() {
        initialize();
    }

    /**
     * Creates a group with the given children, in iteration order.
     */
    public FormGroup(Map<String, ? extends AbstractControl<?>> controls) {
        Objects.requireNonNull(controls, "controls");
        for (Map.Entry<String, ? extends AbstractControl<?>> entry : controls.entrySet()) {
            addControl(entry.getKey(), entry.getValue());
        }
        initialize();
    }

    /**
     * Returns the group value, excluding disabled children.
     */
    @Override
    public Map<String, Object> value() {
        structure.get();
        Map<String, Object> value = new LinkedHashMap<>();
        for (Map.Entry<String, AbstractControl<?>> entry : controls.entrySet()) {
            if (!entry.getValue().isDisabled()) {
                value.put(entry.getKey(), entry.getValue().value());
            }
        }
        return Collections.unmodifiableMap(value);
    }

    /**
     * Applies a value to the group. Every enabled control must have a matching
     * key; unknown keys are rejected.
     */
    @Override
    public void setValue(Map<String, Object> value) {
        Objects.requireNonNull(value, "value");
        structure.get();

        for (String name : value.keySet()) {
            if (!controls.containsKey(name)) {
                throw new IllegalArgumentException("Unknown control: " + name);
            }
        }
        for (Map.Entry<String, AbstractControl<?>> entry : controls.entrySet()) {
            if (!entry.getValue().isDisabled() && !value.containsKey(entry.getKey())) {
                throw new IllegalArgumentException(
                        "Missing value for control: " + entry.getKey());
            }
        }

        Reactive.batch(() -> {
            for (Map.Entry<String, AbstractControl<?>> entry : controls.entrySet()) {
                AbstractControl<?> child = entry.getValue();
                if (!child.isDisabled()) {
                    assign(child, value.get(entry.getKey()));
                }
            }
            markDirtyInternal();
            requestValidation();
        });
    }

    /**
     * Returns the group's own errors followed by the prefixed child errors.
     */
    @Override
    public ValidationErrors errors() {
        structure.get();
        ValidationErrors result = super.errors();
        for (Map.Entry<String, AbstractControl<?>> entry : controls.entrySet()) {
            result = result.merge(entry.getValue().errors().prefix(entry.getKey()));
        }
        return result;
    }

    /**
     * Adds a child control.
     *
     * @return this group, for chaining
     * @throws IllegalArgumentException if the name is already used
     */
    public FormGroup addControl(String name, AbstractControl<?> control) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(control, "control");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Control name must not be blank");
        }
        if (controls.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate control: " + name);
        }
        if (control == this) {
            throw new IllegalArgumentException("A group cannot contain itself");
        }

        controls.put(name, control);
        control.setParent(this);
        structure.update(names -> {
            List<String> updated = new ArrayList<>(names);
            updated.add(name);
            return List.copyOf(updated);
        });
        return this;
    }

    /**
     * Removes a child control and returns it, or {@code null} when absent.
     */
    public AbstractControl<?> removeControl(String name) {
        Objects.requireNonNull(name, "name");
        AbstractControl<?> removed = controls.remove(name);
        if (removed == null) {
            return null;
        }
        removed.setParent(null);
        structure.update(names -> {
            List<String> updated = new ArrayList<>(names);
            updated.remove(name);
            return List.copyOf(updated);
        });
        return removed;
    }

    /**
     * Returns a child control.
     *
     * @throws IllegalArgumentException if no control uses {@code name}
     */
    public AbstractControl<?> control(String name) {
        AbstractControl<?> control = controls.get(name);
        if (control == null) {
            throw new IllegalArgumentException("Unknown control: " + name);
        }
        return control;
    }

    /**
     * Returns the child names in insertion order.
     */
    public Set<String> controlNames() {
        structure.get();
        return Collections.unmodifiableSet(new LinkedHashSet<>(controls.keySet()));
    }

    /**
     * Returns the child controls in insertion order.
     */
    public Map<String, AbstractControl<?>> controls() {
        structure.get();
        return Collections.unmodifiableMap(new LinkedHashMap<>(controls));
    }

    @Override
    public void reset() {
        Reactive.batch(() -> {
            clearOwnInteractionState();
            for (AbstractControl<?> control : controls.values()) {
                control.reset();
            }
            requestValidation();
        });
    }

    @Override
    protected boolean hasInvalidDescendant() {
        structure.get();
        for (AbstractControl<?> control : controls.values()) {
            if (control.status() == ControlStatus.INVALID) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean hasPendingDescendant() {
        structure.get();
        for (AbstractControl<?> control : controls.values()) {
            if (control.pending()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean hasTouchedDescendant() {
        structure.get();
        for (AbstractControl<?> control : controls.values()) {
            if (control.touched()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean hasDirtyDescendant() {
        structure.get();
        for (AbstractControl<?> control : controls.values()) {
            if (control.dirty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void markDescendantsTouched() {
        for (AbstractControl<?> control : List.copyOf(controls.values())) {
            control.markAllAsTouched();
        }
    }

    @Override
    protected void clearDescendantTouched() {
        for (AbstractControl<?> control : List.copyOf(controls.values())) {
            control.markAsUntouched();
        }
    }

    @Override
    protected void clearDescendantDirty() {
        for (AbstractControl<?> control : List.copyOf(controls.values())) {
            control.markAsPristine();
        }
    }

    @Override
    protected void disposeDescendants() {
        for (AbstractControl<?> control : List.copyOf(controls.values())) {
            control.dispose();
        }
    }
}
