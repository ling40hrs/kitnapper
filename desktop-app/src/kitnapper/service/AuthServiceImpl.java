package kitnapper.service;

import java.util.Objects;
import java.util.Optional;

import kitnapper.model.User;

/**
 * Authenticates against the users loaded from {@link DataStore}, never a
 * hard-coded list (D23): the seeded accounts in the JSON save file are the
 * only ones that log in.
 */
public final class AuthServiceImpl implements AuthService {

    private final DataStore store;

    public AuthServiceImpl(DataStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    @Override
    public Optional<User> login(String email, String password) {
        if (email == null || password == null) {
            return Optional.empty();
        }
        String trimmed = email.strip();
        if (trimmed.isEmpty() || password.isEmpty()) {
            return Optional.empty();
        }
        for (User user : store.users()) {
            if (user.getEmail().equalsIgnoreCase(trimmed)
                && user.getPassword().equals(password)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }
}
