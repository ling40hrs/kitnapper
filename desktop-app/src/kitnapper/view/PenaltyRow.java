package kitnapper.view;

import java.time.LocalDate;

import kitnapper.model.PenaltyStatus;

/**
 * One row of the penalties table, resolved by a controller (M4-D29): the
 * loan id, item name and borrower name are resolved from the store and the
 * amount is the frozen figure — the view only formats it (D21).
 *
 * <p>{@code waiveReason} is null unless the penalty was waived (FR-11).</p>
 */
public record PenaltyRow(String penaltyId, String loanId, String itemName,
                         String borrowerName, double amount,
                         PenaltyStatus status, LocalDate assessedDate,
                         String waiveReason) {
}
