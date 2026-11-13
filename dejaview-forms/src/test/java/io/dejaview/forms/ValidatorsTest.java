package io.dejaview.forms;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorsTest {

    @Test
    void requiredRejectsBlankStrings() {
        assertTrue(Validators.required().validate("").has("required"));
        assertTrue(Validators.required().validate("   ").has("required"));
        assertTrue(Validators.required().validate("value").isEmpty());
    }

    @Test
    void lengthValidatorsIgnoreEmptyStrings() {
        assertTrue(Validators.minLength(3).validate("").isEmpty());
        assertTrue(Validators.minLength(3).validate("ab").has("minLength"));
        assertTrue(Validators.minLength(3).validate("abc").isEmpty());
        assertTrue(Validators.maxLength(3).validate("abcd").has("maxLength"));
        assertTrue(Validators.maxLength(3).validate("abc").isEmpty());
    }

    @Test
    void emailAcceptsSimpleAddressesAndIgnoresEmptyInput() {
        Validator<String> email = Validators.email();

        assertTrue(email.validate("").isEmpty());
        assertTrue(email.validate("user@example.com").isEmpty());
        assertTrue(email.validate("user@example.co.uk").isEmpty());
        assertTrue(email.validate("user").has("email"));
        assertTrue(email.validate("@example.com").has("email"));
        assertTrue(email.validate("user@example").has("email"));
        assertTrue(email.validate("user@@example.com").has("email"));
        assertTrue(email.validate("user @example.com").has("email"));
    }

    @Test
    void patternReportsTheConfiguredKey() {
        Validator<String> digits = Validators.pattern(Pattern.compile("\\d+"), "digits");

        assertTrue(digits.validate("").isEmpty());
        assertTrue(digits.validate("123").isEmpty());
        assertTrue(digits.validate("12a").has("digits"));
    }

    @Test
    void numberValidatorsCompareValues() {
        assertTrue(Validators.<Integer>min(18).validate(18).isEmpty());
        assertTrue(Validators.<Integer>min(18).validate(17).has("min"));
        assertTrue(Validators.<Integer>max(10).validate(10).isEmpty());
        assertTrue(Validators.<Integer>max(10).validate(11).has("max"));
    }

    @Test
    void customUsesAPredicateAndKey() {
        Validator<String> even = Validators.custom("even", value -> value.length() % 2 == 0);

        assertTrue(even.validate("ab").isEmpty());
        assertTrue(even.validate("abc").has("even"));
    }

    @Test
    void invalidArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Validators.minLength(-1));
        assertThrows(IllegalArgumentException.class, () -> Validators.maxLength(-1));
    }

    @Test
    void validationErrorsMergeWithFirstWinsAndPrefixKeys() {
        ValidationErrors first = ValidationErrors.of(Map.of("a", "first"));
        ValidationErrors second = ValidationErrors.of(Map.of("a", "second", "b", ""));

        ValidationErrors merged = first.merge(second);

        assertEquals("first", merged.message("a"));
        assertTrue(merged.has("b"));
        assertTrue(merged.prefix("child").has("child.a"));
        assertEquals("first", merged.prefix("child").message("child.a"));
    }

    @Test
    void validationErrorsAreImmutableAndOrdered() {
        ValidationErrors errors = ValidationErrors.of(Map.of("a", "", "b", ""));

        assertEquals(2, errors.keys().size());
        assertThrows(UnsupportedOperationException.class,
                () -> errors.asMap().put("c", ""));
        assertSame(ValidationErrors.none(), ValidationErrors.none());
        assertFalse(errors.isEmpty());
        assertEquals("", errors.message("missing"));
    }

    @Test
    void validationErrorKeysMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> ValidationErrors.of(" "));
    }
}
