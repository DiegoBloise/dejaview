package io.dejaview.forms;

import io.dejaview.core.Attribute;
import io.dejaview.core.ElementView;
import io.dejaview.core.EventBinding;
import io.dejaview.core.EventValueHandler;
import io.dejaview.core.Property;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormBindingsTest {

    @Test
    void textInputReflectsValueStatusAndDisabledState() {
        FormControl<String> control = new FormControl<>("", Validators.required());

        ElementView input = FormBindings.text("email", control);

        assertEquals("input", input.tagName());
        assertEquals("email", attributes(input).get("id"));
        assertEquals("email", attributes(input).get("name"));
        assertEquals("text", attributes(input).get("type"));
        assertEquals("true", attributes(input).get("aria-invalid"));
        assertEquals("", properties(input).get("value"));
        assertEquals("false", properties(input).get("disabled"));

        control.setValue("user@example.com");
        control.disable();

        ElementView updated = FormBindings.text("email", control);
        assertEquals("false", attributes(updated).get("aria-invalid"));
        assertEquals("user@example.com", properties(updated).get("value"));
        assertEquals("true", properties(updated).get("disabled"));
    }

    @Test
    void textInputUpdatesTheControlAndMarksItTouchedOnBlur() {
        FormControl<String> control = new FormControl<>("");

        ElementView input = FormBindings.text("name", control);
        valueEvent(input, "input").handle("typed", false);

        assertEquals("typed", control.value());

        event(input, "blur").handler().run();

        assertTrue(control.touched());
    }

    @Test
    void textareaUsesAValuePropertyAndInputEvent() {
        FormControl<String> control = new FormControl<>("hello");

        ElementView textarea = FormBindings.textarea("bio", control);

        assertEquals("textarea", textarea.tagName());
        assertEquals("hello", properties(textarea).get("value"));

        valueEvent(textarea, "input").handle("changed", false);
        assertEquals("changed", control.value());
    }

    @Test
    void checkboxBindsCheckedAndChange() {
        FormControl<Boolean> control = new FormControl<>(false);

        ElementView checkbox = FormBindings.checkbox("terms", control);

        assertEquals("checkbox", attributes(checkbox).get("type"));
        assertEquals("false", properties(checkbox).get("checked"));

        valueEvent(checkbox, "change").handle("on", true);

        assertTrue(control.value());
        assertEquals("true", properties(FormBindings.checkbox("terms", control)).get("checked"));
    }

    @Test
    void selectMarksTheCurrentOptionAsSelected() {
        FormControl<String> control = new FormControl<>("pt");

        ElementView select = FormBindings.select("language", control, List.of(
                new FormBindings.SelectOption("en", "English"),
                new FormBindings.SelectOption("pt", "Português")));

        assertEquals("select", select.tagName());
        List<ElementView> options = select.children().stream()
                .map(view -> assertInstanceOf(ElementView.class, view))
                .toList();
        assertEquals("option", options.get(0).tagName());
        assertEquals("false", properties(options.get(0)).get("selected"));
        assertEquals("true", properties(options.get(1)).get("selected"));

        valueEvent(select, "change").handle("en", false);

        assertEquals("en", control.value());
    }

    @Test
    void idsAreOptional() {
        ElementView input = FormBindings.text(" ", new FormControl<>(""));

        assertFalse(attributes(input).containsKey("id"));
        assertFalse(attributes(input).containsKey("name"));
    }

    private static EventBinding event(ElementView view, String type) {
        return view.events().stream()
                .filter(binding -> binding.type().equals(type))
                .findFirst()
                .orElseThrow();
    }

    private static EventValueHandler valueEvent(ElementView view, String type) {
        EventBinding binding = event(view, type);
        assertTrue(binding.hasValueHandler());
        return binding.valueHandler();
    }

    private static Map<String, String> attributes(ElementView view) {
        return view.attributes().stream().collect(Collectors.toMap(
                Attribute::name, Attribute::value));
    }

    private static Map<String, String> properties(ElementView view) {
        return view.properties().stream().collect(Collectors.toMap(
                Property::name, Property::value));
    }
}
