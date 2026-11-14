package kitnapper.service.persistence;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Typed accessors over the parser/writer representation: Map, List,
 * String, Double, Boolean, null. Every failed expectation throws
 * IllegalArgumentException, which the loader treats as corruption (D18)
 * and answers with a fresh seed instead of a crash.
 */
public final class Json {

    private Json() {
    }

    /** @param what field name or context used in the error message */
    public static Map<String, Object> asObject(Object value, String what) {
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException(what + " must be a JSON object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        return map;
    }

    public static List<Object> asList(Object value, String what) {
        if (!(value instanceof List)) {
            throw new IllegalArgumentException(what + " must be a JSON array");
        }
        return (List<Object>) value;
    }

    /** Array field that must be present. */
    public static List<Object> requireList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing field: " + key);
        }
        return asList(value, key);
    }

    /** String field that must be present and non-blank. */
    public static String requireString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Missing or blank string field: " + key);
        }
        return text;
    }

    /** String field that may be absent or null. */
    public static String optString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException("Field must be a string or null: " + key);
        }
        return text;
    }

    /** Numeric field that must be present (the parser yields doubles). */
    public static double requireDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Missing or non-numeric field: " + key);
        }
        return number.doubleValue();
    }

    /** Numeric field that must be a whole number before narrowing to int. */
    public static int requireInt(Map<String, Object> map, String key) {
        double raw = requireDouble(map, key);
        if (raw != Math.rint(raw)) {
            throw new IllegalArgumentException("Field must be an integer: " + key);
        }
        return (int) raw;
    }

    /** ISO yyyy-MM-dd field that must be present. */
    public static LocalDate requireDate(Map<String, Object> map, String key) {
        String raw = requireString(map, key);
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Field is not an ISO date: " + key);
        }
    }

    /** ISO yyyy-MM-dd field that may be absent or null. */
    public static LocalDate optDate(Map<String, Object> map, String key) {
        return map.get(key) == null ? null : requireDate(map, key);
    }
}
