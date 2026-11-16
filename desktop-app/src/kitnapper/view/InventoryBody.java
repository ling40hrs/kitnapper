package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * The inventory page's body (design §5): items table or the no-results empty
 * state on the left, the fixed preview pane on the right. Extracted from
 * {@link InventoryScreen} at M5 WP3 to keep that class clear of the line cap;
 * it owns layout and theme refresh only.
 */
final class InventoryBody extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String TABLE = "table";
    private static final String EMPTY = "empty";
    private static final int PAD_X = 32;

    private final JPanel left = new JPanel(new CardLayout());
    private final JScrollPane tableScroll;
    private final InventoryPreviewPane preview;

    InventoryBody(InventoryTable table, EmptyState empty, InventoryPreviewPane preview) {
        super(new BorderLayout(16, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, PAD_X, 16, PAD_X));
        this.tableScroll = table.inScrollPane();
        this.preview = preview;
        left.setOpaque(false);
        left.add(tableScroll, TABLE);
        left.add(empty, EMPTY);
        add(left, BorderLayout.CENTER);
        add(preview, BorderLayout.EAST);
    }

    /** Shows the table, or the empty state when the filter matches nothing. */
    void showView(boolean noResults) {
        ((CardLayout) left.getLayout()).show(left, noResults ? EMPTY : TABLE);
    }

    InventoryPreviewPane preview() {
        return preview;
    }

    /** Re-applies token colors after a Theme switch (design §8.10). */
    void refreshTheme() {
        preview.refreshTheme();
        tableScroll.getViewport().setBackground(Theme.bgSurface);
        repaint();
    }
}
