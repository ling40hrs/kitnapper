package kitnapper.view;

import javax.swing.JButton;

import kitnapper.controller.InventoryController;
import kitnapper.model.Equipment;
import kitnapper.service.AppContext;
import kitnapper.service.DataStoreImpl;

/**
 * Inventory screen checks (M5 WP3, plan §9.4): rows, the omnibar filter, the
 * empty state, preview metadata and action enablement per status, the state
 * transitions through the controller, the loan-aware block, and the status
 * counts. Runs on the EDT inside {@link InventoryUiProbe}.
 */
final class InventoryUiChecks {

    private final AppContext context;
    private final DataStoreImpl store;
    private final InventoryController inventory;
    private final InventoryScreen screen;
    private final MainFrame frame;
    private final InventoryTable table;
    private final InventoryPreviewPane preview;

    InventoryUiChecks(AppContext context, DataStoreImpl store, InventoryController inventory,
                      InventoryScreen screen, MainFrame frame) {
        this.context = context;
        this.store = store;
        this.inventory = inventory;
        this.screen = screen;
        this.frame = frame;
        this.table = UiProbeSupport.find(screen, InventoryTable.class);
        this.preview = UiProbeSupport.find(screen, InventoryPreviewPane.class);
    }

    void run() {
        UiProbeSupport.check("inventory lists every seeded item", table.getRowCount() == 10);
        UiProbeSupport.check("the status bar counts all items",
            UiProbeSupport.hasLabel(frame, "10 items · 0/1 selected"));
        UiProbeSupport.check("the status bar totals the value",
            UiProbeSupport.hasLabel(frame, "₱872,000.00"));

        search("Canon");
        UiProbeSupport.check("the omnibar filters name matches", table.getRowCount() == 2);
        search("SN-504X");
        UiProbeSupport.check("the omnibar filters serial matches", table.getRowCount() == 1);
        search("zzz");
        UiProbeSupport.check("no match shows the empty state",
            UiProbeSupport.labelShowing(screen, "No items match this filter."));
        search("");
        UiProbeSupport.check("clearing restores every row", table.getRowCount() == 10);

        select("e-002");
        UiProbeSupport.check("the preview shows the selected item",
            UiProbeSupport.labelShowing(screen, "Canon EOS R6"));
        UiProbeSupport.check("the preview shows the id", UiProbeSupport.hasLabel(screen, "e-002"));
        UiProbeSupport.check("the preview shows the value",
            UiProbeSupport.hasLabel(screen, "₱145,000.00"));
        UiProbeSupport.check("an available item can be flagged",
            button("Flag for maintenance").isEnabled());
        UiProbeSupport.check("an available item can be retired",
            button("Decommission").isEnabled());
        UiProbeSupport.check("clear is hidden unless flagged",
            !button("Clear maintenance").isVisible());
        UiProbeSupport.check("the status bar counts the selection",
            UiProbeSupport.hasLabel(frame, "10 items · 1/1 selected"));

        Equipment dewalt = item("e-010");
        UiProbeSupport.check("decommissioning succeeds",
            inventory.decommission(dewalt).isEmpty());
        screen.reload();
        select("e-010");
        UiProbeSupport.check("a retired item stays listed", table.getRowCount() == 10);
        UiProbeSupport.check("a retired item cannot be flagged",
            !button("Flag for maintenance").isEnabled());
        UiProbeSupport.check("a retired item cannot be retired again",
            !button("Decommission").isEnabled());
        UiProbeSupport.check("a retired item reads as read-only",
            UiProbeSupport.labelShowing(screen, "Decommissioned items are read-only."));

        select("e-001");
        UiProbeSupport.check("a loaned item cannot be retired",
            !button("Decommission").isEnabled());
        UiProbeSupport.check("a loaned item can be flagged",
            button("Flag for maintenance").isEnabled());
        UiProbeSupport.check("a loaned item explains the block",
            UiProbeSupport.labelShowing(screen,
                "This item is still on loan; clear or retire it after the return."));

        Equipment sony = item("e-001");
        UiProbeSupport.check("flagging a loaned item succeeds",
            inventory.flagMaintenance(sony).isEmpty());
        screen.reload();
        select("e-001");
        UiProbeSupport.check("a flagged item offers clear",
            button("Clear maintenance").isVisible());
        UiProbeSupport.check("clear is blocked while the item is out",
            !button("Clear maintenance").isEnabled());
        UiProbeSupport.check("the painted screen is not blank",
            UiProbeSupport.nonBlank(UiProbeSupport.paint(frame)) > 100);
        UiProbeSupport.check("the probe changed real state",
            store.equipment().stream().anyMatch(item -> item.getId().equals("e-010")
                && item.getStatus() == kitnapper.model.EquipmentStatus.DECOMMISSIONED));
    }

    private void search(String text) {
        frame.searchField().setText(text);
    }

    private void select(String id) {
        for (int row = 0; row < table.getRowCount(); row++) {
            Equipment item = table.itemAtView(row);
            if (item != null && item.getId().equals(id)) {
                table.setRowSelectionInterval(row, row);
                return;
            }
        }
    }

    private JButton button(String text) {
        JButton button = UiProbeSupport.button(preview, text);
        if (button == null) {
            throw new IllegalStateException("missing preview button: " + text);
        }
        return button;
    }

    private Equipment item(String id) {
        return store.equipment().stream()
            .filter(candidate -> candidate.getId().equals(id))
            .findFirst().orElseThrow();
    }
}
