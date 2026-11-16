package kitnapper.controller;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

import kitnapper.model.Equipment;
import kitnapper.model.LoanRecord;
import kitnapper.model.LoanStatus;
import kitnapper.model.User;
import kitnapper.model.UserRole;
import kitnapper.service.AppContext;

/**
 * Inventory query state plus the mutation boundary (M5 WP3). Reads come from
 * {@code CatalogService} so the manager sees the same ordering as the catalog;
 * writes go to {@link kitnapper.service.InventoryService}, whose exceptions are
 * translated here so no view catches anything (D16, the
 * {@link BorrowingController} pattern).
 */
public final class InventoryController {

    private static final String SAVE_FAILED =
            "Could not save the change. Check the app folder and retry.";

    private final AppContext context;
    private String query = "";
    private Runnable onChanged;

    public InventoryController(AppContext context) {
        this.context = context;
        context.addChangeListener(this::notifyChanged);
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query;
    }

    /** Every item matching the omnibar text, name then id (text only, D-M5). */
    public List<Equipment> results() {
        return context.catalog().search(query, null, null);
    }

    public boolean isManager() {
        User user = context.currentUser();
        return user != null && user.getRole() == UserRole.MANAGER;
    }

    /** True while an unreturned loan references the item. */
    public boolean onLoan(Equipment item) {
        if (item == null) {
            return false;
        }
        for (LoanRecord loan : context.dataStore().loans()) {
            if (loan.getEquipmentId().equals(item.getId())
                    && loan.getLoanStatus() != LoanStatus.RETURNED) {
                return true;
            }
        }
        return false;
    }

    public Optional<String> flagMaintenance(Equipment item) {
        return run(() -> context.inventory().flagMaintenance(item.getId()),
            "Could not flag the item for maintenance.");
    }

    public Optional<String> clearMaintenance(Equipment item) {
        return run(() -> context.inventory().clearMaintenance(item.getId()),
            "Could not clear the maintenance flag.");
    }

    public Optional<String> decommission(Equipment item) {
        return run(() -> context.inventory().decommission(item.getId()),
            "Could not decommission the item.");
    }

    public void setOnChanged(Runnable listener) {
        this.onChanged = listener;
    }

    private Optional<String> run(Runnable action, String fallback) {
        try {
            action.run();
            return Optional.empty();
        } catch (UncheckedIOException saveFailure) {
            return Optional.of(SAVE_FAILED);
        } catch (RuntimeException failure) {
            String message = failure.getMessage();
            return Optional.of(message == null || message.isBlank() ? fallback : message);
        }
    }

    private void notifyChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }
}
