package kitnapper;

import java.time.LocalDate;
import java.util.List;

import kitnapper.controller.DashboardController;
import kitnapper.controller.LoansController;
import kitnapper.model.LoanStatus;
import kitnapper.model.PenaltyStatus;
import kitnapper.service.AppContext;
import kitnapper.service.DataStore;
import kitnapper.view.ActivityEntry;
import kitnapper.view.LoanRow;

/**
 * Dashboard checks for {@link LoanProbe}: KPI counts and the derived
 * activity list (D28) on the seed state and after the returns.
 */
final class DashboardChecks {

    private static final String ADMIN = "admin@institution.edu";
    private static final String STUDENT1 = "student1@institution.edu";

    private DashboardChecks() {
    }

    /** Seed: 3 out / 1 overdue / 0 unpaid, three checkout events. */
    static void seed(DashboardController dashboard) {
        ProbeSupport.check("dashboard-seed", dashboard.loansOut() == 3
                && dashboard.overdueCount() == 1
                && dashboard.unpaidCount() == 0
                && dashboard.unpaidTotal().equals("₱0.00"));
        List<ActivityEntry> activity = dashboard.activity();
        ProbeSupport.check("dashboard-activity-seed", activity.size() == 3
                && activity.get(0).loanId().equals("L-001")
                && activity.get(0).checkout()
                && activity.stream().allMatch(event -> event.checkout()
                        && event.fee() == 0.0));
    }

    /** After L-003 and L-001 are back: rows, penalty link, activity order. */
    static void afterReturns(LoansController loans, DashboardController dashboard,
                             AppContext context, DataStore store, LocalDate t) {
        ProbeSupport.signIn(context, store, STUDENT1);
        ProbeSupport.check("rows-after-return",
                loans.myLoans(LoanStatus.ACTIVE).isEmpty()
                && loans.myLoans(LoanStatus.OVERDUE).isEmpty()
                && loans.myLoans(LoanStatus.RETURNED).size() == 2);
        LoanRow returned = loans.myLoans(LoanStatus.RETURNED).stream()
                .filter(row -> row.loanId().equals("L-003"))
                .findFirst().orElseThrow();
        ProbeSupport.check("rows-returned-penalty",
                t.equals(returned.returnDate())
                && returned.daysLate() == 4
                && returned.accruedFee() == 200.0
                && returned.penalty() != null
                && returned.penalty().penaltyId().equals("p-001")
                && returned.penalty().status() == PenaltyStatus.UNPAID
                && returned.penalty().amount() == 200.0);
        ProbeSupport.check("my-penalties-after-return",
                context.penalties().myPenalties().size() == 1);
        ProbeSupport.check("dashboard-post-returns", dashboard.loansOut() == 1
                && dashboard.overdueCount() == 0
                && dashboard.unpaidCount() == 1
                && dashboard.unpaidTotal().equals("₱200.00"));
        List<ActivityEntry> activity = dashboard.activity();
        ProbeSupport.check("dashboard-activity-order", activity.size() == 5
                && activity.get(0).loanId().equals("L-003")
                && !activity.get(0).checkout()
                && activity.get(0).fee() == 200.0
                && activity.get(1).loanId().equals("L-001")
                && !activity.get(1).checkout()
                && activity.get(2).checkout()
                && activity.get(2).loanId().equals("L-001")
                && activity.get(3).loanId().equals("L-002")
                && activity.get(4).loanId().equals("L-003"));
    }
}
