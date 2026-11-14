package kitnapper.service.persistence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Recursive-descent parser for exactly the JSON subset {@link JsonWriter}
 * emits (implementation plan 3.2): objects, arrays, strings, plain-decimal
 * numbers (yielded as double), true, false, null. No comments, no exponent
 * notation, no other escapes; trailing commas rejected. Any deviation
 * throws IllegalArgumentException, which the loader treats as corruption
 * and answers with a fresh seed (NFR-07, D18).
 */
public final class JsonParser {

    private final String text;
    private int pos;

    private JsonParser(String text) {
        this.text = text;
    }

    /** Parses one whole document; returns Map, List, String, Double, Boolean, or null. */
    public static Object parse(String text) {
        JsonParser parser = new JsonParser(Objects.requireNonNull(text, "text"));
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos < parser.text.length()) {
            throw parser.error("unexpected content after document");
        }
        return value;
    }

    private Object readValue() {
        if (pos >= text.length()) {
            throw error("unexpected end of input");
        }
        char c = text.charAt(pos);
        switch (c) {
            case '{':
                return readObject();
            case '[':
                return readArray();
            case '"':
                return readString();
            case 't':
                return readKeyword("true", Boolean.TRUE);
            case 'f':
                return readKeyword("false", Boolean.FALSE);
            case 'n':
                return readKeyword("null", null);
            default:
                if (c == '-' || isDigit(c)) {
                    return readNumber();
                }
                throw error("unexpected character '" + c + "'");
        }
    }

    private Object readKeyword(String keyword, Object value) {
        if (!text.startsWith(keyword, pos)) {
            throw error("invalid literal");
        }
        pos += keyword.length();
        return value;
    }

    private Object readNumber() {
        int start = pos;
        if (text.charAt(pos) == '-') {
            pos++;
        }
        if (pos >= text.length() || !isDigit(text.charAt(pos))) {
            throw error("invalid number");
        }
        while (pos < text.length() && isDigit(text.charAt(pos))) {
            pos++;
        }
        if (pos < text.length() && text.charAt(pos) == '.') {
            pos++;
            if (pos >= text.length() || !isDigit(text.charAt(pos))) {
                throw error("invalid number fraction");
            }
            while (pos < text.length() && isDigit(text.charAt(pos))) {
                pos++;
            }
        }
        String token = text.substring(start, pos);
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException e) {
            throw error("invalid number " + token);
        }
    }

    private String readString() {
        pos++;
        StringBuilder out = new StringBuilder();
        while (true) {
            if (pos >= text.length()) {
                throw error("unterminated string");
            }
            char c = text.charAt(pos++);
            if (c == '"') {
                return out.toString();
            }
            if (c == '\\') {
                if (pos >= text.length()) {
                    throw error("unterminated escape");
                }
                char escaped = text.charAt(pos++);
                switch (escaped) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case 'n' -> out.append('\n');
                    default -> throw error("unsupported escape \\" + escaped);
                }
            } else if (c < 0x20) {
                throw error("control character in string");
            } else {
                out.append(c);
            }
        }
    }

    private Map<String, Object> readObject() {
        pos++;
        skipWhitespace();
        Map<String, Object> object = new LinkedHashMap<>();
        if (peek('}')) {
            return object;
        }
        while (true) {
            skipWhitespace();
            if (pos >= text.length() || text.charAt(pos) != '"') {
                throw error("expected string key");
            }
            String key = readString();
            skipWhitespace();
            if (pos >= text.length() || text.charAt(pos) != ':') {
                throw error("expected ':' after key");
            }
            pos++;
            skipWhitespace();
            object.put(key, readValue());
            skipWhitespace();
            if (peek(',')) {
                continue;
            }
            if (peek('}')) {
                return object;
            }
            throw error("expected ',' or '}'");
        }
    }

    private List<Object> readArray() {
        pos++;
        skipWhitespace();
        List<Object> array = new java.util.ArrayList<>();
        if (peek(']')) {
            return array;
        }
        while (true) {
            skipWhitespace();
            array.add(readValue());
            skipWhitespace();
            if (peek(',')) {
                continue;
            }
            if (peek(']')) {
                return array;
            }
            throw error("expected ',' or ']'");
        }
    }

    private void skipWhitespace() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }

    private boolean peek(char c) {
        if (pos < text.length() && text.charAt(pos) == c) {
            pos++;
            return true;
        }
        return false;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("JSON parse error at offset " + pos + ": " + message);
    }
}
