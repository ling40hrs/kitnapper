package kitnapper.view;

import java.awt.Window;

import kitnapper.model.Equipment;

/**
 * Opens the add/edit dialog for the inventory screen (M5 WP4): owns the
 * dialog call, the success toast, and the "reload only when saved" rule, so
 * {@link InventoryScreen} stays layout and wiring.
 */
final class InventoryEditor {

    private final InventoryScreen.Actions actions;

    InventoryEditor(InventoryScreen.Actions actions) {
        this.actions = actions;
    }

    /** Opens "Add item"; true when an item was saved. */
    boolean add(Window owner) {
        return open(owner, null);
    }

    /** Opens "Edit item"; true when the item was saved. */
    boolean edit(Window owner, Equipment item) {
        return open(owner, item);
    }

    private boolean open(Window owner, Equipment item) {
        if (!EquipmentDialog.show(owner, item, actions)) {
            return false;
        }
        Toasts.show(owner, item == null ? "Item added." : "Item updated.");
        return true;
    }
}
