package kitnapper.service;

import java.time.LocalDate;
import java.util.List;

import kitnapper.model.LoanRecord;

/**
 * Borrowing workflows (REQ-004/005/006, FR-04 to FR-07). The borrower is
 * the current session user and the borrow date is {@code LocalDate.now()}
 * (D4); implementations enforce INV-3, INV-6 and the 14-day window by
 * creating loans through {@code LoanRecord.checkout}. Views get derived
 * status and fees only from this service (D21) — they never compute them
 * from entities themselves.
 */
public interface LoanService {

    /** Checks the session borrower out of an available item (INV-1/2/6). */
    LoanRecord checkout(String equipmentId, LocalDate dueDate);

    /** Ends a loan (manager only, INV-3); freezes at most one LATE_FEE penalty (FR-07). */
    void returnLoan(String loanId, LocalDate returnDate);

    /** The session borrower's own loans (REQ-005). */
    List<LoanRecord> myLoans();

    /** Every loan, any status (manager view, REQ-009). */
    List<LoanRecord> allLoans();

    /** Loans past their due date, longest overdue first (REQ-008). */
    List<LoanRecord> overdueLoans();

    /** Live accrued fee for a loan, computed via the FeeStrategy (D21). */
    double accruedFee(String loanId);
}
