package kitnapper.model;

/**
 * The two account roles. Routing, permissions and the INV-3 checks use
 * this enum; the string identifiers in the JSON use the same names.
 */
public enum UserRole {
    BORROWER,
    MANAGER
}
