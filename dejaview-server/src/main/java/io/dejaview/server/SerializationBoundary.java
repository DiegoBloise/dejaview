package io.dejaview.server;

import io.dejaview.core.DejaViewException;
import io.dejaview.http.Json;
import io.dejaview.http.JsonValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A strict boundary for values crossing out of the framework as JSON.
 *
 * <p>Serialization is intentionally narrow: only values with an obvious JSON
 * representation are accepted, and everything else fails fast with a
 * {@code not-serializable} error instead of silently becoming {@code null}
 * (which is what {@code JsonValue.ofJava} does). The accepted set:</p>
 * <ul>
 *   <li>{@code null} → JSON {@code null};</li>
 *   <li>{@link JsonValue} trees → passed through as-is, subject to the depth
 *       limit; {@link Json} enforces finite numbers at write time;</li>
 *   <li>{@link String}, {@link Boolean} → JSON string/boolean;</li>
 *   <li>{@link Number} → JSON number; non-finite values (NaN, ±Infinity) are
 *       rejected. Note integers above 2^53 lose precision because JSON numbers
 *       are doubles;</li>
 *   <li>{@code Map<String, ?>} → JSON object, preserving iteration order; keys
 *       of any other type are rejected;</li>
 *   <li>{@code List<?>} → JSON array;</li>
 *   <li>nesting deeper than {@value #MAX_DEPTH} levels is rejected, bounding
 *       stack usage for adversarial structures.</li>
 * </ul>
 *
 * <p>Circular references are not detected beyond the depth limit; a cycle that
 * fits within the limit is the caller's bug, surfaced by a stack overflow at
 * write time.</p>
 */
public final class SerializationBoundary {

    /**
     * Maximum nesting depth accepted by {@link #toJson(Object)}.
     */
    public static final int MAX_DEPTH = 512;

    private SerializationBoundary() {
    }

    /**
     * Converts {@code value} to a {@link JsonValue} tree.
     *
     * @param value the value to convert; may be {@code null}
     * @throws DejaViewException with code {@code not-serializable} when a value is
     *                           outside the accepted set, or nesting exceeds
     *                           {@value #MAX_DEPTH}
     */
    public static JsonValue toJson(Object value) {
        return toJson(value, 0);
    }

    /**
     * Converts {@code value} to canonical JSON text.
     *
     * @throws DejaViewException with code {@code not-serializable} under the same
     *                           conditions as {@link #toJson(Object)}
     */
    public static String write(Object value) {
        return Json.write(toJson(value));
    }

    private static JsonValue toJson(Object value, int depth) {
        if (depth > MAX_DEPTH) {
            throw notSerializable("value exceeds the maximum nesting depth of " + MAX_DEPTH);
        }
        if (value == null) {
            return JsonValue.NULL;
        }
        if (value instanceof JsonValue json) {
            return json;
        }
        if (value instanceof String text) {
            return new JsonValue.JsonString(text);
        }
        if (value instanceof Boolean bool) {
            return new JsonValue.JsonBoolean(bool);
        }
        if (value instanceof Number number) {
            double d = number.doubleValue();
            if (!Double.isFinite(d)) {
                throw notSerializable("non-finite number " + number);
            }
            return new JsonValue.JsonNumber(d);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, JsonValue> members = new LinkedHashMap<>(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw notSerializable(
                            "map keys must be strings, got " + describe(entry.getKey()));
                }
                members.put(key, toJson(entry.getValue(), depth + 1));
            }
            return new JsonValue.JsonObject(members);
        }
        if (value instanceof List<?> list) {
            List<JsonValue> elements = new ArrayList<>(list.size());
            for (Object element : list) {
                elements.add(toJson(element, depth + 1));
            }
            return new JsonValue.JsonArray(elements);
        }
        throw notSerializable(describe(value));
    }

    private static DejaViewException notSerializable(String detail) {
        return new DejaViewException("not-serializable", "Value is not serializable as JSON: " + detail, null);
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
