package io.dejaview.http;

import io.dejaview.core.DejaViewException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static helpers for the {@link JsonValue} tree: type coercion, parsing and
 * writing. The parser is a small recursive-descent implementation and rejects
 * malformed input with {@link DejaViewException} codes of {@code json-error}
 * carrying the character offset of the failure.
 */
public final class Json {

    private static final int MAX_DEPTH = 512;

    private Json() {
    }

    /** The string under {@code value}, or {@code null} when the value is absent. Fails on type mismatch. */
    static String asString(JsonValue value, String context) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonValue.JsonString string) {
            return string.value();
        }
        throw mismatch(context, "a string", value);
    }

    /** The number under {@code value}, or {@code null} when the value is absent. Fails on type mismatch. */
    static Double asNumber(JsonValue value, String context) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonValue.JsonNumber number) {
            return number.value();
        }
        throw mismatch(context, "a number", value);
    }

    /** The boolean under {@code value}, or {@code null} when the value is absent. Fails on type mismatch. */
    static Boolean asBoolean(JsonValue value, String context) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonValue.JsonBoolean bool) {
            return bool.value();
        }
        throw mismatch(context, "a boolean", value);
    }

    /** The object under {@code value}, or {@code null} when the value is absent. Fails on type mismatch. */
    static JsonValue.JsonObject asObject(JsonValue value, String context) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonValue.JsonObject object) {
            return object;
        }
        throw mismatch(context, "an object", value);
    }

    /** The array under {@code value}, or {@code null} when the value is absent. Fails on type mismatch. */
    static JsonValue.JsonArray asArray(JsonValue value, String context) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonValue.JsonArray array) {
            return array;
        }
        throw mismatch(context, "an array", value);
    }

    private static DejaViewException mismatch(String context, String expected, JsonValue actual) {
        String name = context == null ? "value" : "member '" + context + "'";
        return new DejaViewException(
                "json-error",
                name + " must be " + expected + " but was " + describe(actual),
                null);
    }

    private static String describe(JsonValue value) {
        if (value == null || value.isNull()) {
            return "null";
        }
        if (value instanceof JsonValue.JsonObject) {
            return "an object";
        }
        if (value instanceof JsonValue.JsonArray) {
            return "an array";
        }
        if (value instanceof JsonValue.JsonString) {
            return "a string";
        }
        if (value instanceof JsonValue.JsonNumber) {
            return "a number";
        }
        return "a boolean";
    }

    /**
     * Parses a JSON document. The document must contain a single value followed
     * only by whitespace. Duplicate object keys collapse to the last value.
     *
     * @param text the JSON text, not {@code null}
     * @return the parsed tree
     * @throws DejaViewException with code {@code json-error} when malformed
     */
    public static JsonValue parse(String text) {
        if (text == null) {
            throw new DejaViewException("json-error", "JSON text must not be null", null);
        }
        Parser parser = new Parser(text);
        JsonValue value = parser.parseDocument();
        return value;
    }

    /**
     * Writes a JSON value to its canonical text form: members keep insertion
     * order, strings are escaped, non-finite numbers are rejected.
     *
     * @param value the tree to write, not {@code null}
     * @return the JSON text
     */
    public static String write(JsonValue value) {
        if (value == null) {
            throw new DejaViewException("json-error", "JSON value must not be null", null);
        }
        StringBuilder out = new StringBuilder();
        writeValue(value, out, 0);
        return out.toString();
    }

    private static void writeValue(JsonValue value, StringBuilder out, int depth) {
        if (value == null || value.isNull()) {
            out.append("null");
            return;
        }
        if (value instanceof JsonValue.JsonObject object) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<String, JsonValue> entry : object.members().entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeString(entry.getKey(), out);
                out.append(':');
                writeValue(entry.getValue(), out, depth + 1);
            }
            out.append('}');
            return;
        }
        if (value instanceof JsonValue.JsonArray array) {
            out.append('[');
            boolean first = true;
            for (JsonValue element : array.elements()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeValue(element, out, depth + 1);
            }
            out.append(']');
            return;
        }
        if (value instanceof JsonValue.JsonString string) {
            writeString(string.value(), out);
            return;
        }
        if (value instanceof JsonValue.JsonNumber number) {
            double d = number.value();
            if (!Double.isFinite(d)) {
                throw new DejaViewException("json-error", "JSON numbers must be finite", null);
            }
            if (d == Math.rint(d) && Math.abs(d) < 1e15) {
                out.append(Long.toString((long) d));
            } else {
                out.append(Double.toString(d));
            }
            return;
        }
        if (value instanceof JsonValue.JsonBoolean bool) {
            out.append(bool.value() ? "true" : "false");
            return;
        }
        throw new DejaViewException("json-error", "Unsupported JSON value: " + value.getClass().getName(), null);
    }

    private static void writeString(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    private static final class Parser {
        private final String text;
        private int offset;

        private Parser(String text) {
            this.text = text;
            this.offset = 0;
        }

        private JsonValue parseDocument() {
            skipWhitespace();
            JsonValue value = parseValue(0);
            skipWhitespace();
            if (offset != text.length()) {
                throw error("Unexpected trailing content");
            }
            return value;
        }

        private JsonValue parseValue(int depth) {
            if (depth > MAX_DEPTH) {
                throw error("Maximum nesting depth exceeded");
            }
            skipWhitespace();
            if (offset >= text.length()) {
                throw error("Unexpected end of input");
            }
            char c = text.charAt(offset);
            return switch (c) {
                case '{' -> parseObject(depth);
                case '[' -> parseArray(depth);
                case '"' -> new JsonValue.JsonString(parseString());
                case 't' -> {
                    expectLiteral("true");
                    yield new JsonValue.JsonBoolean(true);
                }
                case 'f' -> {
                    expectLiteral("false");
                    yield new JsonValue.JsonBoolean(false);
                }
                case 'n' -> {
                    expectLiteral("null");
                    yield JsonValue.NULL;
                }
                default -> {
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        yield parseNumber();
                    }
                    throw error("Unexpected character '" + c + "'");
                }
            };
        }

        private JsonValue parseObject(int depth) {
            expect('{');
            Map<String, JsonValue> members = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                offset++;
                return new JsonValue.JsonObject(members);
            }
            while (true) {
                skipWhitespace();
                if (peek() != '"') {
                    throw error("Expected object member name");
                }
                String key = parseString();
                skipWhitespace();
                expect(':');
                JsonValue value = parseValue(depth + 1);
                members.put(key, value);
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    offset++;
                    continue;
                }
                if (c == '}') {
                    offset++;
                    return new JsonValue.JsonObject(members);
                }
                throw error("Expected ',' or '}' in object");
            }
        }

        private JsonValue parseArray(int depth) {
            expect('[');
            List<JsonValue> elements = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                offset++;
                return new JsonValue.JsonArray(elements);
            }
            while (true) {
                elements.add(parseValue(depth + 1));
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    offset++;
                    continue;
                }
                if (c == ']') {
                    offset++;
                    return new JsonValue.JsonArray(elements);
                }
                throw error("Expected ',' or ']' in array");
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (true) {
                if (offset >= text.length()) {
                    throw error("Unterminated string");
                }
                char c = text.charAt(offset++);
                if (c == '"') {
                    return out.toString();
                }
                if (c == '\\') {
                    if (offset >= text.length()) {
                        throw error("Unterminated escape sequence");
                    }
                    char esc = text.charAt(offset++);
                    switch (esc) {
                        case '"' -> out.append('"');
                        case '\\' -> out.append('\\');
                        case '/' -> out.append('/');
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case 'u' -> {
                            if (offset + 4 > text.length()) {
                                throw error("Truncated unicode escape");
                            }
                            String hex = text.substring(offset, offset + 4);
                            int code;
                            try {
                                code = Integer.parseInt(hex, 16);
                            } catch (NumberFormatException e) {
                                throw error("Invalid unicode escape '\\u" + hex + "'");
                            }
                            offset += 4;
                            out.append((char) code);
                        }
                        default -> throw error("Invalid escape '\\" + esc + "'");
                    }
                    continue;
                }
                if (c < 0x20) {
                    throw error("Unescaped control character in string");
                }
                out.append(c);
            }
        }

        private JsonValue parseNumber() {
            int start = offset;
            if (peek() == '-') {
                offset++;
            }
            int intStart = offset;
            while (offset < text.length() && Character.isDigit(text.charAt(offset))) {
                offset++;
            }
            if (offset == intStart) {
                throw error("Expected digits in number");
            }
            if (offset < text.length() && text.charAt(offset) == '.') {
                offset++;
                int fracStart = offset;
                while (offset < text.length() && Character.isDigit(text.charAt(offset))) {
                    offset++;
                }
                if (offset == fracStart) {
                    throw error("Expected digits after decimal point");
                }
            }
            if (offset < text.length() && (text.charAt(offset) == 'e' || text.charAt(offset) == 'E')) {
                offset++;
                if (offset < text.length() && (text.charAt(offset) == '+' || text.charAt(offset) == '-')) {
                    offset++;
                }
                int expStart = offset;
                while (offset < text.length() && Character.isDigit(text.charAt(offset))) {
                    offset++;
                }
                if (offset == expStart) {
                    throw error("Expected digits in exponent");
                }
            }
            String raw = text.substring(start, offset);
            try {
                double value = Double.parseDouble(raw);
                return new JsonValue.JsonNumber(value);
            } catch (NumberFormatException e) {
                throw error("Invalid number '" + raw + "'");
            }
        }

        private void expectLiteral(String literal) {
            if (!text.regionMatches(offset, literal, 0, literal.length())) {
                throw error("Expected '" + literal + "'");
            }
            offset += literal.length();
        }

        private void expect(char expected) {
            if (offset >= text.length() || text.charAt(offset) != expected) {
                throw error("Expected '" + expected + "'");
            }
            offset++;
        }

        private char peek() {
            if (offset >= text.length()) {
                throw error("Unexpected end of input");
            }
            return text.charAt(offset);
        }

        private void skipWhitespace() {
            while (offset < text.length()) {
                char c = text.charAt(offset);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    offset++;
                } else {
                    return;
                }
            }
        }

        private DejaViewException error(String message) {
            return new DejaViewException("json-error", message + " at offset " + offset, null);
        }
    }
}
