package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JFrame;

import kitnapper.controller.LoansActions;
import kitnapper.controller.LoansController;
import kitnapper.model.Equipment;
import kitnapper.model.EquipmentStatus;
import kitnapper.model.LoanStatus;
import kitnapper.model.UserRole;
import kitnapper.service.AppContext;

/**
 * Check bodies for {@link LoansUiProbe} (plan §9.3, split at WP3 to stay under
 * the 200-line cap). Each method mounts one real screen against a temp-dir seed
 * and reads it through the component tree, so the assertions cover the wiring
 * the screens own: tabs, reloads, status strings, preview state and the return
 * path from All Loans.
 */
final class LoansUiChecks {

    private static final int PAINT_FLOOR = 500;

    private LoansUiChecks() { }

    /** Borrower session: tabs, counts, preview with the return action hidden. */
    static void myLoans(AppContext ctx) {
        LoansController loans = new LoansController(ctx);
        String[] sink = new String[2];
        MyLoansScreen screen = new MyLoansScreen(new LoansActions(loans),
            (left, right) -> {
                sink[0] = left;
                sink[1] = right;
            });
        JFrame frame = frame(screen);
        UiProbeSupport.settle(frame);

        UiProbeSupport.check("my-active-rows", screen.section().table().getRowCount() == 1);
        UiProbeSupport.check("my-status-left", "2 loans · 1 overdue".equals(sink[0]));
        UiProbeSupport.check("my-status-right", "₱200.00".equals(sink[1]));

        screen.tabs().setSelectedIndex(1);
        UiProbeSupport.check("my-overdue-rows", screen.section().table().getRowCount() == 1);
        screen.section().table().setRowSelectionInterval(0, 0);
        UiProbeSupport.settle(frame);
        UiProbeSupport.check("my-preview-item",
            UiProbeSupport.hasLabel(screen, "Rode Wireless PRO kit"));
        UiProbeSupport.check("my-preview-fee", UiProbeSupport.hasLabel(screen, "₱200.00"));
        UiProbeSupport.check("my-preview-badge",
            UiProbeSupport.hasAccessibleName(screen, "Loan status Overdue"));
        JButton action = UiProbeSupport.button(screen, "Log return");
        UiProbeSupport.check("my-action-hidden", action != null && !action.isVisible());

        screen.tabs().setSelectedIndex(2);
        UiProbeSupport.check("my-returned-empty", screen.section().isEmpty());
        UiProbeSupport.check("my-empty-title",
            UiProbeSupport.hasLabel(screen, "No returned loans yet."));
        frame.dispose();
    }

    /** Manager session: the table, the preview, and the return from All Loans. */
    static void allLoans(AppContext ctx, LocalDate today) {
        LoansController loans = new LoansController(ctx);
        LoansActions actions = new LoansActions(loans);
        String[] sink = new String[2];
        AllLoansScreen screen = new AllLoansScreen(actions, (left, right) -> {
            sink[0] = left;
            sink[1] = right;
        });
        JFrame frame = frame(screen);
        UiProbeSupport.settle(frame);

        LoansTable table = screen.section().table();
        UiProbeSupport.check("all-rows", table.getRowCount() == 3);
        UiProbeSupport.check("all-columns", table.getColumnCount() == 8);
        LoanRow overdue = row(loans.allLoans(), "L-003");
        UiProbeSupport.check("all-status-string", "3 loans · 1 overdue".equals(sink[0]));
        UiProbeSupport.check("all-aggregate", "₱200.00".equals(sink[1]));
        UiProbeSupport.check("all-fee-cell", "₱200.00".equals(Formats.money(
            ((Number) table.getValueAt(viewRowOf(table, "L-003"), 7)).doubleValue())));

        // §9.3.4: a click on the Due header sorts earliest first, so L-003 leads.
        table.getRowSorter().toggleSortOrder(4);
        UiProbeSupport.settle(frame);
        UiProbeSupport.check("all-sort-due",
            table.rowAt(0) != null && "L-003".equals(table.rowAt(0).loanId()));

        int overdueRow = viewRowOf(table, "L-003");
        table.setRowSelectionInterval(overdueRow, overdueRow);
        UiProbeSupport.settle(frame);
        UiProbeSupport.check("all-preview-days", UiProbeSupport.hasLabel(screen, "4"));
        JButton action = UiProbeSupport.button(screen, "Log return");
        UiProbeSupport.check("all-action-live", action != null && action.isVisible()
            && action.isEnabled());
        UiProbeSupport.check("all-fee-today", Math.abs(actions.feeOn(overdue, today) - 200.0)
            < 0.001);
        UiProbeSupport.check("all-fee-yesterday",
            Math.abs(actions.feeOn(overdue, today.minusDays(1)) - 150.0) < 0.001);
        UiProbeSupport.check("all-dark-not-blank",
            UiProbeSupport.nonBlank(UiProbeSupport.paint(frame)) > PAINT_FLOOR);

        Theme.apply(false);
        screen.refreshTheme();
        UiProbeSupport.settle(frame);
        UiProbeSupport.check("all-light-not-blank",
            UiProbeSupport.nonBlank(UiProbeSupport.paint(frame)) > PAINT_FLOOR);
        Theme.apply(true);
        screen.refreshTheme();
        UiProbeSupport.settle(frame);

        Optional<String> failure = actions.returnLoan(overdue, today);
        UiProbeSupport.check("all-return-ok", failure.isEmpty());
        screen.reload();
        UiProbeSupport.settle(frame);
        UiProbeSupport.check("all-returned-row",
            row(loans.allLoans(), "L-003").status() == LoanStatus.RETURNED);
        UiProbeSupport.check("all-return-hint",
            UiProbeSupport.hasLabel(screen, "Already returned on 2026-11-14."));
        UiProbeSupport.check("all-action-disabled", action != null && !action.isEnabled());
        UiProbeSupport.check("all-overdue-empty", loans.overdueLoans().isEmpty());
        UiProbeSupport.check("all-status-after", "3 loans · 0 overdue".equals(sink[0]));
        UiProbeSupport.check("all-aggregate-after", "₱0.00".equals(sink[1]));
        UiProbeSupport.check("all-item-available",
            item(ctx, "e-007").getStatus() == EquipmentStatus.AVAILABLE);
        frame.dispose();
    }

    /** §9.3.10: the shell's route vocabulary per role (needs no store). */
    static void navigation() {
        UiProbeSupport.check("nav-manager-keys", ScreenCatalog.navKeys(UserRole.MANAGER)
            .equals(List.of("dashboard", "catalog", "allloans", "overdue", "penalties", "inventory")));
        UiProbeSupport.check("nav-borrower-keys",
            ScreenCatalog.navKeys(UserRole.BORROWER).equals(List.of("catalog", "myloans")));
        UiProbeSupport.check("nav-defaults",
            ScreenCatalog.defaultKey(UserRole.MANAGER).equals("dashboard")
                && ScreenCatalog.defaultKey(UserRole.BORROWER).equals("catalog"));
    }

    /** The row carrying {@code loanId}, or a failure when it is missing. */
    static LoanRow row(List<LoanRow> rows, String loanId) {
        return rows.stream().filter(candidate -> candidate.loanId().equals(loanId))
            .findFirst().orElseThrow();
    }

    private static Equipment item(AppContext ctx, String equipmentId) {
        return ctx.dataStore().equipment().stream()
            .filter(candidate -> candidate.getId().equals(equipmentId))
            .findFirst().orElseThrow();
    }

    private static int viewRowOf(LoansTable table, String loanId) {
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            LoanRow row = table.rowAt(viewRow);
            if (row != null && row.loanId().equals(loanId)) {
                return viewRow;
            }
        }
        throw new IllegalStateException("loan " + loanId + " is not in the table");
    }

    private static JFrame frame(Component screen) {
        JFrame frame = new JFrame("Loans UI probe");
        frame.setContentPane(new javax.swing.JPanel(new BorderLayout()));
        frame.getContentPane().add(screen, BorderLayout.CENTER);
        frame.setSize(1180, 700);
        frame.setVisible(true);
        return frame;
    }
}
