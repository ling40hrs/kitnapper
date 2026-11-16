package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import kitnapper.controller.LoansController;
import kitnapper.controller.PenaltyController;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.LoanStatus;
import kitnapper.model.PenaltyOutcome;
import kitnapper.model.PenaltyStatus;
import kitnapper.service.AppContext;
import kitnapper.service.DataStoreImpl;
import kitnapper.service.persistence.JsonWriter;
import kitnapper.service.persistence.RootMapper;
import kitnapper.service.persistence.SeedData;

/**
 * WP2 paint smoke for the shared loan UI (plan §7 WP2 gate): mounts the real
 * {@link LoansTable}, {@link LoanPreviewPane} and {@link PenaltyPreviewPane}
 * against a temp-dir seed at {@code T = 2026-11-14}, then checks the row
 * model, the preview facts, the return-then-freeze path and dark/light paints.
 * The full §9.3 flow lands in WP6; this probe stays a scaffold until then.
 * Needs a display (xvfb-run).
 */
public final class LoansUiProbe {

    private static final LocalDate T = LocalDate.of(2026, 11, 14);
    private static final int PAINT_FLOOR = 500;

    private LoansUiProbe() { }

    public static void main(String[] args) throws Exception {
        Theme.apply(true);
        Path dir = Files.createTempDirectory("kitnapper-loans-ui-probe");
        try {
            seed(dir);
            DataStoreImpl store = new DataStoreImpl(dir);
            store.load();
            AppContext ctx = new AppContext(store, () -> T);
            ctx.setCurrentUser(ctx.auth().login("admin@institution.edu", "admin123")
                .orElseThrow());
            // Every Swing read and write happens on the EDT (CatalogUiProbe rule).
            SwingUtilities.invokeAndWait(() -> run(ctx));
        } finally {
            deleteTree(dir);
        }
        UiProbeSupport.finish();
    }

    private static void run(AppContext ctx) {
        LoansController loans = new LoansController(ctx);
        List<LoanRow> all = loans.allLoans();
        LoanRow overdue = row(all, "L-003");
        UiProbeSupport.check("all-rows", all.size() == 3);
        UiProbeSupport.check("overdue-status", overdue.status() == LoanStatus.OVERDUE);
        UiProbeSupport.check("overdue-days", overdue.daysLate() == 4);
        UiProbeSupport.check("overdue-fee", overdue.accruedFee() == 200.0);
        UiProbeSupport.check("overdue-category", overdue.category() == EquipmentCategory.AUDIO);

        LoansTable table = new LoansTable(LoansTableModel.Mode.ALL);
        LoanPreviewPane preview = new LoanPreviewPane(loan -> { });
        table.setItems(all);
        table.setOnSelect(loan -> preview.show(loan, true, loan.returnDate() == null, ""));
        JFrame frame = frame(table.inScrollPane(), preview, 900);
        UiProbeSupport.settle(frame);

        UiProbeSupport.check("table-columns", table.getColumnCount() == 8);
        UiProbeSupport.check("table-rows", table.getRowCount() == 3);
        UiProbeSupport.check("status-cell", table.getValueAt(2, 6) == LoanStatus.OVERDUE);
        UiProbeSupport.check("fee-cell", "₱200.00".equals(Formats.money(
            ((Number) table.getValueAt(2, 7)).doubleValue())));

        table.setRowSelectionInterval(2, 2);
        UiProbeSupport.check("preview-item",
            UiProbeSupport.hasLabel(preview, "Rode Wireless PRO kit"));
        UiProbeSupport.check("preview-borrower", UiProbeSupport.hasLabel(preview, "Student One"));
        UiProbeSupport.check("preview-days-late", UiProbeSupport.hasLabel(preview, "4"));
        UiProbeSupport.check("preview-fee", UiProbeSupport.hasLabel(preview, "₱200.00"));
        UiProbeSupport.check("preview-badge",
            UiProbeSupport.hasAccessibleName(preview, "Loan status Overdue"));
        JButton returnButton = UiProbeSupport.button(preview, "Log return");
        UiProbeSupport.check("preview-action", returnButton != null && returnButton.isVisible());

        UiProbeSupport.check("dark-not-blank",
            UiProbeSupport.nonBlank(UiProbeSupport.paint(frame)) > PAINT_FLOOR);
        Theme.apply(false);
        UiProbeSupport.settle(frame);
        UiProbeSupport.check("light-not-blank",
            UiProbeSupport.nonBlank(UiProbeSupport.paint(frame)) > PAINT_FLOOR);
        Theme.apply(true);
        UiProbeSupport.settle(frame);

        Optional<String> failure = loans.returnLoan(overdue, T);
        UiProbeSupport.check("return-ok", failure.isEmpty());
        LoanRow returned = row(loans.allLoans(), "L-003");
        UiProbeSupport.check("returned-status", returned.status() == LoanStatus.RETURNED);
        preview.show(returned, true, false, "Already returned on 2026-11-14.");
        UiProbeSupport.settle(frame);
        UiProbeSupport.check("frozen-fee-line",
            UiProbeSupport.hasLabel(preview, "₱200.00 · Unpaid"));
        UiProbeSupport.check("return-hint",
            UiProbeSupport.hasLabel(preview, "Already returned on 2026-11-14."));

        PenaltyController penalties = new PenaltyController(ctx);
        List<PenaltyRow> fees = penalties.penalties();
        UiProbeSupport.check("one-penalty", fees.size() == 1);
        PenaltyRow penalty = fees.get(0);
        UiProbeSupport.check("penalty-row", penalty.amount() == 200.0
            && penalty.status() == PenaltyStatus.UNPAID);

        PenaltyPreviewPane pane = new PenaltyPreviewPane(fee -> { }, fee -> { });
        JFrame penaltyFrame = frame(new JPanel(), pane, 420);
        pane.show(penalty, true);
        UiProbeSupport.settle(penaltyFrame);
        UiProbeSupport.check("penalty-amount", UiProbeSupport.hasLabel(pane, "₱200.00"));
        UiProbeSupport.check("penalty-loan", UiProbeSupport.hasLabel(pane, "L-003"));
        UiProbeSupport.check("penalty-item",
            UiProbeSupport.hasLabel(pane, "Rode Wireless PRO kit"));
        UiProbeSupport.check("penalty-badge",
            UiProbeSupport.hasAccessibleName(pane, "Penalty status Unpaid"));
        JButton paidButton = UiProbeSupport.button(pane, "Mark paid");
        JButton waiveButton = UiProbeSupport.button(pane, "Waive\u2026");
        UiProbeSupport.check("penalty-actions", paidButton != null && paidButton.isVisible()
            && waiveButton != null && waiveButton.isVisible());
        UiProbeSupport.check("penalty-dark-not-blank",
            UiProbeSupport.nonBlank(UiProbeSupport.paint(penaltyFrame)) > PAINT_FLOOR / 2);

        UiProbeSupport.check("waive-ok",
            penalties.settle(penalty, PenaltyOutcome.WAIVED, "First offense").isEmpty());
        pane.show(penalties.penalties().get(0), true);
        UiProbeSupport.settle(penaltyFrame);
        UiProbeSupport.check("waived-badge",
            UiProbeSupport.hasAccessibleName(pane, "Penalty status Waived"));
        UiProbeSupport.check("waive-reason", UiProbeSupport.hasLabel(pane, "First offense"));
        UiProbeSupport.check("settled-no-actions",
            !paidButton.isVisible() && !waiveButton.isVisible());

        penaltyFrame.dispose();
        frame.dispose();
    }

    private static LoanRow row(List<LoanRow> rows, String loanId) {
        return rows.stream().filter(candidate -> candidate.loanId().equals(loanId))
            .findFirst().orElseThrow();
    }

    /** A bare window with the part under test centred and an optional east pane. */
    private static JFrame frame(Component center, Component east, int width) {
        JFrame frame = new JFrame("Loans UI probe");
        JPanel body = new JPanel(new BorderLayout());
        body.add(center, BorderLayout.CENTER);
        body.add(east, BorderLayout.EAST);
        frame.setContentPane(body);
        frame.setSize(width, 620);
        frame.setVisible(true);
        return frame;
    }

    private static void seed(Path dir) throws java.io.IOException {
        Files.writeString(dir.resolve(DataStoreImpl.SAVE_FILE),
            JsonWriter.write(new RootMapper().toDocument(SeedData.create(T))),
            StandardCharsets.UTF_8);
    }

    private static void deleteTree(Path dir) {
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (java.io.IOException ignored) {
                    // Best effort cleanup of a scratch directory.
                }
            });
        } catch (java.io.IOException ignored) {
            // Best effort cleanup of a scratch directory.
        }
    }
}
