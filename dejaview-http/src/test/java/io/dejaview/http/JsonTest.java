package io.dejaview.http;

import io.dejaview.core.DejaViewException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {

    @Test
    void parsesAndWritesScalars() {
        assertEquals("1", Json.write(Json.parse("1")));
        assertEquals("true", Json.write(Json.parse("true")));
        assertEquals("false", Json.write(Json.parse("false")));
        assertEquals("null", Json.write(Json.parse("null")));
        assertEquals("\"text\"", Json.write(Json.parse("\"text\"")));
        assertEquals("-2.5", Json.write(Json.parse("-2.5")));
        assertEquals("1000000", Json.write(Json.parse("1e6")));
    }

    @Test
    void roundTripsNestedStructures() {
        String source = "{\"users\":[{\"name\":\"Ada\",\"tags\":[\"a\",\"b\"],\"admin\":true},{\"n\":1.5}],\"meta\":{\"count\":2}}";

        JsonValue parsed = Json.parse(source);

        assertEquals(Json.write(parsed), source);
    }

    @Test
    void duplicateObjectKeysCollapseToLastValue() {
        JsonValue.JsonObject object = (JsonValue.JsonObject) Json.parse("{\"a\":1,\"b\":2,\"a\":3}");

        assertEquals(2, object.size());
        assertEquals(3.0, object.getNumber("a"));
        assertEquals(2.0, object.getNumber("b"));
    }

    @Test
    void unicodeEscapesAreDecoded() {
        JsonValue value = Json.parse("\"\\u0041\\u00e9\""); // A é

        assertEquals("Aé", ((JsonValue.JsonString) value).value());
    }

    @Test
    void controlCharactersInStringsAreRejected() {
        DejaViewException error =
                assertThrows(DejaViewException.class, () -> Json.parse("\"a\nb\""));
        assertEquals("json-error", error.code());
    }

    @Test
    void trailingContentIsRejected() {
        DejaViewException error =
                assertThrows(DejaViewException.class, () -> Json.parse("1 2"));
        assertTrue(error.getMessage().contains("offset"));
    }

    @Test
    void errorsCarryOffset() {
        DejaViewException error =
                assertThrows(DejaViewException.class, () -> Json.parse("{\"a\":}"));
        assertEquals("json-error", error.code());
        // the '}' is at offset 5
        assertTrue(error.getMessage().contains("offset 5"));
    }

    @Test
    void unterminatedStringIsRejected() {
        assertThrows(DejaViewException.class, () -> Json.parse("\"abc"));
    }

    @Test
    void deepNestingBeyondCapIsRejected() {
        String deep = "[".repeat(600) + "]".repeat(600);

        assertThrows(DejaViewException.class, () -> Json.parse(deep));
    }

    @Test
    void nonFiniteNumbersAreRejectedAtConstruction() {
        assertThrows(DejaViewException.class, () -> new JsonValue.JsonNumber(Double.NaN));
        assertThrows(DejaViewException.class, () -> new JsonValue.JsonNumber(Double.POSITIVE_INFINITY));
    }

    @Test
    void numbersTooLargeBecomeInfinityAndAreRejectedByTheParser() {
        assertThrows(DejaViewException.class, () -> Json.parse("1e400"));
    }

    @Test
    void writerEscapesStrings() {
        JsonValue.JsonString value = new JsonValue.JsonString("quote:\" backslash:\\ newline:\n");

        assertEquals("\"quote:\\\" backslash:\\\\ newline:\\n\"", Json.write(value));
    }

    @Test
    void ofJavaConvertsCommonJavaValues() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("name", "Ada");
        source.put("count", 2);
        source.put("tags", List.of("x", "y"));
        source.put("admin", true);
        source.put("missing", null);

        JsonValue value = JsonValue.ofJava(source);

        assertEquals(Json.parse("{\"name\":\"Ada\",\"count\":2,\"tags\":[\"x\",\"y\"],\"admin\":true,\"missing\":null}"),
                value);
    }

    @Test
    void ofJavaLeavesUnknownTypesAsNull() {
        assertEquals(JsonValue.NULL, JsonValue.ofJava(new Object()));
    }

    @Test
    void asStringFailsOnTypeMismatchWithMemberName() {
        JsonValue.JsonObject object = (JsonValue.JsonObject) Json.parse("{\"n\":42}");

        DejaViewException error =
                assertThrows(DejaViewException.class, () -> object.getString("n"));
        assertEquals("json-error", error.code());
        assertTrue(error.getMessage().contains("'n'"));
    }

    @Test
    void absentMembersReturnNullFromGetters() {
        JsonValue.JsonObject object = (JsonValue.JsonObject) Json.parse("{}");

        assertEquals(null, object.getString("nope"));
        assertEquals(null, object.getNumber("nope"));
        assertEquals(null, object.getBoolean("nope"));
        assertEquals(null, object.getObject("nope"));
        assertEquals(null, object.getArray("nope"));
    }

    @Test
    void memberAccessMatchesJsonTree() {
        JsonValue.JsonObject object = (JsonValue.JsonObject) Json.parse(
                "{\"user\":{\"name\":\"Ada\",\"roles\":[\"admin\",\"ops\"],\"score\":9.5}}");

        JsonValue.JsonObject user = object.getObject("user");
        assertEquals("Ada", user.getString("name"));
        List<String> roles = new ArrayList<>();
        user.getArray("roles").elements().forEach(v -> roles.add(((JsonValue.JsonString) v).value()));
        assertEquals(List.of("admin", "ops"), roles);
        assertEquals(9.5, user.getNumber("score"));
        assertEquals(List.of("user"), List.copyOf(object.keys()));
    }
}
