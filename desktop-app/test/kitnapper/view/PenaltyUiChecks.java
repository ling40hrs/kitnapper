package kitnapper.view;

import java.awt.BorderLayout;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;

import kitnapper.controller.PenaltyController;
import kitnapper.model.PenaltyOutcome;
import kitnapper.model.PenaltyStatus;
import kitnapper.service.AppContext;

/**
 * Check body for {@link LoansUiProbe} (plan §9.3 steps 6–7 and the WP4 gate):
 * the §8.6 Penalties screen plus the §8.9 settlement rules. The blank-reason
 * gate is proved on the controller path — the dialog renders that exact
 * message inline and stays open, so the assertion is that the service rejects
 * without changing state; the dialog body is painted separately to prove it
 * renders the row. Split out at WP4 to keep {@link LoansUiChecks} under 200.
 */
final class PenaltyUiChecks {

    private static final int PAINT_FLOOR = 400;

    private PenaltyUiChecks() { }

    /** The §8.6 pane through the real screen: amount, loan, badge, actions. */
    static void penalties(AppContext ctx) {
        PenaltyController penalties = new PenaltyController(ctx);
        String[] sink = new String[2];
        PenaltiesScreen screen = new PenaltiesScreen(penalties, (left, right) -> {
            sink[0] = left;
            sink[1] = right;
        });
        JFrame frame = frame(screen);
        UiProbeSupport.settle(frame);

        PenaltiesTable table = screen.table();
        UiProbeSupport.check("penalties-rows", table.getRowCount() == 1);
        UiProbeSupport.check("penalties-columns", table.getColumnCount() == 7);
        UiProbeSupport.check("penalties-status", "1 penalty · 1 unpaid".equals(sink[0]));
        UiProbeSupport.check("penalties-aggregate", "₱200.00".equals(sink[1]));
        UiProbeSupport.check("penalties-amount-cell", "₱200.00".equals(Formats.money(
            ((Number) table.getValueAt(0, 4)).doubleValue())));

        table.setRowSelectionInterval(0, 0);
        UiProbeSupport.settle(frame);
        UiProbeSupport.check("penalties-preview-amount",
            UiProbeSupport.hasLabel(screen, "₱200.00"));
        UiProbeSupport.check("penalties-preview-loan", UiProbeSupport.hasLabel(screen, "L-003"));
        UiProbeSupport.check("penalties-preview-item",
            UiProbeSupport.hasLabel(screen, "Rode Wireless PRO kit"));
        UiProbeSupport.check("penalties-preview-badge",
            UiProbeSupport.hasAccessibleName(screen, "Penalty status Unpaid"));
        JButton paid = UiProbeSupport.button(screen, "Mark paid");
        JButton waive = UiProbeSupport.button(screen, "Waive\u2026");
        UiProbeSupport.check("penalties-actions-live",
            paid != null && paid.isVisible() && waive != null && waive.isVisible());
        UiProbeSupport.check("penalties-dark-not-blank",
            UiProbeSupport.nonBlank(UiProbeSupport.paint(frame)) > PAINT_FLOOR);

        PenaltyRow row = penalties.penalties().get(0);
        Optional<String> blank = penalties.settle(row, PenaltyOutcome.WAIVED, "   ");
        UiProbeSupport.check("penalties-blank-reason", blank.isPresent());
        UiProbeSupport.check("penalties-blank-message",
            blank.orElse("").toLowerCase().contains("reason"));
        UiProbeSupport.check("penalties-still-unpaid",
            penalties.penalties().get(0).status() == PenaltyStatus.UNPAID);

        Optional<String> waived = penalties.settle(penalties.penalties().get(0),
            PenaltyOutcome.WAIVED, "First offense");
        UiProbeSupport.check("penalties-waive-ok", waived.isEmpty());
        screen.reload();
        UiProbeSupport.settle(frame);
        UiProbeSupport.check("penalties-waived-status", "1 penalty · 0 unpaid".equals(sink[0]));
        UiProbeSupport.check("penalties-waived-aggregate", "₱0.00".equals(sink[1]));
        UiProbeSupport.check("penalties-waived-badge",
            UiProbeSupport.hasAccessibleName(screen, "Penalty status Waived"));
        UiProbeSupport.check("penalties-waived-reason",
            UiProbeSupport.hasLabel(screen, "First offense"));
        UiProbeSupport.check("penalties-settled-no-actions",
            paid != null && waive != null && !paid.isVisible() && !waive.isVisible());

        Theme.apply(false);
        screen.refreshTheme();
        UiProbeSupport.settle(frame);
        UiProbeSupport.check("penalties-light-not-blank",
            UiProbeSupport.nonBlank(UiProbeSupport.paint(frame)) > PAINT_FLOOR);
        Theme.apply(true);
        screen.refreshTheme();
        UiProbeSupport.settle(frame);
        frame.dispose();
    }

    /** The §8.9 body paints the row, the choice and a disabled reason field. */
    static void dialogBody(AppContext ctx) {
        List<PenaltyRow> rows = new PenaltyController(ctx).penalties();
        PenaltyRow row = rows.isEmpty() ? dialogRow() : rows.get(0);
        SegmentedControl choice = new SegmentedControl("Mark paid", "Waive");
        JTextField reason = UiFactory.textField("Why is this fee waived?");
        reason.setEnabled(false);
        javax.swing.JComponent body = SettlePenaltyDialog.body(row, choice, reason);
        JFrame frame = new JFrame("Settle dialog probe");
        frame.setContentPane(new javax.swing.JPanel(new BorderLayout()));
        frame.getContentPane().add(body, BorderLayout.CENTER);
        frame.pack();
        frame.setSize(480, frame.getHeight());
        frame.setVisible(true);
        UiProbeSupport.settle(frame);
        UiProbeSupport.check("dialog-body-amount",
            UiProbeSupport.hasLabel(body, Formats.money(row.amount())));
        UiProbeSupport.check("dialog-body-loan",
            UiProbeSupport.hasLabel(body, row.loanId()));
        UiProbeSupport.check("dialog-body-item",
            UiProbeSupport.hasLabel(body, row.itemName()));
        UiProbeSupport.check("dialog-body-reason-disabled", !reason.isEnabled());
        UiProbeSupport.check("dialog-body-not-blank",
            UiProbeSupport.nonBlank(UiProbeSupport.paint(frame)) > PAINT_FLOOR);
        frame.dispose();
    }

    /** A settled row, for the case where every penalty was already waived. */
    private static PenaltyRow dialogRow() {
        return new PenaltyRow("p-001", "L-003", "Rode Wireless PRO kit", "Student One",
            200.0, PenaltyStatus.WAIVED, java.time.LocalDate.of(2026, 11, 14),
            "First offense");
    }

    private static JFrame frame(javax.swing.JComponent screen) {
        JFrame frame = new JFrame("Penalties UI probe");
        frame.setContentPane(new javax.swing.JPanel(new BorderLayout()));
        frame.getContentPane().add(screen, BorderLayout.CENTER);
        frame.setSize(1180, 700);
        frame.setVisible(true);
        return frame;
    }
}
