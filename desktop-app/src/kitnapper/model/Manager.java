package kitnapper.model;

/**
 * An equipment manager account. Return logging, penalty settlement and
 * inventory behavior live in the service layer (decision D6); this
 * subclass contributes the role identity.
 */
public final class Manager extends User {

    public Manager(String userId, String fullName, String email, String password) {
        super(userId, fullName, email, password);
    }

    @Override
    public UserRole getRole() {
        return UserRole.MANAGER;
    }
}
