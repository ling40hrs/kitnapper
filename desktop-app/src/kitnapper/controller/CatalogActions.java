package kitnapper.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.EquipmentStatus;
import kitnapper.view.CatalogScreen;

/**
 * The catalog screen's §5.7 callbacks, extracted from {@link AppController} at
 * M4 WP3 so the session class only owns routing and theming. Behaviour is
 * unchanged: query state comes from {@link CatalogController}, checkout from
 * {@link BorrowingController}, and counts go to the shell's status sink.
 */
public final class CatalogActions implements CatalogScreen.Actions {

    private final CatalogController catalog;
    private final BorrowingController borrowing;
    private final BiConsumer<String, String> status;

    public CatalogActions(CatalogController catalog, BorrowingController borrowing,
        BiConsumer<String, String> status) {
        this.catalog = catalog;
        this.borrowing = borrowing;
        this.status = status;
    }

    @Override
    public List<Equipment> search(String query, EquipmentCategory category,
        EquipmentStatus filter) {
        catalog.setQuery(query);
        catalog.setCategory(category);
        catalog.setStatus(filter);
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
        status.accept(left, right);
    }
}
