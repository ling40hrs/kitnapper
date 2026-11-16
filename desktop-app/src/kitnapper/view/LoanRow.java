package kitnapper.view;

import java.time.LocalDate;

import kitnapper.model.EquipmentCategory;
import kitnapper.model.LoanStatus;

/**
 * One row of a loans table, resolved by a controller (M4-D29): ids are
 * already turned into item and borrower names, {@code status} is the
 * effective status as of today, and {@code accruedFee} is the service's
 * live or frozen accrual — a view never computes any of these (D21).
 *
 * <p>{@code category} drives the preview's gradient tile only (§8.2).
 * {@code returnDate} is null while the loan is out; {@code penalty} is
 * null until a late fee has been frozen for the loan (at most one, D20).</p>
 */
public record LoanRow(String loanId, String itemName, EquipmentCategory category,
                      String borrowerName, LocalDate borrowDate, LocalDate dueDate,
                      LocalDate returnDate, LoanStatus status, long daysLate,
                      double accruedFee, PenaltyRow penalty) {
}
