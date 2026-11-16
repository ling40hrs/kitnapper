package kitnapper;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import kitnapper.controller.DashboardController;
import kitnapper.controller.LoansController;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.LoanStatus;
import kitnapper.service.AppContext;

/**
 * Query timing audit closing D40 (design §13.1, plan §7 WP4): catalog, loan,
 * overdue and dashboard queries are in-memory list scans applied on the EDT.
 * Measure them at seed scale and assert they are far below the 150 ms
 * skeleton gate, so gating stays measurement-driven instead of speculative.
 * Prints min / median / max per query for the execution log. Headless.
 */
final class QueryTimingChecks {

    private static final int WARMUP = 20;
    private static final int RUNS = 100;
    private static final double GATE_MS = 150.0;

    private QueryTimingChecks() { }

    /** Catalog scans: unfiltered, text, category and status filters. */
    static Map<String, Boolean> catalog(AppContext context) {
        Map<String, Boolean> results = new LinkedHashMap<>();
        results.put("catalog-all-under-gate", measure("catalog-all",
            () -> context.catalog().all()));
        results.put("catalog-text-under-gate", measure("catalog-text",
            () -> context.catalog().search("canon", null, null)));
        results.put("catalog-filter-under-gate", measure("catalog-filter",
            () -> context.catalog().search(null, EquipmentCategory.CAMERA, null)));
        return results;
    }

    /** Loan and dashboard scans, including the controller row mapping. */
    static Map<String, Boolean> loans(AppContext context, LoansController loans,
                                      DashboardController dashboard) {
        Map<String, Boolean> results = new LinkedHashMap<>();
        results.put("loans-mine-under-gate", measure("loans-mine",
            () -> loans.myLoans(LoanStatus.ACTIVE)));
        results.put("loans-all-under-gate", measure("loans-all", loans::allLoans));
        results.put("loans-overdue-under-gate", measure("loans-overdue",
            loans::overdueLoans));
        results.put("dashboard-activity-under-gate", measure("dashboard-activity",
            dashboard::activity));
        results.put("dashboard-overdue-under-gate", measure("dashboard-overdue",
            dashboard::overdueLoans));
        return results;
    }

    /** Runs one query WARMUP+RUNS times and reports whether its max is under the gate. */
    private static boolean measure(String label, Runnable query) {
        for (int i = 0; i < WARMUP; i++) {
            query.run();
        }
        double[] ms = new double[RUNS];
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            query.run();
            ms[i] = (System.nanoTime() - start) / 1_000_000.0;
        }
        Arrays.sort(ms);
        double median = ms[RUNS / 2];
        double max = ms[RUNS - 1];
        System.out.printf("query_ms=%s min=%.3f med=%.3f max=%.3f%n",
            label, ms[0], median, max);
        return max < GATE_MS;
    }
}
