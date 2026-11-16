package kitnapper.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.LoanRecord;
import kitnapper.model.Penalty;
import kitnapper.service.AppContext;
import kitnapper.view.ActivityEntry;
import kitnapper.view.LoanRow;
import kitnapper.view.PenaltyRow;

/**
 * The single place loan and penalty ids become names, statuses and fees
 * (M4-D29). Controllers call these helpers to build the view records; fees
 * always come from {@link kitnapper.service.LoanService} (D21) and the
 * dashboard activity is derived here per D28. Package-private: views never
 * see this class.
 */
final class LoanRows {

    private static final int ACTIVITY_LIMIT = 6;

    private final AppContext context;

    LoanRows(AppContext context) {
        this.context = context;
    }

    /** Resolves each loan in the list's existing order. */
    List<LoanRow> toLoanRows(List<LoanRecord> loans) {
        List<LoanRow> rows = new ArrayList<>();
        for (LoanRecord loan : loans) {
            rows.add(toLoanRow(loan));
        }
        return rows;
    }

    LoanRow toLoanRow(LoanRecord loan) {
        LocalDate today = context.today();
        Equipment item = itemOrNull(loan.getEquipmentId());
        return new LoanRow(loan.getLoanId(), nameOf(item), categoryOf(item),
                borrowerName(loan.getBorrowerId()), loan.getBorrowDate(),
                loan.getDueDate(), loan.getReturnDate().orElse(null),
                loan.effectiveStatus(today), loan.daysOverdue(today),
                context.loans().accruedFee(loan.getLoanId()),
                penaltyOf(loan.getLoanId()));
    }

    /** Resolves each penalty in the list's existing order. */
    List<PenaltyRow> toPenaltyRows(List<Penalty> penalties) {
        List<PenaltyRow> rows = new ArrayList<>();
        for (Penalty penalty : penalties) {
            rows.add(toPenaltyRow(penalty));
        }
        return rows;
    }

    PenaltyRow toPenaltyRow(Penalty penalty) {
        LoanRecord loan = loanOrNull(penalty.getLoanId());
        return new PenaltyRow(penalty.getPenaltyId(), penalty.getLoanId(),
                itemName(loan == null ? null : loan.getEquipmentId()),
                borrowerName(loan == null ? null : loan.getBorrowerId()),
                penalty.getAmount(), penalty.getStatus(),
                penalty.getAssessedDate(),
                penalty.getWaiveReason().orElse(null));
    }

    /**
     * The last six borrow/return events, newest first (D28). Each loan
     * contributes a checkout at its borrow date and, once returned, a
     * return at the return date with the frozen fee. Ties break by loan id
     * descending, then returns before checkouts on the same day.
     */
    List<ActivityEntry> activity() {
        List<ActivityEntry> events = new ArrayList<>();
        for (LoanRecord loan : context.dataStore().loans()) {
            String item = itemName(loan.getEquipmentId());
            String borrower = borrowerName(loan.getBorrowerId());
            events.add(new ActivityEntry(loan.getBorrowDate(), loan.getLoanId(),
                    item, borrower, true, 0.0));
            loan.getReturnDate().ifPresent(returned -> events.add(
                    new ActivityEntry(returned, loan.getLoanId(), item,
                            borrower, false, frozenFee(loan))));
        }
        events.sort(Comparator
                .comparing((ActivityEntry event) -> event.date())
                .reversed()
                .thenComparing(ActivityEntry::loanId, Comparator.reverseOrder())
                .thenComparing(ActivityEntry::checkout));
        return List.copyOf(events.subList(0, Math.min(ACTIVITY_LIMIT,
                events.size())));
    }

    /** The loan's frozen penalty row, or null while none exists (D20). */
    private PenaltyRow penaltyOf(String loanId) {
        List<Penalty> penalties = context.penalties().penaltiesForLoan(loanId);
        return penalties.isEmpty() ? null : toPenaltyRow(penalties.get(0));
    }

    private double frozenFee(LoanRecord loan) {
        PenaltyRow penalty = penaltyOf(loan.getLoanId());
        return penalty == null ? 0.0 : penalty.amount();
    }

    private LoanRecord loanOrNull(String loanId) {
        return context.dataStore().loans().stream()
                .filter(loan -> loan.getLoanId().equals(loanId))
                .findFirst()
                .orElse(null);
    }

    private String itemName(String equipmentId) {
        return nameOf(itemOrNull(equipmentId));
    }

    private Equipment itemOrNull(String equipmentId) {
        return context.dataStore().equipment().stream()
                .filter(candidate -> candidate.getId().equals(equipmentId))
                .findFirst()
                .orElse(null);
    }

    private static String nameOf(Equipment item) {
        return item == null ? "Unknown item" : item.getName();
    }

    /** The preview tile's category; OTHER when the item is missing (§8.2). */
    private static EquipmentCategory categoryOf(Equipment item) {
        return item == null ? EquipmentCategory.OTHER : item.getCategory();
    }

    private String borrowerName(String borrowerId) {
        return context.dataStore().users().stream()
                .filter(user -> user.getUserId().equals(borrowerId))
                .findFirst()
                .map(user -> user.getFullName())
                .orElse("Unknown borrower");
    }
}
