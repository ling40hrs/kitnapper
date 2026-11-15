package kitnapper.view;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.swing.JTextField;

import kitnapper.model.Equipment;

/**
 * View-layer query and selection state for the catalog (§8.1): runs the
 * controller's search through {@link CatalogScreen.Actions}, replaces both
 * layouts, keeps one selection shared by table, cards and preview, and
 * computes the status-bar strings. No service or controller imports — the
 * nested actions interface is the whole contract.
 */
final class CatalogPresenter {

    private final CatalogScreen.Actions actions;
    private final CatalogFilters filters;
    private final JTextField search;
    private final CatalogTable table;
    private final CatalogCards cards;
    private final PreviewPane preview;
    private List<Equipment> results = List.of();
    private String selectedId;

    CatalogPresenter(CatalogScreen.Actions actions, CatalogFilters filters,
        JTextField search, CatalogTable table, CatalogCards cards, PreviewPane preview) {
        this.actions = actions;
        this.filters = filters;
        this.search = search;
        this.table = table;
        this.cards = cards;
        this.preview = preview;
    }

    /** Re-runs the query; keeps the selection while the item is still visible. */
    void reload() {
        results = actions.search(search.getText(), filters.categoryFilter(),
            filters.statusFilter());
        table.setItems(results);
        cards.setItems(results);
        Equipment selected = findSelected();
        if (selected == null) {
            selectedId = null;
            preview.clear();
        } else {
            apply(selected);
        }
    }

    /** One shared selection: highlight both layouts, refresh the preview. */
    void select(Equipment item) {
        selectedId = item.getId();
        table.selectItem(selectedId);
        cards.selectItem(selectedId);
        apply(item);
    }

    /** Search and both combos back to neutral (§8.6 Clear filters). */
    void clearFilters() {
        search.setText("");
        filters.selectCategory(null);
        filters.selectStatus(null);
        reload();
    }

    boolean isEmpty() {
        return results.isEmpty();
    }

    int count() {
        return results.size();
    }

    /** "N items · 0/1 selected" for the shell status bar. */
    String statusLeft() {
        String items = results.size() == 1 ? "1 item" : results.size() + " items";
        return items + " · " + (selectedId == null ? "0" : "1") + "/1 selected";
    }

    /** Total replacement value of the visible rows, formatted once. */
    String aggregate() {
        double total = results.stream().mapToDouble(Equipment::getReplacementValue).sum();
        return Formats.money(total);
    }

    /**
     * §8.7: opens the dialog and returns the accepted due date, or empty when
     * the user cancelled or the service rejected the checkout (the dialog
     * already shows that message inline). The caller toasts and reloads.
     */
    Optional<LocalDate> checkout(Equipment item, java.awt.Window owner) {
        LocalDate[] chosen = new LocalDate[1];
        boolean done = CheckoutDialog.show(owner, item, actions.dueDates(), dueDate -> {
            chosen[0] = dueDate;
            return actions.checkout(item, dueDate);
        });
        return done && chosen[0] != null ? Optional.of(chosen[0]) : Optional.empty();
    }

    private void apply(Equipment item) {
        preview.show(item, !actions.isManager(), actions.canCheckout(item),
            actions.checkoutHint(item));
    }

    private Equipment findSelected() {
        if (selectedId == null) {
            return null;
        }
        return results.stream()
            .filter(item -> item.getId().equals(selectedId))
            .findFirst()
            .orElse(null);
    }
}
