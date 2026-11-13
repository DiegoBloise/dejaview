package io.dejaview.forms;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormControlTest {

    @Test
    void startsWithTheInitialValueAndValidStatus() {
        FormControl<String> control = new FormControl<>("initial");

        assertEquals("initial", control.value());
        assertEquals(ControlStatus.VALID, control.status());
        assertTrue(control.errors().isEmpty());
        assertFalse(control.touched());
        assertFalse(control.dirty());
        assertFalse(control.isDisabled());
        assertFalse(control.pending());
    }

    @Test
    void setValueUpdatesValueAndMarksDirty() {
        FormControl<String> control = new FormControl<>("a");

        control.setValue("b");

        assertEquals("b", control.value());
        assertTrue(control.dirty());
        assertTrue(control.errors().isEmpty());
    }

    @Test
    void rejectsNullValues() {
        FormControl<String> control = new FormControl<>("a");

        assertThrows(NullPointerException.class, () -> control.setValue(null));
        assertThrows(NullPointerException.class, () -> new FormControl<String>(null));
    }

    @Test
    void synchronousValidatorsProduceErrors() {
        FormControl<String> control = new FormControl<>(
                "", Validators.required(), Validators.minLength(3));

        assertEquals(ControlStatus.INVALID, control.status());
        assertTrue(control.errors().has("required"));

        control.setValue("ab");
        assertEquals(ControlStatus.INVALID, control.status());
        assertFalse(control.errors().has("required"));
        assertTrue(control.errors().has("minLength"));

        control.setValue("abc");
        assertEquals(ControlStatus.VALID, control.status());
        assertTrue(control.errors().isEmpty());
    }

    @Test
    void validatorsCanBeReplacedAtRuntime() {
        FormControl<String> control = new FormControl<>("abc", Validators.required());
        assertEquals(ControlStatus.VALID, control.status());

        control.setValidators(List.of(Validators.minLength(5)));

        assertEquals(ControlStatus.INVALID, control.status());
        assertTrue(control.errors().has("minLength"));
    }

    @Test
    void touchedAndDirtyFlagsCanBeSetAndCleared() {
        FormControl<String> control = new FormControl<>("a");

        control.markAsTouched();
        control.markAsDirty();
        assertTrue(control.touched());
        assertTrue(control.dirty());

        control.markAsUntouched();
        control.markAsPristine();
        assertFalse(control.touched());
        assertFalse(control.dirty());
    }

    @Test
    void disabledControlReportsDisabledStatus() {
        FormControl<String> control = new FormControl<>("", Validators.required());

        control.disable();
        assertTrue(control.isDisabled());
        assertEquals(ControlStatus.DISABLED, control.status());

        control.enable();
        assertFalse(control.isDisabled());
        assertEquals(ControlStatus.INVALID, control.status());
    }

    @Test
    void asyncValidatorMarksControlPendingUntilCompletion() {
        AtomicReference<ValidationCallback> callback = new AtomicReference<>();
        FormControl<String> control = new FormControl<>("taken");
        control.addAsyncValidator((value, result) -> callback.set(result));

        assertEquals(ControlStatus.PENDING, control.status());
        assertTrue(control.pending());

        callback.get().complete(ValidationErrors.of("taken"));

        assertEquals(ControlStatus.INVALID, control.status());
        assertFalse(control.pending());
        assertTrue(control.errors().has("taken"));
    }

    @Test
    void asyncValidatorsAreSkippedWhenSynchronousValidationFails() {
        AtomicReference<ValidationCallback> callback = new AtomicReference<>();
        FormControl<String> control = new FormControl<>("", Validators.required());
        control.addAsyncValidator((value, result) -> callback.set(result));

        assertEquals(ControlStatus.INVALID, control.status());
        assertFalse(control.pending());
        assertEquals(null, callback.get());
    }

    @Test
    void staleAsyncResultsAreIgnored() {
        AtomicReference<ValidationCallback> callback = new AtomicReference<>();
        FormControl<String> control = new FormControl<>("a");
        control.addAsyncValidator((value, result) -> callback.set(result));

        ValidationCallback first = callback.get();
        control.setValue("b");
        ValidationCallback second = callback.get();

        first.complete(ValidationErrors.of("stale"));

        assertEquals(ControlStatus.PENDING, control.status());
        assertFalse(control.errors().has("stale"));

        second.complete(ValidationErrors.none());

        assertEquals(ControlStatus.VALID, control.status());
        assertTrue(control.errors().isEmpty());
    }

    @Test
    void multipleAsyncValidatorsMustAllComplete() {
        AtomicReference<ValidationCallback> first = new AtomicReference<>();
        AtomicReference<ValidationCallback> second = new AtomicReference<>();
        FormControl<String> control = new FormControl<>("a");
        control.addAsyncValidator((value, result) -> first.set(result));
        control.addAsyncValidator((value, result) -> second.set(result));

        first.get().complete(ValidationErrors.none());
        assertEquals(ControlStatus.PENDING, control.status());

        second.get().complete(ValidationErrors.of("second"));
        assertEquals(ControlStatus.INVALID, control.status());
        assertTrue(control.errors().has("second"));
    }

    @Test
    void resetRestoresTheInitialValueAndClearsState() {
        FormControl<String> control = new FormControl<>("initial", Validators.required());
        control.setValue("changed");
        control.markAsTouched();
        control.disable();

        control.reset();

        assertEquals("initial", control.value());
        assertFalse(control.touched());
        assertFalse(control.dirty());
        assertEquals(ControlStatus.DISABLED, control.status());

        control.enable();
        assertEquals(ControlStatus.VALID, control.status());
    }

    @Test
    void submitMarksTheControlTouchedAndSnapshotsTheValue() {
        FormControl<String> control = new FormControl<>("", Validators.required());

        FormSubmission<String> submission = control.submit();

        assertFalse(submission.valid());
        assertEquals("", submission.value());
        assertTrue(submission.errors().has("required"));
        assertTrue(control.touched());
    }

    @Test
    void disposedControlsIgnoreLateAsyncResults() {
        AtomicReference<ValidationCallback> callback = new AtomicReference<>();
        FormControl<String> control = new FormControl<>("a");
        control.addAsyncValidator((value, result) -> callback.set(result));

        control.dispose();
        callback.get().complete(ValidationErrors.of("late"));

        assertTrue(control.isDisposed());
        assertEquals(ControlStatus.VALID, control.status());
        assertThrows(IllegalStateException.class, control::updateValueAndValidity);
    }
}
