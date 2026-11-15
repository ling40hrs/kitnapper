package kitnapper.service.persistence;

/**
 * Character-level scanner for the JSON subset {@link JsonWriter} emits
 * (implementation plan §3.2): strings with the three supported escapes,
 * plain-decimal numbers, and the true/false/null keywords. Structure
 * (objects, arrays) lives in {@link JsonParser}.
 */
final class JsonScanner {

    private final String text;
    private int pos;

    JsonScanner(String text) {
        this.text = text;
    }

    boolean hasMore() {
        return pos < text.length();
    }

    char current() {
        return text.charAt(pos);
    }

    void advance() {
        pos++;
    }

    Object readKeyword(String keyword, Object value) {
        if (!text.startsWith(keyword, pos)) {
            throw error("invalid literal");
        }
        pos += keyword.length();
        return value;
    }

    Object readNumber() {
        int start = pos;
        if (current() == '-') {
            pos++;
        }
        if (!hasMore() || !isDigit(current())) {
            throw error("invalid number");
        }
        while (hasMore() && isDigit(current())) {
            pos++;
        }
        if (hasMore() && current() == '.') {
            pos++;
            if (!hasMore() || !isDigit(current())) {
                throw error("invalid number fraction");
            }
            while (hasMore() && isDigit(current())) {
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

    String readString() {
        pos++;
        StringBuilder out = new StringBuilder();
        while (true) {
            if (!hasMore()) {
                throw error("unterminated string");
            }
            char c = text.charAt(pos++);
            if (c == '"') {
                return out.toString();
            }
            if (c == '\\') {
                if (!hasMore()) {
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

    void skipWhitespace() {
        while (hasMore() && Character.isWhitespace(current())) {
            pos++;
        }
    }

    boolean peek(char c) {
        if (hasMore() && current() == c) {
            pos++;
            return true;
        }
        return false;
    }

    static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    IllegalArgumentException error(String message) {
        return new IllegalArgumentException("JSON parse error at offset " + pos + ": " + message);
    }
}
