package kitnapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import kitnapper.controller.DashboardController;
import kitnapper.controller.LoansController;
import kitnapper.controller.PenaltyController;
import kitnapper.service.AppContext;
import kitnapper.service.DataStore;
import kitnapper.service.DataStoreImpl;

/**
 * Loan and penalty probe (M4 plan §9.2, WP1): seed reads, the fee overload,
 * controller rows, the return workflow (zero fee, D15 maintenance),
 * settlement and persistence — all against a scratch directory. Runs
 * headless; the clock is pinned at T = 2026-11-14 and advanced once to
 * freeze a second fee (D4, D20).
 */
public final class LoanProbe {

    private LoanProbe() {
    }

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("kitnapper-loan-probe");
        try {
            DataStore store = new DataStoreImpl(dir);
            store.load();
            LocalDate t = ProbeSupport.loan(store, "L-003").getDueDate().plusDays(4);
            AtomicReference<LocalDate> clock = new AtomicReference<>(t);
            AppContext context = new AppContext(store, clock::get);
            LoansController loans = new LoansController(context);
            PenaltyController penalties = new PenaltyController(context);
            DashboardController dashboard = new DashboardController(context);

            LoanChecks.seedReads(context, store, t);
            LoanChecks.controllerRows(loans, context, t);
            DashboardChecks.seed(dashboard);
            for (Map.Entry<String, Boolean> entry
                    : QueryTimingChecks.loans(context, loans, dashboard).entrySet()) {
                ProbeSupport.check(entry.getKey(), entry.getValue());
            }
            LoanChecks.returns(loans, dashboard, context, store, t);
            DashboardChecks.afterReturns(loans, dashboard, context, store, t);
            clock.set(t.plusDays(3));
            PenaltyChecks.secondPenalty(loans, context, store, t);
            PenaltyChecks.settlement(penalties, context, store, t);
            PenaltyChecks.persistence(store, dir);
        } finally {
            ProbeSupport.deleteTree(dir);
        }
        System.out.println(ProbeSupport.tally());
        if (ProbeSupport.failing()) {
            System.exit(1);
        }
    }
}
