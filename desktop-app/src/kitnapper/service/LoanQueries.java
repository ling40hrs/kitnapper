package kitnapper.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import kitnapper.model.Equipment;
import kitnapper.model.FeeStrategy;
import kitnapper.model.LoanRecord;

/**
 * Read-side queries over the live dataset (D21): entity lookups, the sorted
 * loan lists the screens render, and live fee accrual. Mutations stay in
 * {@link LoanServiceImpl}.
 */
final class LoanQueries {

    private static final Comparator<LoanRecord> BORROW_DATE_DESC =
            Comparator.comparing(LoanRecord::getBorrowDate).reversed()
                    .thenComparing(LoanRecord::getLoanId);

    private final DataStore store;
    private final Supplier<LocalDate> today;

    LoanQueries(DataStore store, Supplier<LocalDate> today) {
        this.store = Objects.requireNonNull(store, "store");
        this.today = Objects.requireNonNull(today, "today");
    }

    LocalDate today() {
        return Objects.requireNonNull(today.get(), "today");
    }

    Equipment equipmentOrNull(String equipmentId) {
        if (equipmentId == null || equipmentId.isBlank()) {
            return null;
        }
        return store.equipment().stream()
                .filter(item -> item.getId().equals(equipmentId))
                .findFirst()
                .orElse(null);
    }

    LoanRecord findLoan(String loanId) {
        if (loanId == null || loanId.isBlank()) {
            throw new IllegalArgumentException(LoanMessages.LOAN_MISSING);
        }
        return store.loans().stream()
                .filter(loan -> loan.getLoanId().equals(loanId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(LoanMessages.LOAN_MISSING));
    }

    List<LoanRecord> myLoans(String borrowerId) {
        return store.loans().stream()
                .filter(loan -> loan.getBorrowerId().equals(borrowerId))
                .sorted(BORROW_DATE_DESC)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    List<LoanRecord> allLoans() {
        return store.loans().stream()
                .sorted(BORROW_DATE_DESC)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    List<LoanRecord> overdueLoans() {
        LocalDate asOf = today();
        return store.loans().stream()
                .filter(loan -> loan.isOverdue(asOf))
                .sorted(Comparator.comparingLong((LoanRecord loan) -> loan.daysOverdue(asOf))
                        .reversed()
                        .thenComparing(LoanRecord::getDueDate)
                        .thenComparing(LoanRecord::getLoanId))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    double accruedFee(String loanId, FeeStrategy feeStrategy) {
        return accruedFee(loanId, today(), feeStrategy);
    }

    /** The fee as of an explicit date; frozen at the return date once returned. */
    double accruedFee(String loanId, LocalDate asOf, FeeStrategy feeStrategy) {
        return findLoan(loanId).accruedFee(asOf, feeStrategy);
    }
}
