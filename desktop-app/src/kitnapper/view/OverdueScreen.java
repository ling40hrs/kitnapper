package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Window;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The §8.5 Overdue page: overdue loans longest first, the shared preview with
 * its pinned {@code Log return} action (M4-D30), and the §8.8 return dialog.
 * Returning the last overdue loan swaps the table for the success empty state.
 * The service owns the order and every fee; this screen only counts rows and
 * formats money.
 */
public final class OverdueScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    /** View-declared callbacks (§5.5); the controller supplies them. */
    public interface Actions {
        List<LoanRow> overdueLoans();

        List<LocalDate> returnDates(LoanRow row);

        double feeOn(LoanRow row, LocalDate date);

        Optional<String> returnLoan(LoanRow row, LocalDate date);
    }

    private static final int PAD_X = 32;
    private static final String EMPTY_TITLE = "Nothing is overdue. Good.";

    private final Actions actions;
    private final BiConsumer<String, String> status;
    private final LoansTableSection section = new LoansTableSection(LoansTableModel.Mode.OVERDUE);
    private final LoanPreviewPane preview = new LoanPreviewPane(this::requestReturn);
    private String selectedId;

    public OverdueScreen(Actions actions, BiConsumer<String, String> status) {
        super(new BorderLayout());
        this.actions = Objects.requireNonNull(actions, "actions");
        this.status = Objects.requireNonNull(status, "status");
        setOpaque(false);
        ScreenCatalog.Meta meta = ScreenCatalog.meta("overdue");
        add(new PageHeader(meta.title(), meta.subtitle()), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        section.setOnSelect(this::select);
        reload();
    }

    /** No omnibar commands (§8.5). */
    public JComponent commands() {
        return null;
    }

    /** Runs whenever the shell routes here. */
    public void onShown() {
        reload();
    }

    /** Re-reads the overdue query and re-renders table, preview and counts. */
    public void reload() {
        List<LoanRow> rows = actions.overdueLoans();
        section.setRows(rows, EMPTY_TITLE, null);
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

    /** Opens §8.8 for an overdue loan and toasts the frozen fee. */
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

    /** "N overdue · M borrowers" over the overdue rows (§5.7). */
    private static String statusLeft(List<LoanRow> rows) {
        Set<String> borrowers = new HashSet<>();
        for (LoanRow row : rows) {
            borrowers.add(row.borrowerName());
        }
        return rows.size() + " overdue · " + borrowers.size() + " borrowers";
    }

    /** Σ accrued of the overdue rows, formatted once (§5.7). */
    private static String aggregate(List<LoanRow> rows) {
        double total = 0;
        for (LoanRow row : rows) {
            total += row.accruedFee();
        }
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
