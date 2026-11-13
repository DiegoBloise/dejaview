package io.dejaview.forms;

import io.dejaview.core.ElementView;
import io.dejaview.core.View;
import io.dejaview.core.Views;

import java.util.List;
import java.util.Objects;

/**
 * Builds form elements bound to a {@link FormControl}.
 *
 * <p>Each factory returns a regular DejaView {@link ElementView}. Re-rendering
 * the surrounding component reads the control value, status and disabled state,
 * so the DOM stays synchronized with the form state without manual wiring.</p>
 *
 * <p>Typed text is applied on {@code input}; selects and checkboxes use
 * {@code change}. Blur marks the control as touched, which is what validation
 * messages usually key off.</p>
 *
 * <pre>{@code
 * FormControl<String> email = new FormControl<>("", Validators.email());
 * ElementView input = FormBindings.email("email", email);
 * }</pre>
 */
public final class FormBindings {

    private FormBindings() {
    }

    /**
     * Creates a text input.
     */
    public static ElementView text(String id, FormControl<String> control) {
        return input(id, "text", control);
    }

    /**
     * Creates an email input.
     */
    public static ElementView email(String id, FormControl<String> control) {
        return input(id, "email", control);
    }

    /**
     * Creates a password input.
     */
    public static ElementView password(String id, FormControl<String> control) {
        return input(id, "password", control);
    }

    /**
     * Creates an input of the given type bound to a string control.
     */
    public static ElementView input(String id, String type, FormControl<String> control) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(control, "control");

        ElementView input = Views.element("input")
                .attr("type", type)
                .attr("aria-invalid", invalid(control))
                .prop("value", control.value())
                .prop("disabled", Boolean.toString(control.isDisabled()))
                .onValue("input", (value, checked) -> control.setValue(value))
                .on("blur", control::markAsTouched);
        return identify(input, id);
    }

    /**
     * Creates a textarea bound to a string control.
     */
    public static ElementView textarea(String id, FormControl<String> control) {
        Objects.requireNonNull(control, "control");

        ElementView textarea = Views.element("textarea")
                .attr("aria-invalid", invalid(control))
                .prop("value", control.value())
                .prop("disabled", Boolean.toString(control.isDisabled()))
                .onValue("input", (value, checked) -> control.setValue(value))
                .on("blur", control::markAsTouched);
        return identify(textarea, id);
    }

    /**
     * Creates a checkbox bound to a boolean control.
     */
    public static ElementView checkbox(String id, FormControl<Boolean> control) {
        Objects.requireNonNull(control, "control");

        ElementView checkbox = Views.element("input")
                .attr("type", "checkbox")
                .attr("aria-invalid", invalid(control))
                .prop("checked", Boolean.toString(control.value()))
                .prop("disabled", Boolean.toString(control.isDisabled()))
                .onValue("change", (value, checked) -> control.setValue(checked))
                .on("blur", control::markAsTouched);
        return identify(checkbox, id);
    }

    /**
     * Creates a single-choice select bound to a string control.
     */
    public static ElementView select(
            String id, FormControl<String> control, List<SelectOption> options) {
        Objects.requireNonNull(control, "control");
        Objects.requireNonNull(options, "options");

        View[] children = options.stream()
                .map(option -> (View) Views.element("option", Views.text(option.label()))
                        .attr("value", option.value())
                        .prop("selected", Boolean.toString(
                                option.value().equals(control.value()))))
                .toArray(View[]::new);

        ElementView select = Views.element("select", children)
                .attr("aria-invalid", invalid(control))
                .prop("disabled", Boolean.toString(control.isDisabled()))
                .onValue("change", (value, checked) -> control.setValue(value))
                .on("blur", control::markAsTouched);
        return identify(select, id);
    }

    /**
     * An option in a {@link #select(String, FormControl, List) select}.
     *
     * @param value submitted value
     * @param label visible, already-localized label
     */
    public record SelectOption(String value, String label) {

        public SelectOption {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(label, "label");
        }
    }

    private static ElementView identify(ElementView element, String id) {
        if (id == null || id.isBlank()) {
            return element;
        }
        return element.id(id).attr("name", id);
    }

    private static String invalid(AbstractControl<?> control) {
        return control.status() == ControlStatus.INVALID ? "true" : "false";
    }
}
