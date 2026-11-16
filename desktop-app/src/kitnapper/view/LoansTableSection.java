package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * The §8.1 left region shared by My Loans, All Loans and Overdue: the skinned
 * {@link LoansTable} in its scroll surface, swapping to the §6 empty state
 * whenever a query returns no rows. Rows arrive in service order and the
 * section never re-sorts or filters them; the screen owns query state and the
 * preview stays the caller's concern, so the same body serves all three
 * screens without a per-screen skin.
 */
final class LoansTableSection extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String TABLE = "table";
    private static final String EMPTY = "empty";

    private final LoansTable table;
    private final JScrollPane scroll;
    private final JPanel left = new JPanel(new CardLayout());
    private final JPanel emptySlot = new JPanel(new BorderLayout());

    LoansTableSection(LoansTableModel.Mode mode) {
        super(new BorderLayout());
        setOpaque(false);
        this.table = new LoansTable(mode);
        this.scroll = table.inScrollPane();
        emptySlot.setOpaque(false);
        left.setOpaque(false);
        left.add(scroll, TABLE);
        left.add(emptySlot, EMPTY);
        add(left, BorderLayout.CENTER);
    }

    void setOnSelect(Consumer<LoanRow> listener) {
        table.setOnSelect(listener);
    }

    /** Replaces the rows and picks the table or the empty state (§6). */
    void setRows(List<LoanRow> rows, String emptyTitle, String emptyDetail) {
        table.setItems(rows);
        emptySlot.removeAll();
        if (emptyTitle != null) {
            emptySlot.add(new EmptyState(Fonts.glyph("\uE721", "\u2315"),
                emptyTitle, emptyDetail, null), BorderLayout.CENTER);
        }
        emptySlot.revalidate();
        ((CardLayout) left.getLayout()).show(left, rows.isEmpty() ? EMPTY : TABLE);
    }

    /** Silently highlights a loan; no-op when it is not on screen. */
    void selectRow(String loanId) {
        table.selectRow(loanId);
    }

    boolean isEmpty() {
        return table.getRowCount() == 0;
    }

    LoansTable table() {
        return table;
    }

    void refreshTheme() {
        UiFactory.refresh(this);
        scroll.getViewport().setBackground(Theme.bgSurface);
        repaint();
    }
}
