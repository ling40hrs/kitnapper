package kitnapper.view;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import kitnapper.controller.BorrowingController;
import kitnapper.controller.CatalogController;
import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.EquipmentStatus;

/**
 * Test double for {@link CatalogScreen.Actions} (plan §9.3). Wires the screen
 * straight to the real controllers and records the status-bar strings the
 * probe asserts on, since the production screen keeps them private.
 */
final class CatalogUiActions implements CatalogScreen.Actions {

    private final CatalogController catalog;
    private final BorrowingController borrowing;
    private String statusLeft = "";
    private String statusRight = "";

    CatalogUiActions(CatalogController catalog, BorrowingController borrowing) {
        this.catalog = catalog;
        this.borrowing = borrowing;
    }

    CatalogController catalog() {
        return catalog;
    }

    BorrowingController borrowing() {
        return borrowing;
    }

    String statusLeft() {
        return statusLeft;
    }

    String statusRight() {
        return statusRight;
    }

    @Override
    public List<Equipment> search(String query, EquipmentCategory category,
        EquipmentStatus status) {
        catalog.setQuery(query);
        catalog.setCategory(category);
        catalog.setStatus(status);
        return catalog.results();
    }

    @Override
    public boolean isManager() {
        return catalog.isManager();
    }

    @Override
    public boolean canCheckout(Equipment item) {
        return catalog.canCheckout(item);
    }

    @Override
    public String checkoutHint(Equipment item) {
        return catalog.checkoutHint(item);
    }

    @Override
    public List<LocalDate> dueDates() {
        return borrowing.dueDateChoices();
    }

    @Override
    public Optional<String> checkout(Equipment item, LocalDate dueDate) {
        return borrowing.checkout(item, dueDate);
    }

    @Override
    public void setStatus(String left, String right) {
        statusLeft = left;
        statusRight = right;
    }
}
