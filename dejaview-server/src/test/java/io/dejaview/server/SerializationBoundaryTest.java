package io.dejaview.server;

import io.dejaview.core.DejaViewException;
import io.dejaview.http.JsonValue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializationBoundaryTest {

    @Test
    void passesScalarValuesThrough() {
        assertEquals(new JsonValue.JsonString("hello"), SerializationBoundary.toJson("hello"));
        assertEquals(new JsonValue.JsonBoolean(true), SerializationBoundary.toJson(Boolean.TRUE));
        assertEquals(new JsonValue.JsonNumber(42.0), SerializationBoundary.toJson(42));
        assertEquals(new JsonValue.JsonNumber(2.5), SerializationBoundary.toJson(2.5));
    }

    @Test
    void nullBecomesJsonNull() {
        assertEquals(JsonValue.NULL, SerializationBoundary.toJson(null));
    }

    @Test
    void mapsPreserveOrderAndConvertRecursively() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("b", 2);
        nested.put("a", 1);
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("nested", nested);
        value.put("tags", List.of("x", "y"));

        JsonValue json = SerializationBoundary.toJson(value);

        JsonValue.JsonObject object = assertInstanceOf(JsonValue.JsonObject.class, json);
        assertEquals(List.of("nested", "tags"), List.copyOf(object.members().keySet()));
        JsonValue.JsonObject nestedJson = assertInstanceOf(JsonValue.JsonObject.class, object.members().get("nested"));
        assertEquals(List.of("b", "a"), List.copyOf(nestedJson.members().keySet()));
        assertEquals(new JsonValue.JsonNumber(1.0), nestedJson.members().get("a"));
    }

    @Test
    void listsConvertElements() {
        JsonValue json = SerializationBoundary.toJson(java.util.Arrays.asList("a", 1, true, null));

        JsonValue.JsonArray array = assertInstanceOf(JsonValue.JsonArray.class, json);
        assertEquals(List.of(
                        new JsonValue.JsonString("a"),
                        new JsonValue.JsonNumber(1.0),
                        new JsonValue.JsonBoolean(true),
                        JsonValue.NULL),
                array.elements());
    }

    @Test
    void jsonValuesPassThroughUnchanged() {
        JsonValue tree = new JsonValue.JsonObject(Map.of("k", new JsonValue.JsonString("v")));

        assertEquals(tree, SerializationBoundary.toJson(tree));
    }

    @Test
    void nonFiniteNumbersAreRejected() {
        DejaViewException first = assertThrows(
                DejaViewException.class, () -> SerializationBoundary.toJson(Double.NaN));
        DejaViewException second = assertThrows(
                DejaViewException.class, () -> SerializationBoundary.toJson(Double.POSITIVE_INFINITY));
        DejaViewException third = assertThrows(
                DejaViewException.class, () -> SerializationBoundary.toJson(Float.NEGATIVE_INFINITY));

        assertEquals("not-serializable", first.code());
        assertEquals("not-serializable", second.code());
        assertEquals("not-serializable", third.code());
    }

    @Test
    void mapKeysMustBeStrings() {
        Map<Object, Object> value = new LinkedHashMap<>();
        value.put(7, "seven");

        DejaViewException failure = assertThrows(DejaViewException.class, () -> SerializationBoundary.toJson(value));

        assertEquals("not-serializable", failure.code());
    }

    @Test
    void arbitraryObjectsAreRejected() {
        DejaViewException failure =
                assertThrows(DejaViewException.class, () -> SerializationBoundary.toJson(new Object()));

        assertEquals("not-serializable", failure.code());
    }

    @Test
    void nestingBeyondTheDepthLimitIsRejected() {
        Object value = buildNestedList(SerializationBoundary.MAX_DEPTH + 1);

        DejaViewException failure = assertThrows(DejaViewException.class, () -> SerializationBoundary.toJson(value));

        assertEquals("not-serializable", failure.code());
        assertTrue(failure.getMessage().contains("nesting depth"));
    }

    @Test
    void depthAtTheLimitIsAccepted() {
        Object value = buildNestedList(SerializationBoundary.MAX_DEPTH - 1);

        JsonValue json = SerializationBoundary.toJson(value);

        assertInstanceOf(JsonValue.JsonArray.class, json);
    }

    @Test
    void writeProducesCanonicalJsonText() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("name", "Déjà Vu");
        value.put("count", 3);
        value.put("active", true);

        String text = SerializationBoundary.write(value);

        assertEquals("{\"name\":\"Déjà Vu\",\"count\":3,\"active\":true}", text);
    }

    private static Object buildNestedList(int depth) {
        Object value = "leaf";
        for (int index = 0; index < depth; index++) {
            value = new ArrayList<>(List.of(value));
        }
        return value;
    }
}
