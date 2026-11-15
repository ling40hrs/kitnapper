package kitnapper.service.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Recursive-descent parser for exactly the JSON subset {@link JsonWriter}
 * emits (implementation plan §3.2): objects, arrays, strings, plain-decimal
 * numbers (yielded as double), true, false, null. No comments, no exponent
 * notation, no other escapes; trailing commas rejected. Any deviation
 * throws IllegalArgumentException, which the loader treats as corruption
 * and answers with a fresh seed (NFR-07, D18). Character scanning lives in
 * {@link JsonScanner}; this class handles the document structure.
 */
public final class JsonParser {

    private final JsonScanner scanner;

    private JsonParser(JsonScanner scanner) {
        this.scanner = scanner;
    }

    /** Parses one whole document; returns Map, List, String, Double, Boolean, or null. */
    public static Object parse(String text) {
        JsonScanner scanner = new JsonScanner(Objects.requireNonNull(text, "text"));
        JsonParser parser = new JsonParser(scanner);
        scanner.skipWhitespace();
        Object value = parser.readValue();
        scanner.skipWhitespace();
        if (scanner.hasMore()) {
            throw scanner.error("unexpected content after document");
        }
        return value;
    }

    private Object readValue() {
        if (!scanner.hasMore()) {
            throw scanner.error("unexpected end of input");
        }
        char c = scanner.current();
        switch (c) {
            case '{':
                return readObject();
            case '[':
                return readArray();
            case '"':
                return scanner.readString();
            case 't':
                return scanner.readKeyword("true", Boolean.TRUE);
            case 'f':
                return scanner.readKeyword("false", Boolean.FALSE);
            case 'n':
                return scanner.readKeyword("null", null);
            default:
                if (c == '-' || JsonScanner.isDigit(c)) {
                    return scanner.readNumber();
                }
                throw scanner.error("unexpected character '" + c + "'");
        }
    }

    private Map<String, Object> readObject() {
        scanner.advance();
        scanner.skipWhitespace();
        Map<String, Object> object = new LinkedHashMap<>();
        if (scanner.peek('}')) {
            return object;
        }
        while (true) {
            scanner.skipWhitespace();
            if (!scanner.hasMore() || scanner.current() != '"') {
                throw scanner.error("expected string key");
            }
            String key = scanner.readString();
            scanner.skipWhitespace();
            if (!scanner.hasMore() || scanner.current() != ':') {
                throw scanner.error("expected ':' after key");
            }
            scanner.advance();
            scanner.skipWhitespace();
            object.put(key, readValue());
            scanner.skipWhitespace();
            if (scanner.peek(',')) {
                continue;
            }
            if (scanner.peek('}')) {
                return object;
            }
            throw scanner.error("expected ',' or '}'");
        }
    }

    private List<Object> readArray() {
        scanner.advance();
        scanner.skipWhitespace();
        List<Object> array = new ArrayList<>();
        if (scanner.peek(']')) {
            return array;
        }
        while (true) {
            scanner.skipWhitespace();
            array.add(readValue());
            scanner.skipWhitespace();
            if (scanner.peek(',')) {
                continue;
            }
            if (scanner.peek(']')) {
                return array;
            }
            throw scanner.error("expected ',' or ']'");
        }
    }
}
