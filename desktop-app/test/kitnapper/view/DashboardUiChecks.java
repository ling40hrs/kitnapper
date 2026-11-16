package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import kitnapper.controller.DashboardController;
import kitnapper.controller.LoansController;
import kitnapper.service.AppContext;

/**
 * Check body for {@link LoansUiProbe} (plan §9.3 step 8): the §8.7 manager
 * Dashboard on a freshly seeded store — KPI counts, the read-only overdue
 * table, the §5.7 status counts and the derived activity order, before and
 * after the return that empties the overdue query. Split out of
 * {@link LoansUiChecks} to keep every probe file under the cap.
 */
final class DashboardUiChecks {

    private static final int PAINT_FLOOR = 500;

    private DashboardUiChecks() { }

    /** Manager session: seed counts, overdue table, activity, return, paint. */
    static void dashboard(AppContext ctx, LocalDate today) {
        DashboardController controller = new DashboardController(ctx);
        String[] sink = new String[2];
        DashboardScreen screen = new DashboardScreen(controller, (left, right) -> {
            sink[0] = left;
            sink[1] = right;
        });
        JFrame frame = frame(screen);
        UiProbeSupport.settle(frame);

        UiProbeSupport.check("dashboard-kpi-out",
            UiProbeSupport.hasAccessibleName(screen, "Out now: 3"));
        UiProbeSupport.check("dashboard-kpi-overdue",
            UiProbeSupport.hasAccessibleName(screen, "Overdue: 1"));
        UiProbeSupport.check("dashboard-kpi-unpaid",
            UiProbeSupport.hasAccessibleName(screen, "Unpaid penalties: 0"));
        UiProbeSupport.check("dashboard-unpaid-caption",
            UiProbeSupport.hasLabel(screen, "₱0.00"));
        UiProbeSupport.check("dashboard-overdue-title",
            UiProbeSupport.hasLabel(screen, "Overdue now"));
        UiProbeSupport.check("dashboard-status", "3 loans · 1 overdue".equals(sink[0]));
        UiProbeSupport.check("dashboard-status-money", "₱0.00".equals(sink[1]));

        LoansTable table = screen.section().table();
        UiProbeSupport.check("dashboard-overdue-rows", table.getRowCount() == 1);
        UiProbeSupport.check("dashboard-overdue-columns", table.getColumnCount() == 6);
        UiProbeSupport.check("dashboard-overdue-first",
            "L-003".equals(table.getValueAt(0, 0)));
        UiProbeSupport.check("dashboard-overdue-days-late",
            Long.valueOf(4L).equals(table.getValueAt(0, 4)));
        UiProbeSupport.check("dashboard-overdue-fee",
            "₱200.00".equals(Formats.money(((Number) table.getValueAt(0, 5)).doubleValue())));
        UiProbeSupport.check("dashboard-overdue-matches", matches(controller, table));

        String sony = "Student One checked out Sony A7 IV";
        String macbook = "Student Two checked out MacBook Pro M3 (14\")";
        String rode = "Student One checked out Rode Wireless PRO kit";
        List<String> seedLabels = labels(screen);
        UiProbeSupport.check("dashboard-activity-count", seedLabels.contains(sony)
            && seedLabels.contains(macbook) && seedLabels.contains(rode));
        UiProbeSupport.check("dashboard-activity-newest",
            indexOf(seedLabels, sony) < indexOf(seedLabels, macbook)
                && indexOf(seedLabels, macbook) < indexOf(seedLabels, rode));
        UiProbeSupport.check("dashboard-activity-dates",
            seedLabels.contains("2026-11-12") && seedLabels.contains("2026-11-08")
                && seedLabels.contains("2026-11-03"));
        UiProbeSupport.check("dashboard-dark-not-blank",
            UiProbeSupport.nonBlank(UiProbeSupport.paint(frame)) > PAINT_FLOOR);

        Optional<String> failure = new LoansController(ctx)
            .returnLoan(controller.overdueLoans().get(0), today);
        UiProbeSupport.check("dashboard-return-ok", failure.isEmpty());
        screen.reload();
        UiProbeSupport.settle(frame);

        UiProbeSupport.check("dashboard-kpi-out-after",
            UiProbeSupport.hasAccessibleName(screen, "Out now: 2"));
        UiProbeSupport.check("dashboard-kpi-overdue-after",
            UiProbeSupport.hasAccessibleName(screen, "Overdue: 0"));
        UiProbeSupport.check("dashboard-kpi-unpaid-after",
            UiProbeSupport.hasAccessibleName(screen, "Unpaid penalties: 1"));
        UiProbeSupport.check("dashboard-unpaid-after",
            UiProbeSupport.hasLabel(screen, "₱200.00"));
        UiProbeSupport.check("dashboard-overdue-empty", screen.section().isEmpty());
        UiProbeSupport.check("dashboard-status-after",
            "2 loans · 0 overdue".equals(sink[0]));
        UiProbeSupport.check("dashboard-status-money-after",
            "₱200.00".equals(sink[1]));

        String returned = "Student One returned Rode Wireless PRO kit · ₱200.00 fee";
        List<String> afterLabels = labels(screen);
        UiProbeSupport.check("dashboard-activity-return", afterLabels.contains(returned));
        UiProbeSupport.check("dashboard-activity-return-newest",
            indexOf(afterLabels, returned) < indexOf(afterLabels, rode)
                && indexOf(afterLabels, returned) == indexOf(afterLabels, "2026-11-14") + 1);

        Theme.apply(false);
        screen.refreshTheme();
        UiProbeSupport.settle(frame);
        UiProbeSupport.check("dashboard-light-not-blank",
            UiProbeSupport.nonBlank(UiProbeSupport.paint(frame)) > PAINT_FLOOR);
        Theme.apply(true);
        screen.refreshTheme();
        UiProbeSupport.settle(frame);
        frame.dispose();
    }

    /** The table shows exactly the controller's overdue rows, in its order. */
    private static boolean matches(DashboardController controller, LoansTable table) {
        List<LoanRow> rows = controller.overdueLoans();
        if (rows.size() != table.getRowCount()) {
            return false;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (!rows.get(i).loanId().equals(table.getValueAt(i, 0))) {
                return false;
            }
        }
        return true;
    }

    /** Every non-blank label under {@code root}, in tree order. */
    private static List<String> labels(Container root) {
        List<String> texts = new ArrayList<>();
        collect(root, texts);
        return texts;
    }

    private static void collect(Container root, List<String> texts) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label && label.getText() != null
                && !label.getText().isBlank()) {
                texts.add(label.getText());
            }
            if (child instanceof Container container) {
                collect(container, texts);
            }
        }
    }

    private static int indexOf(List<String> texts, String text) {
        return texts.indexOf(text);
    }

    private static JFrame frame(javax.swing.JComponent screen) {
        JFrame frame = new JFrame("Dashboard UI probe");
        frame.setContentPane(new JPanel(new BorderLayout()));
        frame.getContentPane().add(screen, BorderLayout.CENTER);
        frame.setSize(1280, 800);
        frame.setVisible(true);
        return frame;
    }
}
