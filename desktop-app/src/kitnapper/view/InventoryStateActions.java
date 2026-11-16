package kitnapper.view;

import java.awt.Dimension;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import kitnapper.model.Equipment;
import kitnapper.model.EquipmentStatus;

/**
 * The inventory preview's pinned state actions (design §5 "Inventory"):
 * flag for maintenance or clear it, and decommission. Enablement follows the
 * item's status and whether it is out on loan, so the screen never offers a
 * transition the service would reject; the hint line explains the block.
 */
final class InventoryStateActions extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JButton edit = UiFactory.primaryButton("Edit item");
    private final JButton flag = UiFactory.secondaryButton("Flag for maintenance");
    private final JButton clear = UiFactory.secondaryButton("Clear maintenance");
    private final JButton retire = UiFactory.destructiveButton("Decommission");
    private final JLabel hint = UiFactory.label("", "muted");
    private final Consumer<Equipment> onEdit;
    private final Consumer<Equipment> onFlag;
    private final Consumer<Equipment> onClear;
    private final Consumer<Equipment> onRetire;
    private Equipment shown;

    InventoryStateActions(Consumer<Equipment> onEdit, Consumer<Equipment> onFlag,
                          Consumer<Equipment> onClear, Consumer<Equipment> onRetire) {
        this.onEdit = onEdit;
        this.onFlag = onFlag;
        this.onClear = onClear;
        this.onRetire = onRetire;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        for (JButton button : new JButton[] { edit, flag, clear, retire }) {
            button.setAlignmentX(0f);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            add(button);
            add(Box.createVerticalStrut(6));
        }
        edit.addActionListener(e -> fire(onEdit));
        flag.addActionListener(e -> fire(onFlag));
        clear.addActionListener(e -> fire(onClear));
        retire.addActionListener(e -> fire(onRetire));
        hint.setAlignmentX(0f);
        add(hint);
    }

    /** Recomputes which actions are offered for the selected item. */
    void show(Equipment item, boolean onLoan, String hintText) {
        shown = item;
        EquipmentStatus status = item.getStatus();
        boolean retired = status == EquipmentStatus.DECOMMISSIONED;
        edit.setEnabled(!retired);
        flag.setEnabled(status == EquipmentStatus.AVAILABLE || status == EquipmentStatus.LOANED);
        clear.setVisible(status == EquipmentStatus.UNDER_MAINTENANCE);
        clear.setEnabled(!retired && !onLoan);
        retire.setEnabled(!retired && !onLoan
            && (status == EquipmentStatus.AVAILABLE || status == EquipmentStatus.UNDER_MAINTENANCE));
        boolean hasHint = hintText != null && !hintText.isBlank();
        hint.setText(hasHint ? hintText : "");
        hint.setVisible(hasHint);
        revalidate();
        repaint();
    }

    /** Nothing selected: drop the target so a stale action cannot fire. */
    void clear() {
        shown = null;
    }

    private void fire(Consumer<Equipment> action) {
        if (shown != null) {
            action.accept(shown);
        }
    }
}
