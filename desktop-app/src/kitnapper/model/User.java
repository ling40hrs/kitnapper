package kitnapper.model;

import java.util.Objects;

/**
 * Abstract account holder. Borrower and Manager inherit from it so that
 * role-dependent behavior is resolved by dynamic dispatch (PRD 3.0).
 *
 * <p>Instances are immutable: there is no account management in scope
 * (PRD 8), so every field is set once by the constructor.</p>
 */
public abstract class User {

    private final String userId;
    private final String fullName;
    private final String email;
    private final String password;

    /**
     * @param userId   unique identifier, non-blank
     * @param fullName display name, non-blank
     * @param email    login identifier, non-blank
     * @param password mock credential compared at login (PRD 9.1), non-blank;
     *                 stored in plain text by design, never hashed
     */
    protected User(String userId, String fullName, String email, String password) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.fullName = requireNonBlank(fullName, "fullName");
        this.email = requireNonBlank(email, "email");
        this.password = requireNonBlank(password, "password");
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Mock credential, exposed only for JSON persistence and the seeded
     * login check. Real authentication is a strict non-goal (PRD 8).
     */
    public String getPassword() {
        return password;
    }

    /** Role of this account; implemented by each concrete subclass. */
    public abstract UserRole getRole();

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
