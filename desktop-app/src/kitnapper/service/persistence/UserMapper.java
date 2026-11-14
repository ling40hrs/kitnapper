package kitnapper.service.persistence;

import java.util.LinkedHashMap;
import java.util.Map;

import kitnapper.model.Borrower;
import kitnapper.model.Manager;
import kitnapper.model.User;

/**
 * Maps users to JSON and back. The {@code role} field selects the concrete
 * subclass on read (polymorphic restore); an unknown role is corruption
 * (D18).
 */
public final class UserMapper {

    public Map<String, Object> toDocument(User user) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("userId", user.getUserId());
        doc.put("fullName", user.getFullName());
        doc.put("email", user.getEmail());
        doc.put("password", user.getPassword());
        doc.put("role", user.getRole().name());
        return doc;
    }

    public User fromDocument(Object value) {
        Map<String, Object> doc = Json.asObject(value, "user");
        String role = Json.requireString(doc, "role");
        return switch (role) {
            case "BORROWER" -> new Borrower(
                    Json.requireString(doc, "userId"),
                    Json.requireString(doc, "fullName"),
                    Json.requireString(doc, "email"),
                    Json.requireString(doc, "password"));
            case "MANAGER" -> new Manager(
                    Json.requireString(doc, "userId"),
                    Json.requireString(doc, "fullName"),
                    Json.requireString(doc, "email"),
                    Json.requireString(doc, "password"));
            default -> throw new IllegalArgumentException("Unknown role: " + role);
        };
    }
}
