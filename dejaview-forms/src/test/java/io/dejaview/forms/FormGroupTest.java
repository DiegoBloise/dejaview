package io.dejaview.forms;

import io.dejaview.reactivity.Effect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormGroupTest {

    @Test
    void aggregatesChildValuesInInsertionOrder() {
        FormGroup group = new FormGroup()
                .addControl("first", new FormControl<>("one"))
                .addControl("second", new FormControl<>("two"));

        assertEquals(List.of("first", "second"), List.copyOf(group.value().keySet()));
        assertEquals("one", group.value().get("first"));
        assertEquals("two", group.value().get("second"));
        assertEquals(ControlStatus.VALID, group.status());
    }

    @Test
    void setValuePropagatesToChildrenAndMarksTheGroupDirty() {
        FormControl<String> first = new FormControl<>("one");
        FormControl<String> second = new FormControl<>("two");
        FormGroup group = new FormGroup(Map.of("first", first, "second", second));

        group.setValue(Map.of("first", "uno", "second", "dos"));

        assertEquals("uno", first.value());
        assertEquals("dos", second.value());
        assertEquals("uno", group.value().get("first"));
        assertTrue(group.dirty());
        assertTrue(first.dirty());
    }

    @Test
    void setValueRejectsUnknownAndMissingKeys() {
        FormGroup group = new FormGroup(Map.of("first", new FormControl<>("one")));

        assertThrows(IllegalArgumentException.class,
                () -> group.setValue(Map.of("unknown", "x")));
        assertThrows(IllegalArgumentException.class,
                () -> group.setValue(Map.of()));
    }

    @Test
    void aggregatesAndPrefixesChildErrors() {
        FormGroup group = new FormGroup(Map.of(
                "email", new FormControl<>("", Validators.required()),
                "age", new FormControl<>(10, Validators.min(18))));

        assertEquals(ControlStatus.INVALID, group.status());
        assertTrue(group.errors().has("email.required"));
        assertTrue(group.errors().has("age.min"));

        group.setValue(Map.of("email", "a@b.co", "age", 20));

        assertEquals(ControlStatus.VALID, group.status());
        assertTrue(group.errors().isEmpty());
    }

    @Test
    void groupValidatorsReceiveTheAggregatedValue() {
        FormGroup group = new FormGroup(Map.of(
                "password", new FormControl<>("secret"),
                "confirm", new FormControl<>("different")));
        group.addValidator(value -> value.get("password").equals(value.get("confirm"))
                ? ValidationErrors.none()
                : ValidationErrors.of("mismatch"));

        assertEquals(ControlStatus.INVALID, group.status());
        assertTrue(group.errors().has("mismatch"));

        group.setValue(Map.of("password", "secret", "confirm", "secret"));

        assertEquals(ControlStatus.VALID, group.status());
    }

    @Test
    void aggregatesTouchedAndDirtyFlags() {
        FormControl<String> first = new FormControl<>("one");
        FormControl<String> second = new FormControl<>("two");
        FormGroup group = new FormGroup(Map.of("first", first, "second", second));

        assertFalse(group.touched());
        assertFalse(group.dirty());

        second.markAsTouched();
        first.setValue("changed");

        assertTrue(group.touched());
        assertTrue(group.dirty());

        group.markAsUntouched();
        group.markAsPristine();

        assertFalse(group.touched());
        assertFalse(group.dirty());
    }

    @Test
    void markAllAsTouchedReachesEveryDescendant() {
        FormControl<String> first = new FormControl<>("one");
        FormControl<String> second = new FormControl<>("two");
        FormGroup group = new FormGroup(Map.of("first", first, "second", second));

        group.markAllAsTouched();

        assertTrue(first.touched());
        assertTrue(second.touched());
    }

    @Test
    void disabledChildrenAreExcludedFromTheValueAndInheritStatus() {
        FormControl<String> enabled = new FormControl<>("on");
        FormControl<String> disabled = new FormControl<>("off");
        FormGroup group = new FormGroup(Map.of("enabled", enabled, "disabled", disabled));

        disabled.disable();

        assertFalse(group.value().containsKey("disabled"));
        assertTrue(group.value().containsKey("enabled"));
        assertEquals(ControlStatus.DISABLED, disabled.status());
        assertEquals(ControlStatus.VALID, group.status());

        disabled.enable();
        assertEquals("off", group.value().get("disabled"));
    }

    @Test
    void disablingTheGroupDisablesEveryChildEffectively() {
        FormControl<String> child = new FormControl<>("value");
        FormGroup group = new FormGroup(Map.of("child", child));

        group.disable();

        assertTrue(group.isDisabled());
        assertTrue(child.isDisabled());
        assertEquals(ControlStatus.DISABLED, group.status());
        assertEquals(ControlStatus.DISABLED, child.status());

        group.enable();
        assertFalse(child.isDisabled());
    }

    @Test
    void structureChangesAreObservable() {
        FormGroup group = new FormGroup(Map.of("first", new FormControl<>("one")));
        List<Map<String, Object>> values = new ArrayList<>();
        Effect effect = new Effect(() -> values.add(group.value()));

        group.addControl("second", new FormControl<>("two"));

        assertEquals(2, values.size());
        assertTrue(values.get(1).containsKey("second"));

        group.removeControl("first");

        assertEquals(3, values.size());
        assertFalse(values.get(2).containsKey("first"));
        effect.dispose();
    }

    @Test
    void nestedGroupsAndArraysAggregateErrors() {
        FormArray lines = new FormArray()
                .add(new FormControl<>("", Validators.required()));
        FormGroup address = new FormGroup(Map.of(
                "city", new FormControl<>("", Validators.required())));
        FormGroup form = new FormGroup(Map.of("address", address, "lines", lines));

        assertEquals(ControlStatus.INVALID, form.status());
        assertTrue(form.errors().has("address.city.required"));
        assertTrue(form.errors().has("lines.0.required"));
    }

    @Test
    void removeControlDetachesTheChild() {
        FormControl<String> child = new FormControl<>("value");
        FormGroup group = new FormGroup(Map.of("child", child));

        group.removeControl("child");
        group.disable();

        assertFalse(child.isDisabled());
        assertTrue(group.value().isEmpty());
        assertNull(group.removeControl("child"));
    }

    @Test
    void resetRestoresChildrenAndClearsTheGroup() {
        FormControl<String> first = new FormControl<>("one");
        FormControl<String> second = new FormControl<>("two");
        FormGroup group = new FormGroup(Map.of("first", first, "second", second));
        group.setValue(Map.of("first", "uno", "second", "dos"));
        group.markAllAsTouched();

        group.reset();

        assertEquals("one", first.value());
        assertEquals("two", second.value());
        assertFalse(group.touched());
        assertFalse(group.dirty());
        assertEquals(ControlStatus.VALID, group.status());
    }

    @Test
    void submitReturnsTheAggregatedSnapshot() {
        FormGroup group = new FormGroup(Map.of(
                "first", new FormControl<>("one"),
                "second", new FormControl<>("", Validators.required())));

        FormSubmission<Map<String, Object>> submission = group.submit();

        assertFalse(submission.valid());
        assertEquals("one", submission.value().get("first"));
        assertTrue(submission.errors().has("second.required"));
        assertTrue(group.touched());
    }
}
