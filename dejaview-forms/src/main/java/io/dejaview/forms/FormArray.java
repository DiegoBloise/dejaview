package io.dejaview.forms;

import io.dejaview.reactivity.Reactive;
import io.dejaview.reactivity.Signal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An ordered list of controls.
 *
 * <p>The array value is an immutable list of child values, excluding disabled
 * children. Errors are prefixed with the child index
 * ({@code "0.required"}).</p>
 *
 * <p>Rows can be added and removed at runtime, which is the building block for
 * "add another item" style forms.</p>
 *
 * <pre>{@code
 * FormArray lines = new FormArray()
 *     .push(new FormControl<>("", Validators.required()));
 * }</pre>
 */
public final class FormArray extends AbstractControl<List<Object>> {

    private final List<AbstractControl<?>> controls = new ArrayList<>();
    private final Signal<Integer> structure = new Signal<>(0);

    /**
     * Creates an empty array.
     */
    public FormArray() {
        initialize();
    }

    /**
     * Creates an array with the given children, in order.
     */
    public FormArray(List<? extends AbstractControl<?>> controls) {
        Objects.requireNonNull(controls, "controls");
        for (AbstractControl<?> control : controls) {
            add(control);
        }
        initialize();
    }

    /**
     * Returns the array value, excluding disabled children.
     */
    @Override
    public List<Object> value() {
        structure.get();
        List<Object> value = new ArrayList<>(controls.size());
        for (AbstractControl<?> control : controls) {
            if (!control.isDisabled()) {
                value.add(control.value());
            }
        }
        return List.copyOf(value);
    }

    /**
     * Applies a value by position. The list size must match the array size.
     */
    @Override
    public void setValue(List<Object> value) {
        Objects.requireNonNull(value, "value");
        structure.get();
        if (value.size() != controls.size()) {
            throw new IllegalArgumentException(
                    "Expected " + controls.size() + " values but received " + value.size());
        }

        Reactive.batch(() -> {
            for (int index = 0; index < controls.size(); index++) {
                AbstractControl<?> child = controls.get(index);
                if (!child.isDisabled()) {
                    assign(child, value.get(index));
                }
            }
            markDirtyInternal();
            requestValidation();
        });
    }

    /**
     * Returns the array's own errors followed by the indexed child errors.
     */
    @Override
    public ValidationErrors errors() {
        structure.get();
        ValidationErrors result = super.errors();
        for (int index = 0; index < controls.size(); index++) {
            result = result.merge(controls.get(index).errors().prefix(Integer.toString(index)));
        }
        return result;
    }

    /**
     * Appends a control.
     *
     * @return this array, for chaining
     */
    public FormArray add(AbstractControl<?> control) {
        Objects.requireNonNull(control, "control");
        if (control == this) {
            throw new IllegalArgumentException("An array cannot contain itself");
        }
        controls.add(control);
        control.setParent(this);
        bumpStructure();
        return this;
    }

    /**
     * Inserts a control at {@code index}.
     *
     * @return this array, for chaining
     */
    public FormArray insert(int index, AbstractControl<?> control) {
        Objects.requireNonNull(control, "control");
        if (control == this) {
            throw new IllegalArgumentException("An array cannot contain itself");
        }
        controls.add(index, control);
        control.setParent(this);
        bumpStructure();
        return this;
    }

    /**
     * Removes the control at {@code index} and detaches it.
     *
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    public AbstractControl<?> removeAt(int index) {
        AbstractControl<?> removed = controls.remove(index);
        removed.setParent(null);
        bumpStructure();
        return removed;
    }

    /**
     * Returns the control at {@code index}.
     *
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    public AbstractControl<?> at(int index) {
        structure.get();
        return controls.get(index);
    }

    /**
     * Returns the number of controls.
     */
    public int size() {
        structure.get();
        return controls.size();
    }

    /**
     * Returns an immutable snapshot of the controls.
     */
    public List<AbstractControl<?>> controls() {
        structure.get();
        return List.copyOf(controls);
    }

    @Override
    public void reset() {
        Reactive.batch(() -> {
            clearOwnInteractionState();
            for (AbstractControl<?> control : controls) {
                control.reset();
            }
            requestValidation();
        });
    }

    @Override
    protected boolean hasInvalidDescendant() {
        structure.get();
        for (AbstractControl<?> control : controls) {
            if (control.status() == ControlStatus.INVALID) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean hasPendingDescendant() {
        structure.get();
        for (AbstractControl<?> control : controls) {
            if (control.pending()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean hasTouchedDescendant() {
        structure.get();
        for (AbstractControl<?> control : controls) {
            if (control.touched()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean hasDirtyDescendant() {
        structure.get();
        for (AbstractControl<?> control : controls) {
            if (control.dirty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void markDescendantsTouched() {
        for (AbstractControl<?> control : List.copyOf(controls)) {
            control.markAllAsTouched();
        }
    }

    @Override
    protected void clearDescendantTouched() {
        for (AbstractControl<?> control : List.copyOf(controls)) {
            control.markAsUntouched();
        }
    }

    @Override
    protected void clearDescendantDirty() {
        for (AbstractControl<?> control : List.copyOf(controls)) {
            control.markAsPristine();
        }
    }

    @Override
    protected void disposeDescendants() {
        for (AbstractControl<?> control : List.copyOf(controls)) {
            control.dispose();
        }
    }

    private void bumpStructure() {
        structure.update(version -> version + 1);
    }
}
