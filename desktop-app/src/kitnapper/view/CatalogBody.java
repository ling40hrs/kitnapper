package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import kitnapper.model.Equipment;

/**
 * The catalog page's body (design §8.1): the table or cards on the left and
 * the fixed preview pane on the right, plus the no-results empty state.
 * Extracted from {@link CatalogScreen} at M5 WP2 because photo wiring pushed
 * that class at the 200-line cap. It owns layout only; query state stays in
 * {@link CatalogPresenter} and selection is forwarded to the screen's
 * listeners.
 */
final class CatalogBody extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String TABLE = "table";
    private static final String CARDS = "cards";
    private static final String EMPTY = "empty";
    private static final int PAD_X = 32;

    private final JPanel left = new JPanel(new CardLayout());
    private final JScrollPane tableScroll;
    private final CatalogCards cards;
    private final PreviewPane preview;

    CatalogBody(CatalogTable table, EmptyState empty, PhotoSource photos,
                Consumer<Equipment> onCheckout) {
        super(new BorderLayout(16, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, PAD_X, 16, PAD_X));
        this.tableScroll = table.inScrollPane();
        this.cards = new CatalogCards(photos);
        this.preview = new PreviewPane(onCheckout, photos);
        left.setOpaque(false);
        left.add(tableScroll, TABLE);
        left.add(cards, CARDS);
        left.add(empty, EMPTY);
        add(left, BorderLayout.CENTER);
        add(preview, BorderLayout.EAST);
    }

    /** Shows the table, the cards, or the empty state for the current query. */
    void showView(int layout, boolean noResults) {
        CardLayout state = (CardLayout) left.getLayout();
        if (noResults) {
            state.show(left, EMPTY);
        } else {
            state.show(left, layout == 0 ? TABLE : CARDS);
        }
    }

    CatalogCards cards() {
        return cards;
    }

    PreviewPane preview() {
        return preview;
    }

    /** Re-applies token colors after a Theme switch (design §8.10). */
    void refreshTheme() {
        cards.refreshTheme();
        preview.refreshTheme();
        tableScroll.getViewport().setBackground(Theme.bgSurface);
        repaint();
    }
}
