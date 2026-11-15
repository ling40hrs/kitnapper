package kitnapper.view;

import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JTable;

import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.EquipmentStatus;
import kitnapper.service.DataStoreImpl;

/**
 * Borrower-side checks for {@link CatalogUiProbe} (plan §9.3): search,
 * filters, selection, checkout, layout toggle, empty state, theme paints and
 * query speed. Reads the rendered catalog through the component tree only.
 */
final class CatalogUiChecks {

    private static final LocalDate T = LocalDate.of(2026, 11, 14);
    private static final String CANON = "Canon EOS R6";
    private static final String EMPTY_TEXT = "No gear matches this filter.";

    private final DataStoreImpl store;
    private final CatalogUiActions actions;
    private final CatalogScreen screen;
    private final MainFrame frame;
    private final JTable table;
    private final PreviewPane preview;

    CatalogUiChecks(DataStoreImpl store, CatalogUiActions actions, CatalogScreen screen,
        MainFrame frame) {
        this.store = store;
        this.actions = actions;
        this.screen = screen;
        this.frame = frame;
        this.table = UiProbeSupport.find(screen, JTable.class);
        this.preview = screen.preview();
    }

    void run() {
        mountAndStatus();
        searchAndFilters();
        selectionAndCheckout();
        layoutToggle();
        emptyState();
        themePaints();
        querySpeed();
    }

    private void mountAndStatus() {
        boolean mounted = table != null && table.getRowCount() == 10;
        UiProbeSupport.check("mount-rows", mounted);
        UiProbeSupport.check("status-bar", actions.statusLeft().contains("10 items")
            && actions.statusLeft().contains("0/1 selected")
            && actions.statusRight().equals("₱872,000.00"));
    }

    private void searchAndFilters() {
        frame.searchField().setText("Sony");
        int sony = table.getRowCount();
        frame.searchField().setText("");
        screen.filters().selectCategory(EquipmentCategory.LENS);
        int lenses = table.getRowCount();
        frame.searchField().setText("Sony");
        int loanedLenses = table.getRowCount();
        UiProbeSupport.check("search-filters",
            sony == 2 && lenses == 2 && loanedLenses == 1);
    }

    private void selectionAndCheckout() {
        screen.clearFilters();
        frame.searchField().setText(CANON);
        table.setRowSelectionInterval(0, 0);
        UiProbeSupport.check("preview-select", UiProbeSupport.hasLabel(preview, CANON)
            && UiProbeSupport.hasLabel(preview, "₱145,000.00")
            && UiProbeSupport.hasAccessibleName(preview, "Status Available"));

        Equipment canon = item("e-002");
        Optional<String> checkedOut = actions.borrowing().checkout(canon, T.plusDays(3));
        JButton action = UiProbeSupport.button(preview, "Check out");
        UiProbeSupport.check("checkout-row", checkedOut.isEmpty()
            && canon.getStatus() == EquipmentStatus.LOANED);
        UiProbeSupport.check("preview-after-checkout",
            UiProbeSupport.hasAccessibleName(preview, "Status Loaned")
            && action != null && action.isVisible() && !action.isEnabled());
    }

    private void layoutToggle() {
        screen.clearFilters();
        table.setRowSelectionInterval(0, 0);
        screen.filters().selectLayout(1);
        boolean cardsShown = UiProbeSupport.hasLabel(preview, CANON);
        screen.filters().selectLayout(0);
        UiProbeSupport.check("cards-toggle",
            screen.cards().cardCount() == 10 && cardsShown
            && table.getSelectedRow() == 0 && UiProbeSupport.hasLabel(preview, CANON));
    }

    private void emptyState() {
        screen.filters().selectStatus(EquipmentStatus.AVAILABLE);
        UiProbeSupport.check("available-after-checkout", table.getRowCount() == 6);
        screen.clearFilters();

        frame.searchField().setText("zzz");
        boolean emptyShowing = UiProbeSupport.labelShowing(screen, EMPTY_TEXT);
        screen.clearFilters();
        UiProbeSupport.check("empty-state", emptyShowing && table.getRowCount() == 10
            && !UiProbeSupport.labelShowing(screen, EMPTY_TEXT));
    }

    private void themePaints() {
        Theme.apply(true);
        screen.refreshTheme();
        frame.refreshTheme();
        UiProbeSupport.settle(frame);
        BufferedImage dark = UiProbeSupport.paint(frame);
        int darkPixels = UiProbeSupport.nonBlank(dark);
        Theme.apply(false);
        screen.refreshTheme();
        frame.refreshTheme();
        UiProbeSupport.settle(frame);
        int lightPixels = UiProbeSupport.nonBlank(UiProbeSupport.paint(frame));
        System.out.println("dark_pixels=" + darkPixels + " light_pixels=" + lightPixels);
        UiProbeSupport.check("paint-themes", darkPixels > 750 && lightPixels > 750);
    }

    private void querySpeed() {
        long start = System.nanoTime();
        for (int run = 0; run < 100; run++) {
            actions.catalog().results();
        }
        double averageMs = (System.nanoTime() - start) / 1_000_000.0 / 100.0;
        System.out.printf("query_ms=%.3f%n", averageMs);
        UiProbeSupport.check("query-under-150ms", averageMs < 150.0);
    }

    private Equipment item(String id) {
        return store.equipment().stream()
            .filter(entry -> entry.getId().equals(id))
            .findFirst()
            .orElseThrow();
    }
}
