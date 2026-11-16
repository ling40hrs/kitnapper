package kitnapper.controller;

import java.util.List;

import kitnapper.model.LoanStatus;
import kitnapper.model.PenaltyStatus;
import kitnapper.service.AppContext;
import kitnapper.view.ActivityEntry;
import kitnapper.view.DashboardScreen;
import kitnapper.view.Formats;
import kitnapper.view.LoanRow;

/**
 * Backs the manager Dashboard: KPI counts, the overdue table and the
 * derived activity list (plan §5.4). Counts come from
 * {@link kitnapper.service.LoanService} and
 * {@link kitnapper.service.PenaltyService}; the screen does no entity math
 * and money is formatted here with {@link Formats}.
 */
public final class DashboardController implements DashboardScreen.Actions {

    private final AppContext context;
    private final LoanRows rows;
    private Runnable onChanged;

    public DashboardController(AppContext context) {
        this.context = context;
        this.rows = new LoanRows(context);
        context.addChangeListener(this::notifyChanged);
    }

    /** Loans not returned yet ("out now"). */
    public int loansOut() {
        return (int) context.loans().allLoans().stream()
                .filter(loan -> loan.getLoanStatus() != LoanStatus.RETURNED)
                .count();
    }

    /** Loans past their due date right now. */
    public int overdueCount() {
        return context.loans().overdueLoans().size();
    }

    /** Penalties still awaiting settlement. */
    public int unpaidCount() {
        return (int) context.penalties().allPenalties().stream()
                .filter(penalty -> penalty.getStatus() == PenaltyStatus.UNPAID)
                .count();
    }

    /** Unpaid total as money text for the KPI caption. */
    public String unpaidTotal() {
        return Formats.money(context.penalties().unpaidTotal());
    }

    /** Overdue rows for the dashboard table, longest overdue first. */
    public List<LoanRow> overdueLoans() {
        return rows.toLoanRows(context.loans().overdueLoans());
    }

    /** The last six borrow/return events, newest first (D28). */
    public List<ActivityEntry> activity() {
        return rows.activity();
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
