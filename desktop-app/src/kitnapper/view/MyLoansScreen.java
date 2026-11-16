package kitnapper.view;

import java.awt.BorderLayout;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import kitnapper.model.LoanStatus;

/**
 * The §8.3 My Loans page: the Active / Overdue / Returned segmented control in
 * the omnibar, the MY loan table on the left and the loan preview on the right
 * with its return action hidden (M4-D5). Reload queries every tab so the status
 * bar can report the borrower's whole scope (§5.7) without a second contract;
 * counts are plain row counts, never fee rules.
 */
public final class MyLoansScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    /** View-declared callbacks (§5.5); the controller supplies them. */
    public interface Actions {
        List<LoanRow> myLoans(LoanStatus tab);
    }

    private static final int PAD_X = 32;

    private static final String[] EMPTY_TITLES = {
        "No active loans.", "Nothing overdue. Good.", "No returned loans yet.",
    };
    private static final String[] EMPTY_DETAILS = {
        "Browse the catalog to check out gear.", null,
        "Returns appear here once a manager logs them.",
    };

    private final Actions actions;
    private final BiConsumer<String, String> status;
    private final SegmentedControl tabs =
        new SegmentedControl("Active", "Overdue", "Returned");
    private final LoansTableSection section = new LoansTableSection(LoansTableModel.Mode.MY);
    private final LoanPreviewPane preview = new LoanPreviewPane(row -> { });
    private String selectedId;

    public MyLoansScreen(Actions actions, BiConsumer<String, String> status) {
        super(new BorderLayout());
        this.actions = Objects.requireNonNull(actions, "actions");
        this.status = Objects.requireNonNull(status, "status");
        setOpaque(false);
        tabs.getAccessibleContext().setAccessibleName("Loan status filter");
        ScreenCatalog.Meta meta = ScreenCatalog.meta("myloans");
        add(new PageHeader(meta.title(), meta.subtitle()), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        tabs.addChangeListener(index -> reload());
        section.setOnSelect(this::select);
        reload();
    }

    /** The Active / Overdue / Returned tabs, mounted in the omnibar (§8.3). */
    public JComponent commands() {
        return tabs;
    }

    /** Runs whenever the shell routes here; keeps counts correct on return. */
    public void onShown() {
        reload();
    }

    /** Re-queries all three tabs and re-renders the current one. */
    public void reload() {
        List<LoanRow> active = actions.myLoans(LoanStatus.ACTIVE);
        List<LoanRow> overdue = actions.myLoans(LoanStatus.OVERDUE);
        List<LoanRow> returned = actions.myLoans(LoanStatus.RETURNED);
        int tab = tabs.getSelectedIndex();
        List<LoanRow> rows = switch (tab) {
            case 1 -> overdue;
            case 2 -> returned;
            default -> active;
        };
        section.setRows(rows, EMPTY_TITLES[tab], EMPTY_DETAILS[tab]);
        reapplySelection(rows);
        status.accept(statusLeft(active, overdue, returned), aggregate(active, overdue));
    }

    /** Re-applies token colors after a Theme switch (design §8.10). */
    public void refreshTheme() {
        UiFactory.refresh(this);
        tabs.repaint();
        section.refreshTheme();
        preview.refreshTheme();
    }

    /** The tab control, for probes that drive the screen like a user would. */
    SegmentedControl tabs() {
        return tabs;
    }

    LoansTableSection section() {
        return section;
    }

    LoanPreviewPane preview() {
        return preview;
    }

    private void select(LoanRow row) {
        selectedId = row.loanId();
        preview.show(row, false, false, null);
    }

    /** Keeps the preview on the same loan across a reload, or clears it. */
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

    /** "N loans · M overdue" over the borrower's whole scope (§5.7). */
    private static String statusLeft(List<LoanRow> active, List<LoanRow> overdue,
        List<LoanRow> returned) {
        int total = active.size() + overdue.size() + returned.size();
        return total + " loans · " + overdue.size() + " overdue";
    }

    /** Σ live accrued of the outstanding loans, formatted once. */
    private static String aggregate(List<LoanRow> active, List<LoanRow> overdue) {
        double total = 0;
        for (LoanRow row : active) {
            total += row.accruedFee();
        }
        for (LoanRow row : overdue) {
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
