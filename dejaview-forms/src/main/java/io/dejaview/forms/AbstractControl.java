package io.dejaview.forms;

import io.dejaview.reactivity.Reactive;
import io.dejaview.reactivity.ReactiveScope;
import io.dejaview.reactivity.Signal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Base class for every form control.
 *
 * <p>A control owns a value, its validation state and the user interaction
 * flags ({@code touched}, {@code dirty} and {@code disabled}). Reading any of
 * them inside a {@link io.dejaview.reactivity.Effect} or a render function
 * tracks it reactively. Writing one of them outside an effect notifies
 * observers synchronously.</p>
 *
 * <p>Validation runs automatically when the value or the validator list
 * changes. Synchronous validators run first; asynchronous validators only run
 * when the synchronous result is valid. A control is {@link ControlStatus#PENDING}
 * while at least one asynchronous validator is in flight.</p>
 *
 * <p>The value of a control is never {@code null}. Use the type's empty value
 * ({@code ""}, {@code 0}, {@code false}) to represent "not filled in".</p>
 *
 * <p>Controls are single-threaded and follow the same model as signals. Call
 * {@link #dispose()} when a control is permanently discarded; this stops its
 * internal validation effect.</p>
 *
 * @param <V> value type
 */
public abstract class AbstractControl<V> implements AutoCloseable {

    private final ReactiveScope scope = new ReactiveScope();
    private final Signal<ValidationErrors> ownErrors = new Signal<>(ValidationErrors.none());
    private final Signal<Boolean> pending = new Signal<>(false);
    private final Signal<Boolean> touched = new Signal<>(false);
    private final Signal<Boolean> dirty = new Signal<>(false);
    private final Signal<Boolean> disabled = new Signal<>(false);
    private final Signal<Integer> validationTick = new Signal<>(0);
    private final List<Validator<V>> validators = new ArrayList<>();
    private final List<AsyncValidator<V>> asyncValidators = new ArrayList<>();

    private AbstractControl<?> parent;
    private boolean initialized;
    private boolean disposed;
    private int asyncGeneration;

    protected AbstractControl() {
    }

    // ---------------------------------------------------------------------
    // Value and state
    // ---------------------------------------------------------------------

    /**
     * Returns the current value and registers a reactive dependency.
     */
    public abstract V value();

    /**
     * Replaces the value and re-runs validation.
     *
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public abstract void setValue(V value);

    /**
     * Returns the aggregated validation errors of this control.
     */
    public ValidationErrors errors() {
        return ownErrors.get();
    }

    /**
     * Returns the derived lifecycle state.
     */
    public final ControlStatus status() {
        if (isDisabled()) {
            return ControlStatus.DISABLED;
        }
        if (pending.get()) {
            return ControlStatus.PENDING;
        }
        if (!ownErrors.get().isEmpty() || hasInvalidDescendant()) {
            return ControlStatus.INVALID;
        }
        if (hasPendingDescendant()) {
            return ControlStatus.PENDING;
        }
        return ControlStatus.VALID;
    }

    /**
     * Returns whether an asynchronous validation is in flight.
     */
    public final boolean pending() {
        return pending.get() || hasPendingDescendant();
    }

    /**
     * Returns whether this control or one of its ancestors is disabled.
     */
    public final boolean isDisabled() {
        return disabled.get() || (parent != null && parent.isDisabled());
    }

    /**
     * Returns whether the user has visited this control or any descendant.
     */
    public final boolean touched() {
        return touched.get() || hasTouchedDescendant();
    }

    /**
     * Returns whether the value has changed since creation or reset.
     */
    public final boolean dirty() {
        return dirty.get() || hasDirtyDescendant();
    }

    // ---------------------------------------------------------------------
    // User interaction flags
    // ---------------------------------------------------------------------

    /**
     * Marks this control as visited.
     */
    public void markAsTouched() {
        touched.set(true);
    }

    /**
     * Clears the visited flag of this control and its descendants.
     */
    public void markAsUntouched() {
        touched.set(false);
        clearDescendantTouched();
    }

    /**
     * Marks this control and every descendant as visited.
     */
    public final void markAllAsTouched() {
        markAsTouched();
        markDescendantsTouched();
    }

    /**
     * Marks this control as modified.
     */
    public void markAsDirty() {
        dirty.set(true);
    }

    /**
     * Clears the modified flag of this control and its descendants.
     */
    public void markAsPristine() {
        dirty.set(false);
        clearDescendantDirty();
    }

    /**
     * Disables this control. Ancestor disabling is inherited automatically.
     */
    public void disable() {
        disabled.set(true);
    }

    /**
     * Enables this control.
     */
    public void enable() {
        disabled.set(false);
    }

    // ---------------------------------------------------------------------
    // Validators
    // ---------------------------------------------------------------------

    /**
     * Replaces the synchronous validators and re-runs validation.
     */
    public final void setValidators(Collection<? extends Validator<V>> next) {
        Objects.requireNonNull(next, "validators");
        Reactive.batch(() -> {
            validators.clear();
            validators.addAll(next);
            validationTick.update(tick -> tick + 1);
        });
    }

    /**
     * Adds a synchronous validator and re-runs validation.
     */
    public final void addValidator(Validator<V> validator) {
        Objects.requireNonNull(validator, "validator");
        Reactive.batch(() -> {
            validators.add(validator);
            validationTick.update(tick -> tick + 1);
        });
    }

    /**
     * Returns the synchronous validators in order.
     */
    public final List<Validator<V>> validators() {
        return List.copyOf(validators);
    }

    /**
     * Replaces the asynchronous validators and re-runs validation.
     */
    public final void setAsyncValidators(Collection<? extends AsyncValidator<V>> next) {
        Objects.requireNonNull(next, "asyncValidators");
        Reactive.batch(() -> {
            asyncValidators.clear();
            asyncValidators.addAll(next);
            validationTick.update(tick -> tick + 1);
        });
    }

    /**
     * Adds an asynchronous validator and re-runs validation.
     */
    public final void addAsyncValidator(AsyncValidator<V> validator) {
        Objects.requireNonNull(validator, "asyncValidator");
        Reactive.batch(() -> {
            asyncValidators.add(validator);
            validationTick.update(tick -> tick + 1);
        });
    }

    /**
     * Returns the asynchronous validators in order.
     */
    public final List<AsyncValidator<V>> asyncValidators() {
        return List.copyOf(asyncValidators);
    }

    /**
     * Forces synchronous validation to run again. This is rarely needed
     * because value and validator changes already trigger it.
     */
    public final void updateValueAndValidity() {
        ensureActive();
        validationTick.update(tick -> tick + 1);
    }

    // ---------------------------------------------------------------------
    // Submission and lifecycle
    // ---------------------------------------------------------------------

    /**
     * Marks the control tree as visited, revalidates and returns a snapshot.
     */
    public final FormSubmission<V> submit() {
        markAllAsTouched();
        updateValueAndValidity();
        return new FormSubmission<>(
                status() == ControlStatus.VALID,
                value(),
                errors());
    }

    /**
     * Restores the initial value and clears the interaction flags, recursively.
     */
    public abstract void reset();

    /**
     * Stops the internal validation effect. Calling this method more than once
     * has no additional effect.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        asyncGeneration++;
        pending.set(false);
        disposeDescendants();
        scope.dispose();
    }

    /**
     * Returns whether this control has been disposed.
     */
    public final boolean isDisposed() {
        return disposed;
    }

    @Override
    public final void close() {
        dispose();
    }

    // ---------------------------------------------------------------------
    // Package/protected hooks for subclasses
    // ---------------------------------------------------------------------

    /**
     * Starts the internal validation effect. Subclasses call this once after
     * their fields are initialized.
     */
    protected final void initialize() {
        if (initialized) {
            throw new IllegalStateException("Control is already initialized");
        }
        if (disposed) {
            throw new IllegalStateException("Control is disposed");
        }
        initialized = true;
        scope.effect(this::runValidation);
    }

    /**
     * Assigns the parent used for disable inheritance.
     */
    final void setParent(AbstractControl<?> newParent) {
        this.parent = newParent;
    }

    /**
     * Assigns a value to a child without exposing the unchecked cast at call
     * sites.
     */
    @SuppressWarnings("unchecked")
    protected static void assign(AbstractControl<?> control, Object value) {
        ((AbstractControl<Object>) control).setValue(value);
    }

    /**
     * Returns whether any descendant is invalid.
     */
    protected boolean hasInvalidDescendant() {
        return false;
    }

    /**
     * Returns whether any descendant is pending.
     */
    protected boolean hasPendingDescendant() {
        return false;
    }

    /**
     * Returns whether any descendant is touched.
     */
    protected boolean hasTouchedDescendant() {
        return false;
    }

    /**
     * Returns whether any descendant is dirty.
     */
    protected boolean hasDirtyDescendant() {
        return false;
    }

    /**
     * Marks descendants as touched.
     */
    protected void markDescendantsTouched() {
    }

    /**
     * Clears the touched flag on descendants.
     */
    protected void clearDescendantTouched() {
    }

    /**
     * Clears the dirty flag on descendants.
     */
    protected void clearDescendantDirty() {
    }

    /**
     * Disposes descendant controls.
     */
    protected void disposeDescendants() {
    }

    /**
     * Marks the control dirty without notifying twice.
     */
    protected final void markDirtyInternal() {
        dirty.set(true);
    }

    /**
     * Clears this control's own touched and dirty flags.
     */
    protected final void clearOwnInteractionState() {
        touched.set(false);
        dirty.set(false);
    }

    /**
     * Triggers validation from a subclass reset without entering another
     * public batch.
     */
    protected final void requestValidation() {
        validationTick.update(tick -> tick + 1);
    }

    // ---------------------------------------------------------------------
    // Validation implementation
    // ---------------------------------------------------------------------

    private void runValidation() {
        validationTick.get();
        V current = value();

        ValidationErrors result = ValidationErrors.none();
        for (Validator<V> validator : List.copyOf(validators)) {
            result = result.merge(Objects.requireNonNull(
                    validator.validate(current), "validator result"));
        }
        ownErrors.set(result);
        runAsyncValidation(current, result);
    }

    private void runAsyncValidation(V current, ValidationErrors syncErrors) {
        int generation = ++asyncGeneration;
        List<AsyncValidator<V>> snapshot = List.copyOf(asyncValidators);
        if (snapshot.isEmpty() || !syncErrors.isEmpty()) {
            pending.set(false);
            return;
        }

        pending.set(true);
        AsyncAccumulator accumulator = new AsyncAccumulator(generation, snapshot.size());
        for (AsyncValidator<V> validator : snapshot) {
            validator.validate(current, accumulator::complete);
        }
    }

    private void ensureActive() {
        if (disposed) {
            throw new IllegalStateException("Control is disposed");
        }
    }

    /**
     * Collects results from the asynchronous validators of one validation run.
     * Callbacks from superseded runs are ignored.
     */
    private final class AsyncAccumulator {

        private final int generation;
        private final ValidationErrors[] collected = {ValidationErrors.none()};
        private int remaining;

        AsyncAccumulator(int generation, int count) {
            this.generation = generation;
            this.remaining = count;
        }

        void complete(ValidationErrors errors) {
            if (disposed || generation != asyncGeneration || remaining == 0) {
                return;
            }
            Objects.requireNonNull(errors, "async validator result");
            collected[0] = collected[0].merge(errors);
            remaining--;
            if (remaining == 0) {
                ownErrors.set(collected[0]);
                pending.set(false);
            }
        }
    }
}
