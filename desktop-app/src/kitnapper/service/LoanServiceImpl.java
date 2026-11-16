package kitnapper.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import kitnapper.model.Borrower;
import kitnapper.model.Equipment;
import kitnapper.model.EquipmentStatus;
import kitnapper.model.FeeStrategy;
import kitnapper.model.LoanRecord;
import kitnapper.model.LoanStatus;
import kitnapper.model.Manager;
import kitnapper.model.Penalty;
import kitnapper.model.PenaltyType;
import kitnapper.model.User;

/**
 * Borrowing workflows (REQ-004/005/006, FR-04 to FR-07). The borrower comes
 * from the session supplier and the clock is injectable (D4); every mutation
 * saves through {@link DataStore} and then notifies the app context. Reads
 * and lookups are delegated to {@link LoanQueries}.
 */
public final class LoanServiceImpl implements LoanService {

    private static final int LOAN_CAP = 3;

    private final DataStore store;
    private final FeeStrategy feeStrategy;
    private final Supplier<User> session;
    private final Runnable onChange;
    private final LoanQueries queries;

    public LoanServiceImpl(DataStore store, FeeStrategy feeStrategy,
                           Supplier<User> session, Supplier<LocalDate> today,
                           Runnable onChange) {
        this.store = Objects.requireNonNull(store, "store");
        this.feeStrategy = Objects.requireNonNull(feeStrategy, "feeStrategy");
        this.session = Objects.requireNonNull(session, "session");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
        this.queries = new LoanQueries(store, Objects.requireNonNull(today, "today"));
    }

    @Override
    public LoanRecord checkout(String equipmentId, LocalDate dueDate) {
        LocalDate borrowDate = queries.today();
        User user = session.get();
        if (!(user instanceof Borrower)) {
            throw new IllegalStateException(LoanMessages.BORROWER_ONLY);
        }
        if (dueDate == null) {
            throw new IllegalArgumentException(LoanMessages.window(borrowDate));
        }
        Equipment item = queries.equipmentOrNull(equipmentId);
        if (item == null) {
            throw new IllegalArgumentException(LoanMessages.ITEM_MISSING);
        }
        if (!item.isBorrowable()) {
            throw new IllegalStateException(LoanMessages.ITEM_UNAVAILABLE);
        }
        long active = store.loans().stream()
                .filter(loan -> loan.getBorrowerId().equals(user.getUserId()))
                .filter(loan -> loan.effectiveStatus(borrowDate) != LoanStatus.RETURNED)
                .count();
        if (active >= LOAN_CAP) {
            throw new IllegalStateException(LoanMessages.LOAN_CAP_REACHED);
        }
        LoanRecord loan;
        try {
            loan = LoanRecord.checkout(Ids.nextLoanId(store.loans()),
                    user.getUserId(), item.getId(), borrowDate, dueDate);
        } catch (IllegalArgumentException invalidWindow) {
            throw new IllegalArgumentException(LoanMessages.window(borrowDate), invalidWindow);
        }
        item.markLoaned();
        store.loans().add(loan);
        store.save();
        onChange.run();
        return loan;
    }

    @Override
    public void returnLoan(String loanId, LocalDate returnDate) {
        LocalDate asOf = queries.today();
        User user = session.get();
        if (!(user instanceof Manager)) {
            throw new IllegalStateException(LoanMessages.MANAGER_ONLY);
        }
        LoanRecord loan = queries.findLoan(loanId);
        if (loan.getLoanStatus() == LoanStatus.RETURNED) {
            throw new IllegalStateException(LoanMessages.ALREADY_RETURNED);
        }
        if (returnDate == null) {
            throw new IllegalArgumentException(LoanMessages.RETURN_REQUIRED);
        }
        if (returnDate.isAfter(asOf)) {
            throw new IllegalArgumentException(LoanMessages.RETURN_FUTURE);
        }
        if (returnDate.isBefore(loan.getBorrowDate())) {
            throw new IllegalArgumentException(LoanMessages.RETURN_BEFORE_BORROW);
        }
        Equipment item = queries.equipmentOrNull(loan.getEquipmentId());
        if (item == null) {
            throw new IllegalStateException(LoanMessages.EQUIPMENT_MISSING);
        }
        if (item.getStatus() == EquipmentStatus.DECOMMISSIONED) {
            throw new IllegalStateException(LoanMessages.EQUIPMENT_DECOMMISSIONED);
        }
        double fee = loan.accruedFee(returnDate, feeStrategy);
        loan.completeReturn(returnDate);
        if (fee > 0) {
            store.penalties().add(new Penalty(Ids.nextPenaltyId(store.penalties()),
                    loan.getLoanId(), PenaltyType.LATE_FEE, fee, returnDate));
        }
        if (item.getStatus() == EquipmentStatus.LOANED) {
            item.markAvailable();
        }
        store.save();
        onChange.run();
    }

    @Override
    public List<LoanRecord> myLoans() {
        User user = session.get();
        if (user == null) {
            return new ArrayList<>();
        }
        return queries.myLoans(user.getUserId());
    }

    @Override
    public List<LoanRecord> allLoans() {
        return queries.allLoans();
    }

    @Override
    public List<LoanRecord> overdueLoans() {
        return queries.overdueLoans();
    }

    @Override
    public double accruedFee(String loanId) {
        return queries.accruedFee(loanId, feeStrategy);
    }

    @Override
    public double accruedFee(String loanId, LocalDate asOf) {
        return queries.accruedFee(loanId, asOf, feeStrategy);
    }
}
