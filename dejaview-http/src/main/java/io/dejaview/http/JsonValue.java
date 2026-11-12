package io.dejaview.http;

import io.dejaview.core.DejaViewException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A minimal, dependency-free JSON value tree.
 *
 * <p>Numbers are stored as {@code double}; integers beyond 2^53 lose precision.
 * Non-finite numbers cannot be represented and are rejected by
 * {@link Json#write(JsonValue)}. The tree is immutable.</p>
 */
public sealed interface JsonValue {

    /** The JSON {@code null} literal. */
    JsonNull NULL = new JsonNull();

    /** {@code true} for the {@code null} literal. */
    default boolean isNull() {
        return this == NULL || this instanceof JsonNull;
    }

    /** A JSON object; member order is insertion order, duplicate keys collapse to the last value. */
    record JsonObject(Map<String, JsonValue> members) implements JsonValue {
        public JsonObject {
            members = Collections.unmodifiableMap(new LinkedHashMap<>(members));
        }

        /** The member under {@code name}, or {@code null} when absent. */
        public JsonValue get(String name) {
            return members.get(name);
        }

        /** The string under {@code name}, or {@code null} when absent; fails on type mismatch. */
        public String getString(String name) {
            return Json.asString(members.get(name), name);
        }

        /** The number under {@code name}, or {@code null} when absent; fails on type mismatch. */
        public Double getNumber(String name) {
            if (!members.containsKey(name)) {
                return null;
            }
            return Json.asNumber(members.get(name), name);
        }

        /** The boolean under {@code name}, or {@code null} when absent; fails on type mismatch. */
        public Boolean getBoolean(String name) {
            if (!members.containsKey(name)) {
                return null;
            }
            return Json.asBoolean(members.get(name), name);
        }

        /** The object under {@code name}, or {@code null} when absent; fails on type mismatch. */
        public JsonObject getObject(String name) {
            return Json.asObject(members.get(name), name);
        }

        /** The array under {@code name}, or {@code null} when absent; fails on type mismatch. */
        public JsonArray getArray(String name) {
            return Json.asArray(members.get(name), name);
        }

        /** The member names in insertion order. */
        public Set<String> keys() {
            return members.keySet();
        }

        /** The number of members. */
        public int size() {
            return members.size();
        }

        /** {@code true} when there are no members. */
        public boolean isEmpty() {
            return members.isEmpty();
        }
    }

    /** A JSON array; element order is preserved. */
    record JsonArray(List<JsonValue> elements) implements JsonValue {
        public JsonArray {
            elements = List.copyOf(elements);
        }

        /** The element at {@code index}. */
        public JsonValue get(int index) {
            return elements.get(index);
        }

        /** The number of elements. */
        public int size() {
            return elements.size();
        }

        /** {@code true} when there are no elements. */
        public boolean isEmpty() {
            return elements.isEmpty();
        }
    }

    /** A JSON string. */
    record JsonString(String value) implements JsonValue {
        public JsonString {
            if (value == null) {
                throw new DejaViewException("json-error", "JSON string must not be null", null);
            }
        }
    }

    /** A JSON number, stored as {@code double}. */
    record JsonNumber(double value) implements JsonValue {
        public JsonNumber {
            if (!Double.isFinite(value)) {
                throw new DejaViewException("json-error", "JSON numbers must be finite", null);
            }
        }
    }

    /** A JSON boolean. */
    record JsonBoolean(boolean value) implements JsonValue {
    }

    /** The JSON {@code null} literal. Use {@link JsonValue#NULL}. */
    record JsonNull() implements JsonValue {
        @Override
        public String toString() {
            return "null";
        }
    }

    /** Convenience factory matching {@link #JsonObject}. */
    static JsonValue of(Map<String, JsonValue> members) {
        return new JsonObject(members);
    }

    /** Convenience factory matching {@link #JsonArray}. */
    static JsonValue of(List<JsonValue> elements) {
        return new JsonArray(elements);
    }

    /** Convenience factory converting common Java values recursively; everything else becomes {@code null}. */
    static JsonValue ofJava(Object value) {
        if (value == null) {
            return JsonValue.NULL;
        }
        if (value instanceof JsonValue json) {
            return json;
        }
        if (value instanceof String text) {
            return new JsonString(text);
        }
        if (value instanceof Boolean bool) {
            return new JsonBoolean(bool);
        }
        if (value instanceof Number number) {
            return new JsonNumber(number.doubleValue());
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, JsonValue> members = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                members.put(String.valueOf(entry.getKey()), ofJava(entry.getValue()));
            }
            return new JsonObject(members);
        }
        if (value instanceof List<?> list) {
            List<JsonValue> elements = new ArrayList<>(list.size());
            for (Object element : list) {
                elements.add(ofJava(element));
            }
            return new JsonArray(elements);
        }
        return JsonValue.NULL;
    }
}
