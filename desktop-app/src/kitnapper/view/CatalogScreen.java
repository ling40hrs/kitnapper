package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Window;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.EquipmentStatus;

/**
 * The §8.1 catalog page: page header, the {@link CatalogBody} (table or cards
 * plus the preview pane), and the counts registered on the shell status bar.
 * Query and selection state live in {@link CatalogPresenter}; every rule
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

    private final Actions actions;
    private final JTextField search;
    private final CatalogFilters filters = new CatalogFilters();
    private final CatalogTable table = new CatalogTable();
    private final CatalogBody body;
    private final CatalogPresenter presenter;
    private int layout;

    public CatalogScreen(Actions actions, JTextField searchField) {
        this(actions, searchField, PhotoSource.NONE);
    }

    /**
     * @param photos item photos for the cards layout and the preview pane;
     *               {@link PhotoSource#NONE} keeps every media block on the
     *               category fallback tile
     */
    public CatalogScreen(Actions actions, JTextField searchField, PhotoSource photos) {
        super(new BorderLayout());
        this.actions = Objects.requireNonNull(actions, "actions");
        this.search = Objects.requireNonNull(searchField, "searchField");
        JButton clear = UiFactory.secondaryButton("Clear filters");
        clear.addActionListener(e -> clearFilters());
        EmptyState empty = new EmptyState(Fonts.glyph("\uE721", "\u2315"),
            "No gear matches this filter.", "Try another search, or clear the filters.", clear);
        this.body = new CatalogBody(table, empty, photos, this::requestCheckout);
        this.presenter = new CatalogPresenter(actions, filters, search, table,
            body.cards(), body.preview());
        setOpaque(false);
        ScreenCatalog.Meta meta = ScreenCatalog.meta("catalog");
        add(new PageHeader(meta.title(), meta.subtitle()), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
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
        body.showView(layout, presenter.isEmpty());
        actions.setStatus(presenter.statusLeft(), presenter.aggregate());
    }

    /** Re-applies token colors after a Theme switch (design §8.10). */
    public void refreshTheme() {
        UiFactory.refresh(this);
        filters.refreshTheme();
        body.refreshTheme();
        repaint();
    }

    CatalogTable table() {
        return table;
    }

    CatalogCards cards() {
        return body.cards();
    }

    CatalogFilters filters() {
        return filters;
    }

    PreviewPane preview() {
        return body.preview();
    }

    /** Search plus both combos back to neutral, layout back to table. */
    void clearFilters() {
        presenter.clearFilters();
        filters.selectLayout(0);
        layout = 0;
        body.showView(layout, presenter.isEmpty());
        actions.setStatus(presenter.statusLeft(), presenter.aggregate());
    }

    private void wire() {
        filters.setOnFilterChanged(this::reload);
        filters.setOnLayoutChanged(this::showLayout);
        table.setOnSelect(presenter::select);
        body.cards().setOnSelect(presenter::select);
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
        body.showView(layout, presenter.isEmpty());
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
}
