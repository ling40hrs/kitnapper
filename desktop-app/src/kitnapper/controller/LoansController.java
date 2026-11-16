package kitnapper.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import kitnapper.model.Borrower;
import kitnapper.model.LoanStatus;
import kitnapper.model.User;
import kitnapper.service.AppContext;
import kitnapper.view.LoanRow;

/**
 * Backs My Loans, All Loans and Overdue (M4-D3/D10). Rows are resolved
 * through {@link LoanRows}; returns delegate to {@link BorrowingController}
 * so failure translation stays in one place. The screen Actions interfaces
 * are attached as the screens land (WP3/WP4); the status sink lives in the
 * screens, never here.
 */
public final class LoansController {

    private final AppContext context;
    private final LoanRows rows;
    private final BorrowingController borrowing;
    private Runnable onChanged;

    public LoansController(AppContext context) {
        this.context = context;
        this.rows = new LoanRows(context);
        this.borrowing = new BorrowingController(context);
        context.addChangeListener(this::notifyChanged);
    }

    /** The session borrower's loans in one tab, borrow-date order. */
    public List<LoanRow> myLoans(LoanStatus tab) {
        User user = context.currentUser();
        if (!(user instanceof Borrower)) {
            return new ArrayList<>();
        }
        List<LoanRow> result = new ArrayList<>();
        for (LoanRow row : rows.toLoanRows(context.loans().myLoans())) {
            if (row.status() == tab) {
                result.add(row);
            }
        }
        return result;
    }

    /** Every loan, service order (borrow date descending). */
    public List<LoanRow> allLoans() {
        return rows.toLoanRows(context.loans().allLoans());
    }

    /** Overdue loans, longest overdue first (service order). */
    public List<LoanRow> overdueLoans() {
        return rows.toLoanRows(context.loans().overdueLoans());
    }

    /**
     * Valid return dates for the loan: borrow date … today, ascending.
     * Always at least one date (today) even on clock skew.
     */
    public List<LocalDate> returnDates(LoanRow row) {
        if (row == null) {
            return new ArrayList<>();
        }
        LocalDate today = context.today();
        LocalDate start = row.borrowDate().isAfter(today)
                ? today : row.borrowDate();
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(today);
                date = date.plusDays(1)) {
            dates.add(date);
        }
        return dates;
    }

    /** Logs the return; empty means success, otherwise the failure message. */
    public Optional<String> returnLoan(LoanRow row, LocalDate returnDate) {
        return borrowing.returnLoan(row.loanId(), returnDate);
    }

    /**
     * The live fee as of a chosen return date (§8.8), computed by
     * {@code LoanService.accruedFee(loanId, asOf)} so the dialog's preview and
     * the frozen penalty can never disagree (D21).
     */
    public double feeOn(LoanRow row, LocalDate date) {
        return context.loans().accruedFee(row.loanId(), date);
    }

    /** The screen reload hook, fired after every app-level change. */
    public void setOnChanged(Runnable listener) {
        onChanged = listener;
    }

    private void notifyChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }
}
