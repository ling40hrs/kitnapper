package kitnapper.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * A monetary penalty attached to exactly one loan. The amount is computed
 * by the fee strategy and frozen here; it is never hand-editable
 * (INV-4, FR-07).
 */
public final class Penalty {

    private final String penaltyId;
    private final String loanId;
    private final PenaltyType type;
    private final double amount;
    private PenaltyStatus status;
    private final LocalDate assessedDate;
    private String waiveReason;

    /**
     * @param penaltyId    unique identifier, non-blank
     * @param loanId       the loan the fee belongs to, non-blank
     * @param type         fee type, non-null
     * @param amount       PHP amount, finite and non-negative (INV-4)
     * @param assessedDate the date the fee was frozen (the return date)
     */
    public Penalty(String penaltyId, String loanId, PenaltyType type,
                   double amount, LocalDate assessedDate) {
        this.penaltyId = requireNonBlank(penaltyId, "penaltyId");
        this.loanId = requireNonBlank(loanId, "loanId");
        this.type = Objects.requireNonNull(type, "type");
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("amount must be a finite non-negative number");
        }
        this.amount = amount;
        this.status = PenaltyStatus.UNPAID;
        this.assessedDate = Objects.requireNonNull(assessedDate, "assessedDate");
        this.waiveReason = null;
    }

    public String getPenaltyId() {
        return penaltyId;
    }

    public String getLoanId() {
        return loanId;
    }

    public PenaltyType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public PenaltyStatus getStatus() {
        return status;
    }

    public LocalDate getAssessedDate() {
        return assessedDate;
    }

    /** Present only when the penalty was waived. */
    public Optional<String> getWaiveReason() {
        return Optional.ofNullable(waiveReason);
    }

    /** UNPAID -> PAID. A settled penalty cannot change again. */
    public void markPaid() {
        if (status != PenaltyStatus.UNPAID) {
            throw new IllegalStateException("Penalty " + penaltyId + " is already settled");
        }
        status = PenaltyStatus.PAID;
    }

    /** UNPAID -> WAIVED. A non-blank reason is required (FR-11). */
    public void waive(String reason) {
        if (status != PenaltyStatus.UNPAID) {
            throw new IllegalStateException("Penalty " + penaltyId + " is already settled");
        }
        Objects.requireNonNull(reason, "reason");
        String trimmed = reason.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        status = PenaltyStatus.WAIVED;
        waiveReason = trimmed;
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
