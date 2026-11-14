package kitnapper.model;

/**
 * A borrower account. Loan-creating behavior lives in the service layer
 * (decision D6); this subclass contributes the role identity and keeps the
 * User hierarchy polymorphic.
 */
public final class Borrower extends User {

    public Borrower(String userId, String fullName, String email, String password) {
        super(userId, fullName, email, password);
    }

    @Override
    public UserRole getRole() {
        return UserRole.BORROWER;
    }
}
