package kitnapper.model;

/**
 * Loan states. Only ACTIVE and RETURNED are ever persisted; OVERDUE is
 * derived at read time (PRD 4.2, decision D1) and must never be written
 * to the JSON save file.
 */
public enum LoanStatus {
    ACTIVE,
    OVERDUE,
    RETURNED
}
