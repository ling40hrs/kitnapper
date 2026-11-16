package kitnapper.service;

import java.util.List;

import kitnapper.model.Penalty;
import kitnapper.model.PenaltyOutcome;

/**
 * Penalty reads and settlement (REQ-011, FR-11, M4-D1/D31). Reads are open
 * to any signed-in session; {@link #settle} is manager-only (INV-3) and
 * one-way — a PAID or WAIVED penalty can never be settled again. Views get
 * penalty amounts and statuses only from here (D21); the model's
 * {@code markPaid}/{@code waive} stay the only status writers.
 */
public interface PenaltyService {

    /** Every penalty, newest assessed first (manager view). */
    List<Penalty> allPenalties();

    /** The penalties of one loan; empty for a blank or unknown loan id. */
    List<Penalty> penaltiesForLoan(String loanId);

    /** The session borrower's penalties, derived from their own loans. */
    List<Penalty> myPenalties();

    /** Sum of UNPAID amounts, in PHP. */
    double unpaidTotal();

    /**
     * Settles one penalty: PAID marks it paid, WAIVED requires a non-blank
     * reason. Manager-only; saves and notifies on success. Unknown id
     * throws {@link IllegalArgumentException}, everything else
     * {@link IllegalStateException} with a user-facing message.
     */
    void settle(String penaltyId, PenaltyOutcome outcome, String reason);
}
