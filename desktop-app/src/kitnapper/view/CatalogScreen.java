package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Window;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.EquipmentStatus;

/**
 * The §8.1 catalog page: page header, table or cards on the left, the fixed
 * preview pane on the right, and the counts registered on the shell status
 * bar. Query and selection state live in {@link CatalogPresenter}; every rule
 * (search, filters, checkout, hints) comes from the controller through
 * {@link Actions}. Views never import the controller or the services.
 */
public final class CatalogScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    /** View-declared callbacks; the controller supplies the implementation (§5.7). */
    public interface Actions {
        List<Equipment> search(String query, EquipmentCategory category, EquipmentStatus status);

        boolean isManager();

        boolean canCheckout(Equipment item);

        String checkoutHint(Equipment item);

        List<LocalDate> dueDates();

        Optional<String> checkout(Equipment item, LocalDate dueDate);

        void setStatus(String left, String right);
    }

    private static final String TABLE = "table";
    private static final String CARDS = "cards";
    private static final String EMPTY = "empty";
    private static final int PAD_X = 32;

    private final Actions actions;
    private final JTextField search;
    private final CatalogFilters filters = new CatalogFilters();
    private final CatalogTable table = new CatalogTable();
    private final CatalogCards cards = new CatalogCards();
    private final PreviewPane preview = new PreviewPane(this::requestCheckout);
    private final CatalogPresenter presenter;
    private final JPanel left = new JPanel(new CardLayout());
    private final JScrollPane tableScroll;
    private final EmptyState empty;
    private int layout;

    public CatalogScreen(Actions actions, JTextField searchField) {
        super(new BorderLayout());
        this.actions = Objects.requireNonNull(actions, "actions");
        this.search = Objects.requireNonNull(searchField, "searchField");
        this.tableScroll = table.inScrollPane();
        JButton clear = UiFactory.secondaryButton("Clear filters");
        clear.addActionListener(e -> clearFilters());
        this.empty = new EmptyState(Fonts.glyph("\uE721", "\u2315"),
            "No gear matches this filter.", "Try another search, or clear the filters.", clear);
        this.presenter = new CatalogPresenter(actions, filters, search, table, cards, preview);
        setOpaque(false);
        ScreenCatalog.Meta meta = ScreenCatalog.meta("catalog");
        add(new PageHeader(meta.title(), meta.subtitle()), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        wire();
        reload();
    }

    /** The omnibar command row for this screen (§8.5). */
    public JComponent commands() {
        return filters;
    }

    /** Runs whenever the shell routes here; keeps counts correct after navigation. */
    public void onShown() {
        reload();
    }

    /** Re-runs the query and re-renders both layouts; also the store-change hook. */
    public void reload() {
        presenter.reload();
        showLeft();
        actions.setStatus(presenter.statusLeft(), presenter.aggregate());
    }
    /** Re-applies token colors after a Theme switch (design §8.10). */
    public void refreshTheme() {
        UiFactory.refresh(this);
        filters.refreshTheme();
        cards.refreshTheme();
        preview.refreshTheme();
        tableScroll.getViewport().setBackground(Theme.bgSurface);
        repaint();
    }

    CatalogTable table() {
        return table;
    }

    CatalogCards cards() {
        return cards;
    }

    CatalogFilters filters() {
        return filters;
    }

    PreviewPane preview() {
        return preview;
    }

    /** Search plus both combos back to neutral, layout back to table. */
    void clearFilters() {
        presenter.clearFilters();
        filters.selectLayout(0);
        layout = 0;
        showLeft();
        actions.setStatus(presenter.statusLeft(), presenter.aggregate());
    }

    private void wire() {
        filters.setOnFilterChanged(this::reload);
        filters.setOnLayoutChanged(this::showLayout);
        table.setOnSelect(presenter::select);
        cards.setOnSelect(presenter::select);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                reload();
            }

            public void removeUpdate(DocumentEvent e) {
                reload();
            }

            public void changedUpdate(DocumentEvent e) {
                reload();
            }
        });
    }

    private void showLayout(int index) {
        layout = index;
        showLeft();
    }

    private void showLeft() {
        CardLayout cardsLayout = (CardLayout) left.getLayout();
        if (presenter.isEmpty()) {
            cardsLayout.show(left, EMPTY);
        } else {
            cardsLayout.show(left, layout == 0 ? TABLE : CARDS);
        }
    }

    /**
     * Opens the §8.7 dialog. The chosen date arrives through the confirm
     * callback, which keeps {@link CheckoutDialog}'s pinned boolean return
     * while still naming the date in the toast.
     */
    private void requestCheckout(Equipment item) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        presenter.checkout(item, owner).ifPresent(dueDate -> {
            Toasts.show(owner, "Checked out " + item.getName() + " · due "
                + Formats.date(dueDate));
            reload();
        });
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, PAD_X, 16, PAD_X));
        left.setOpaque(false);
        left.add(tableScroll, TABLE);
        left.add(cards, CARDS);
        left.add(empty, EMPTY);
        body.add(left, BorderLayout.CENTER);
        body.add(preview, BorderLayout.EAST);
        return body;
    }
}
