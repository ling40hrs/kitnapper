package kitnapper.view;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JFrame;

import kitnapper.controller.LoansActions;
import kitnapper.controller.LoansController;
import kitnapper.model.EquipmentStatus;
import kitnapper.model.LoanStatus;
import kitnapper.service.AppContext;

/**
 * Check body for {@link LoansUiProbe} (plan §9.3 step 5–6): the §8.5 Overdue
 * screen end to end — the longest-overdue row, its days-late column, the
 * preview and the return that empties the query and swaps in the success empty
 * state. Split out at WP4 to keep {@link LoansUiChecks} under the 200-line cap.
 */
final class OverdueUiChecks {

    private static final int PAINT_FLOOR = 500;

    private OverdueUiChecks() { }

    /** Manager session: rows, counts, preview, paint, and the return flow. */
    static void overdue(AppContext ctx, LocalDate today) {
        LoansController loans = new LoansController(ctx);
        LoansActions actions = new LoansActions(loans);
        String[] sink = new String[2];
        OverdueScreen screen = new OverdueScreen(actions, (left, right) -> {
            sink[0] = left;
            sink[1] = right;
        });
        JFrame frame = frame(screen);
        UiProbeSupport.settle(frame);

        LoansTable table = screen.section().table();
        UiProbeSupport.check("overdue-rows", table.getRowCount() == 1);
        UiProbeSupport.check("overdue-columns", table.getColumnCount() == 6);
        UiProbeSupport.check("overdue-status", "1 overdue · 1 borrower".equals(sink[0]));
        UiProbeSupport.check("overdue-aggregate", "₱200.00".equals(sink[1]));
        UiProbeSupport.check("overdue-first-row", "L-003".equals(table.getValueAt(0, 0)));
        UiProbeSupport.check("overdue-days-late", Long.valueOf(4L).equals(table.getValueAt(0, 4)));

        table.setRowSelectionInterval(0, 0);
        UiProbeSupport.settle(frame);
        UiProbeSupport.check("overdue-preview-item",
            UiProbeSupport.hasLabel(screen, "Rode Wireless PRO kit"));
        UiProbeSupport.check("overdue-preview-badge",
            UiProbeSupport.hasAccessibleName(screen, "Loan status Overdue"));
        JButton action = UiProbeSupport.button(screen, "Log return");
        UiProbeSupport.check("overdue-action-live",
            action != null && action.isVisible() && action.isEnabled());
        UiProbeSupport.check("overdue-dark-not-blank",
            UiProbeSupport.nonBlank(UiProbeSupport.paint(frame)) > PAINT_FLOOR);

        Theme.apply(false);
        screen.refreshTheme();
        UiProbeSupport.settle(frame);
        UiProbeSupport.check("overdue-light-not-blank",
            UiProbeSupport.nonBlank(UiProbeSupport.paint(frame)) > PAINT_FLOOR);
        Theme.apply(true);
        screen.refreshTheme();
        UiProbeSupport.settle(frame);

        List<LoanRow> before = loans.overdueLoans();
        LoanRow row = before.get(0);
        Optional<String> failure = actions.returnLoan(row, today);
        UiProbeSupport.check("overdue-return-ok", failure.isEmpty());
        screen.reload();
        UiProbeSupport.settle(frame);
        UiProbeSupport.check("overdue-empty", screen.section().isEmpty());
        UiProbeSupport.check("overdue-empty-title",
            UiProbeSupport.hasLabel(screen, "Nothing is overdue. Good."));
        UiProbeSupport.check("overdue-status-after",
            "0 overdue · 0 borrowers".equals(sink[0]));
        UiProbeSupport.check("overdue-aggregate-after", "₱0.00".equals(sink[1]));
        UiProbeSupport.check("overdue-item-available",
            ctx.dataStore().equipment().stream()
                .filter(item -> item.getId().equals("e-007"))
                .findFirst().orElseThrow().getStatus() == EquipmentStatus.AVAILABLE);
        UiProbeSupport.check("overdue-row-returned",
            LoansUiChecks.row(loans.allLoans(), row.loanId()).status() == LoanStatus.RETURNED);
        frame.dispose();
    }

    private static JFrame frame(javax.swing.JComponent screen) {
        JFrame frame = new JFrame("Overdue UI probe");
        frame.setContentPane(new javax.swing.JPanel(new java.awt.BorderLayout()));
        frame.getContentPane().add(screen, java.awt.BorderLayout.CENTER);
        frame.setSize(1180, 700);
        frame.setVisible(true);
        return frame;
    }
}
