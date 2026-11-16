package kitnapper.view;

import java.awt.Component;
import java.awt.Window;
import java.util.Optional;

import javax.swing.SwingUtilities;

import kitnapper.model.Equipment;

/**
 * Runs the inventory state actions (M5 WP4): confirms the destructive one,
 * toasts the controller's message, and reloads the screen only after a success.
 * Extracted from {@link InventoryScreen} to keep it clear of the line cap; the
 * enablement rules stay in {@link InventoryStateActions}.
 */
final class InventoryStateRunner {

    private final InventoryScreen.Actions actions;
    private final Component anchor;
    private final Runnable onSaved;

    InventoryStateRunner(InventoryScreen.Actions actions, Component anchor, Runnable onSaved) {
        this.actions = actions;
        this.anchor = anchor;
        this.onSaved = onSaved;
    }

    void flag(Equipment item) {
        apply(actions.flagMaintenance(item), "Flagged " + item.getName() + " for maintenance.");
    }

    void clear(Equipment item) {
        apply(actions.clearMaintenance(item), "Cleared maintenance on " + item.getName() + ".");
    }

    void retire(Equipment item) {
        Window owner = SwingUtilities.getWindowAncestor(anchor);
        if (!Dialogs.confirmDestructive(owner, "Decommission item?",
                "“" + item.getName() + "” will be retired and can never be borrowed again.",
                "Decommission")) {
            return;
        }
        apply(actions.decommission(item), "Decommissioned " + item.getName() + ".");
    }

    /** Toasts the failure, or the success followed by a reload. */
    private void apply(Optional<String> failure, String success) {
        Window owner = SwingUtilities.getWindowAncestor(anchor);
        if (failure.isPresent()) {
            Toasts.show(owner, failure.get());
            return;
        }
        Toasts.show(owner, success);
        onSaved.run();
    }
}
