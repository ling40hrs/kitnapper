package kitnapper.service;

import java.util.Optional;

import kitnapper.model.User;

/**
 * Mock login (REQ-003, FR-03): accepts only the email/password pairs held
 * in the loaded dataset. Real authentication is a strict non-goal (PRD 8).
 */
public interface AuthService {

    /**
     * @param email    login identifier; compared trimmed and
     *                 case-insensitively
     * @param password mock credential; compared exactly
     * @return the matching user, or empty when the credentials do not
     *         match (or either argument is null/blank)
     */
    Optional<User> login(String email, String password);
}
