package kitnapper.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * A single borrow transaction. Created only through {@link #checkout},
 * which validates INV-2; only {@link #completeReturn} can end a loan
 * (INV-5). Loans cannot be cancelled or extended (PRD 5.5).
 *
 * <p>Only ACTIVE and RETURNED are ever persisted; OVERDUE is derived by
 * {@link #effectiveStatus(LocalDate)} (decision D1) and is never stored.</p>
 */
public final class LoanRecord {

    private final String loanId;
    private final String borrowerId;
    private final String equipmentId;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;
    private LocalDate returnDate;
    private LoanStatus loanStatus;

    private LoanRecord(String loanId, String borrowerId, String equipmentId,
                       LocalDate borrowDate, LocalDate dueDate) {
        this.loanId = requireNonBlank(loanId, "loanId");
        this.borrowerId = requireNonBlank(borrowerId, "borrowerId");
        this.equipmentId = requireNonBlank(equipmentId, "equipmentId");
        this.borrowDate = Objects.requireNonNull(borrowDate, "borrowDate");
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate");
        this.returnDate = null;
        this.loanStatus = LoanStatus.ACTIVE;
    }

    /**
     * Creates an active loan. INV-2: {@code borrowDate <= dueDate} and the
     * window may not exceed 14 days (REQ-004). The 3-loan cap (INV-6) needs
     * the borrower's full history, so the service layer enforces it.
     */
    public static LoanRecord checkout(String loanId, String borrowerId,
                                      String equipmentId, LocalDate borrowDate,
                                      LocalDate dueDate) {
        if (dueDate.isBefore(borrowDate)) {
            throw new IllegalArgumentException("dueDate must not precede borrowDate");
        }
        if (ChronoUnit.DAYS.between(borrowDate, dueDate) > 14) {
            throw new IllegalArgumentException("dueDate must be within 14 days of borrowDate");
        }
        return new LoanRecord(loanId, borrowerId, equipmentId, borrowDate, dueDate);
    }

    /**
     * Ends the loan. Rejected on a second return or a date before the
     * borrow date. No damage assessment is performed (PRD 5.5).
     */
    public void completeReturn(LocalDate returnDate) {
        if (loanStatus == LoanStatus.RETURNED) {
            throw new IllegalStateException("Loan " + loanId + " is already returned");
        }
        Objects.requireNonNull(returnDate, "returnDate");
        if (returnDate.isBefore(borrowDate)) {
            throw new IllegalArgumentException("returnDate must not precede borrowDate");
        }
        this.returnDate = returnDate;
        this.loanStatus = LoanStatus.RETURNED;
    }

    public String getLoanId() {
        return loanId;
    }

    public String getBorrowerId() {
        return borrowerId;
    }

    public String getEquipmentId() {
        return equipmentId;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    /** Empty while the loan is outstanding. */
    public Optional<LocalDate> getReturnDate() {
        return Optional.ofNullable(returnDate);
    }

    /** Persisted status; ACTIVE or RETURNED only (OVERDUE is derived, D1). */
    public LoanStatus getLoanStatus() {
        return loanStatus;
    }

    /** Current status as of {@code asOf}, deriving OVERDUE past the due date. */
    public LoanStatus effectiveStatus(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        if (loanStatus == LoanStatus.RETURNED) {
            return LoanStatus.RETURNED;
        }
        return asOf.isAfter(dueDate) ? LoanStatus.OVERDUE : LoanStatus.ACTIVE;
    }

    /** True only while the loan is active and past its due date. */
    public boolean isOverdue(LocalDate asOf) {
        return effectiveStatus(asOf) == LoanStatus.OVERDUE;
    }

    /**
     * Whole days past the due date; 0 on or before it. Frozen at the return
     * date once returned, so the count never grows after the loan ends.
     */
    public long daysOverdue(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        LocalDate end = (returnDate != null) ? returnDate : asOf;
        return end.isAfter(dueDate) ? ChronoUnit.DAYS.between(dueDate, end) : 0;
    }

    /** Live late fee as of {@code asOf}; delegates to the pluggable rule. */
    public double accruedFee(LocalDate asOf, FeeStrategy strategy) {
        Objects.requireNonNull(asOf, "asOf");
        Objects.requireNonNull(strategy, "strategy");
        return strategy.calculate(this, asOf);
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
