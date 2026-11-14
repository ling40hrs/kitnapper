package kitnapper.controller;

import java.util.List;

import kitnapper.model.Borrower;
import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.EquipmentStatus;
import kitnapper.model.User;
import kitnapper.model.UserRole;
import kitnapper.service.AppContext;

/**
 * Catalog query state and role-dependent checkout hints. The controller owns
 * the current filters and relays app-level changes to the screen, but never
 * touches Swing components or entity mutators.
 */
public final class CatalogController {

    private final AppContext context;
    private String query = "";
    private EquipmentCategory category;
    private EquipmentStatus status;
    private Runnable onChanged;

    public CatalogController(AppContext context) {
        this.context = context;
        context.addChangeListener(this::notifyChanged);
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query;
    }

    public void setCategory(EquipmentCategory category) {
        this.category = category;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }

    public void clearFilters() {
        query = "";
        category = null;
        status = null;
    }

    public List<Equipment> results() {
        return context.catalog().search(query, category, status);
    }

    public boolean isManager() {
        User user = context.currentUser();
        return user != null && user.getRole() == UserRole.MANAGER;
    }

    /** Only a signed-in borrower may check out a currently available item. */
    public boolean canCheckout(Equipment item) {
        User user = context.currentUser();
        return user instanceof Borrower && item != null && item.isBorrowable();
    }

    /** Reason the checkout action is unavailable, or an empty string. */
    public String checkoutHint(Equipment item) {
        if (isManager()) {
            return "Manager view — checkout is borrower-only.";
        }
        if (item == null || item.isBorrowable()) {
            return "";
        }
        return switch (item.getStatus()) {
            case LOANED -> "Currently on loan.";
            case UNDER_MAINTENANCE -> "Under maintenance.";
            case DECOMMISSIONED -> "Decommissioned.";
            default -> "Not available.";
        };
    }

    public void setOnChanged(Runnable listener) {
        this.onChanged = listener;
    }

    private void notifyChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }
}
