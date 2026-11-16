package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Window;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import kitnapper.model.LoanStatus;

/**
 * The §8.4 All Loans page: every loan in service order, the shared preview with
 * its pinned {@code Log return} action, and the §8.8 return dialog. Returning
 * is a manager workflow (INV-3) and is reachable from here as well as Overdue
 * (M4-D30); the screen only opens the dialog — dates, fees and the service call
 * all come through {@link Actions}.
 */
public final class AllLoansScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    /** View-declared callbacks (§5.5); the controller supplies them. */
    public interface Actions {
        List<LoanRow> allLoans();

        List<LocalDate> returnDates(LoanRow row);

        double feeOn(LoanRow row, LocalDate date);

        Optional<String> returnLoan(LoanRow row, LocalDate date);
    }

    private static final int PAD_X = 32;
    private static final String EMPTY_TITLE = "No loans yet.";
    private static final String EMPTY_DETAIL = "Checkouts appear here as borrowers borrow gear.";

    private final Actions actions;
    private final BiConsumer<String, String> status;
    private final LoansTableSection section = new LoansTableSection(LoansTableModel.Mode.ALL);
    private final LoanPreviewPane preview = new LoanPreviewPane(this::requestReturn);
    private String selectedId;

    public AllLoansScreen(Actions actions, BiConsumer<String, String> status) {
        super(new BorderLayout());
        this.actions = Objects.requireNonNull(actions, "actions");
        this.status = Objects.requireNonNull(status, "status");
        setOpaque(false);
        ScreenCatalog.Meta meta = ScreenCatalog.meta("allloans");
        add(new PageHeader(meta.title(), meta.subtitle()), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        section.setOnSelect(this::select);
        reload();
    }

    /** No omnibar commands beyond the inert search field (§8.4). */
    public JComponent commands() {
        return null;
    }

    /** Runs whenever the shell routes here. */
    public void onShown() {
        reload();
    }

    /** Re-reads every loan and re-renders table, preview and counts. */
    public void reload() {
        List<LoanRow> rows = actions.allLoans();
        section.setRows(rows, EMPTY_TITLE, EMPTY_DETAIL);
        reapplySelection(rows);
        status.accept(statusLeft(rows), aggregate(rows));
    }

    /** Re-applies token colors after a Theme switch (design §8.10). */
    public void refreshTheme() {
        UiFactory.refresh(this);
        section.refreshTheme();
        preview.refreshTheme();
    }

    LoansTableSection section() {
        return section;
    }

    LoanPreviewPane preview() {
        return preview;
    }

    private void select(LoanRow row) {
        selectedId = row.loanId();
        preview.show(row, true, row.returnDate() == null, hint(row));
    }

    /** Opens §8.8 for an outstanding loan and toasts the frozen fee. */
    private void requestReturn(LoanRow row) {
        if (row.returnDate() != null) {
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        Optional<Double> frozen = ReturnDialog.show(owner, row, actions.returnDates(row),
            date -> actions.feeOn(row, date),
            (target, date) -> actions.returnLoan(target, date));
        if (frozen.isEmpty()) {
            return;
        }
        double fee = frozen.get();
        String message = "Returned " + row.itemName()
            + (fee > 0 ? " · fee " + Formats.money(fee) : " · no fee");
        if (owner != null) {
            Toasts.show(owner, message);
        }
        reload();
    }

    private void reapplySelection(List<LoanRow> rows) {
        LoanRow row = rows.stream()
            .filter(candidate -> candidate.loanId().equals(selectedId))
            .findFirst()
            .orElse(null);
        if (row == null) {
            selectedId = null;
            preview.clear();
        } else {
            section.selectRow(selectedId);
            select(row);
        }
    }

    /** Why the action is disabled, or an empty string while it is live (§8.2). */
    private static String hint(LoanRow row) {
        return row.returnDate() == null ? ""
            : "Already returned on " + Formats.date(row.returnDate()) + ".";
    }

    /** "N loans · M overdue" over every loan (§5.7). */
    private static String statusLeft(List<LoanRow> rows) {
        long overdue = rows.stream()
            .filter(row -> row.status() == LoanStatus.OVERDUE)
            .count();
        return Formats.count(rows.size(), "loan", "loans") + " · " + overdue + " overdue";
    }

    /** Σ live accrued of the outstanding loans, formatted once. */
    private static String aggregate(List<LoanRow> rows) {
        double total = rows.stream()
            .filter(row -> row.returnDate() == null)
            .mapToDouble(LoanRow::accruedFee)
            .sum();
        return Formats.money(total);
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, PAD_X, 16, PAD_X));
        body.add(section, BorderLayout.CENTER);
        body.add(preview, BorderLayout.EAST);
        return body;
    }
}
