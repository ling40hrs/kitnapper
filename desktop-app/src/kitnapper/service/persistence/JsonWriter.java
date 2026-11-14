package kitnapper.service.persistence;

import java.util.List;
import java.util.Map;

/**
 * Pretty-printed JSON writer (implementation plan 3.2): two-space indent,
 * insertion-ordered objects, strings escaping exactly what this app emits
 * (backslash, double quote, newline), numbers as plain decimals. The
 * parser accepts exactly the output of this writer and nothing more.
 */
public final class JsonWriter {

    private JsonWriter() {
    }

    /** Renders a supported value: Map, List, String, Double, Integer, Boolean, or null. */
    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(out, value, 0);
        return out.toString();
    }

    private static void writeValue(StringBuilder out, Object value, int indent) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String text) {
            writeString(out, text);
        } else if (value instanceof Boolean bool) {
            out.append(bool);
        } else if (value instanceof Double number) {
            out.append(number);
        } else if (value instanceof Integer number) {
            out.append(number);
        } else if (value instanceof Map<?, ?> map) {
            writeObject(out, map, indent);
        } else if (value instanceof List<?> list) {
            writeArray(out, list, indent);
        } else {
            throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
        }
    }

    private static void writeObject(StringBuilder out, Map<?, ?> map, int indent) {
        if (map.isEmpty()) {
            out.append("{}");
            return;
        }
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            newline(out, indent + 1);
            writeString(out, String.valueOf(entry.getKey()));
            out.append(": ");
            writeValue(out, entry.getValue(), indent + 1);
        }
        newline(out, indent);
        out.append('}');
    }

    private static void writeArray(StringBuilder out, List<?> list, int indent) {
        if (list.isEmpty()) {
            out.append("[]");
            return;
        }
        out.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            newline(out, indent + 1);
            writeValue(out, list.get(i), indent + 1);
        }
        newline(out, indent);
        out.append(']');
    }

    private static void newline(StringBuilder out, int indent) {
        out.append('\n');
        out.append("  ".repeat(indent));
    }

    private static void writeString(StringBuilder out, String text) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                default -> out.append(c);
            }
        }
        out.append('"');
    }
}
