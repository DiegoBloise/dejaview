package io.dejaview.forms;

import io.dejaview.reactivity.Reactive;
import io.dejaview.reactivity.Signal;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A form control that owns a single value.
 *
 * <p>Values are never {@code null}; use the type's empty value instead
 * ({@code ""} for text, {@code false} for checkboxes, {@code 0} for numbers).</p>
 *
 * <pre>{@code
 * FormControl<String> email = new FormControl<>(
 *     "",
 *     Validators.required(),
 *     Validators.email());
 * }</pre>
 *
 * @param <V> value type
 */
public final class FormControl<V> extends AbstractControl<V> {

    private final V initialValue;
    private final Signal<V> valueSignal;

    /**
     * Creates a control with an initial value and no validators.
     *
     * @throws NullPointerException if {@code initialValue} is {@code null}
     */
    public FormControl(V initialValue) {
        this(initialValue, List.of());
    }

    /**
     * Creates a control with an initial value and synchronous validators.
     *
     * @throws NullPointerException if {@code initialValue} is {@code null}
     */
    @SafeVarargs
    public FormControl(V initialValue, Validator<V>... validators) {
        this(initialValue, List.of(validators));
    }

    /**
     * Creates a control with an initial value and synchronous validators.
     *
     * @throws NullPointerException if {@code initialValue} is {@code null}
     */
    public FormControl(V initialValue, Collection<? extends Validator<V>> validators) {
        this.initialValue = Objects.requireNonNull(initialValue, "initialValue");
        this.valueSignal = new Signal<>(initialValue);
        setValidators(validators);
        initialize();
    }

    @Override
    public V value() {
        return valueSignal.get();
    }

    @Override
    public void setValue(V value) {
        Objects.requireNonNull(value, "value");
        Reactive.batch(() -> {
            valueSignal.set(value);
            markDirtyInternal();
        });
    }

    /**
     * Returns the value supplied at construction, used by {@link #reset()}.
     */
    public V initialValue() {
        return initialValue;
    }

    @Override
    public void reset() {
        Reactive.batch(() -> {
            valueSignal.set(initialValue);
            clearOwnInteractionState();
            requestValidation();
        });
    }
}
