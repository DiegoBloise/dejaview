package io.dejaview.forms;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormArrayTest {

    @Test
    void aggregatesValuesInOrder() {
        FormArray array = new FormArray()
                .add(new FormControl<>("a"))
                .add(new FormControl<>("b"));

        assertEquals(List.of("a", "b"), array.value());
        assertEquals(2, array.size());
        assertEquals(ControlStatus.VALID, array.status());
    }

    @Test
    void insertAndRemoveKeepPositionsConsistent() {
        FormControl<String> first = new FormControl<>("a");
        FormControl<String> second = new FormControl<>("c");
        FormArray array = new FormArray(List.of(first, second));

        FormControl<String> middle = new FormControl<>("b");
        array.insert(1, middle);

        assertEquals(List.of("a", "b", "c"), array.value());
        assertSame(middle, array.at(1));

        array.removeAt(1);

        assertEquals(List.of("a", "c"), array.value());
        assertEquals(2, array.size());
    }

    @Test
    void setValueAppliesValuesByPosition() {
        FormControl<String> first = new FormControl<>("a");
        FormControl<String> second = new FormControl<>("b");
        FormArray array = new FormArray(List.of(first, second));

        array.setValue(List.of("x", "y"));

        assertEquals("x", first.value());
        assertEquals("y", second.value());
        assertEquals(List.of("x", "y"), array.value());
        assertTrue(array.dirty());
    }

    @Test
    void setValueRequiresTheSameNumberOfValues() {
        FormArray array = new FormArray(List.of(new FormControl<>("a")));

        assertThrows(IllegalArgumentException.class, () -> array.setValue(List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> array.setValue(List.of("a", "b")));
    }

    @Test
    void prefixesErrorsWithTheIndex() {
        FormArray array = new FormArray()
                .add(new FormControl<>("ok"))
                .add(new FormControl<>("", Validators.required()));

        assertEquals(ControlStatus.INVALID, array.status());
        assertTrue(array.errors().has("1.required"));
        assertFalse(array.errors().has("0.required"));
    }

    @Test
    void disabledRowsAreExcludedFromTheValueAndStatus() {
        FormControl<String> first = new FormControl<>("a");
        FormControl<String> second = new FormControl<>("b");
        FormArray array = new FormArray(List.of(first, second));

        second.disable();

        assertEquals(List.of("a"), array.value());
        assertEquals(ControlStatus.VALID, array.status());
        assertEquals(ControlStatus.DISABLED, second.status());
    }

    @Test
    void markAllAsTouchedAndResetReachEveryRow() {
        FormControl<String> first = new FormControl<>("a");
        FormControl<String> second = new FormControl<>("b");
        FormArray array = new FormArray(List.of(first, second));

        array.markAllAsTouched();
        first.setValue("changed");

        assertTrue(first.touched());
        assertTrue(second.touched());
        assertTrue(array.touched());
        assertTrue(array.dirty());

        array.reset();

        assertEquals(List.of("a", "b"), array.value());
        assertFalse(array.touched());
        assertFalse(array.dirty());
    }

    @Test
    void arrayValidatorsReceiveTheAggregatedList() {
        FormArray array = new FormArray()
                .add(new FormControl<>("a"))
                .add(new FormControl<>("b"));
        array.addValidator(values -> values.size() >= 2
                ? ValidationErrors.none()
                : ValidationErrors.of("tooFew"));

        assertEquals(ControlStatus.VALID, array.status());

        array.removeAt(0);

        assertEquals(ControlStatus.INVALID, array.status());
        assertTrue(array.errors().has("tooFew"));
    }

    @Test
    void submitSnapshotsTheValuesAndErrors() {
        FormArray array = new FormArray()
                .add(new FormControl<>("a"))
                .add(new FormControl<>("", Validators.required()));

        FormSubmission<List<Object>> submission = array.submit();

        assertFalse(submission.valid());
        assertEquals(List.of("a", ""), submission.value());
        assertTrue(submission.errors().has("1.required"));
    }
}
