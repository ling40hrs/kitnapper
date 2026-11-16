package kitnapper.view;

import java.awt.Dimension;
import java.util.Optional;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import kitnapper.model.DigitalEquipment;
import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.MechanicalTool;

/**
 * The add/edit form's fields (design §8.2, D19): name, category, serial,
 * replacement value, and the one extra field the item's concrete kind carries.
 * Add mode offers the field implied by the selected category and swaps it when
 * the category changes; edit mode shows only the existing kind's field, so a
 * category edit can never migrate the class.
 */
final class EquipmentFields extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String[] CATEGORY_LABELS = {
        "Camera", "Lens", "Laptop", "Audio", "Tool", "Other"
    };

    private static final EquipmentCategory[] CATEGORIES = {
        EquipmentCategory.CAMERA, EquipmentCategory.LENS, EquipmentCategory.LAPTOP,
        EquipmentCategory.AUDIO, EquipmentCategory.TOOL, EquipmentCategory.OTHER
    };

    private final Equipment existing;
    private final JTextField name = FieldSkin.field("e.g. Canon EOS R6");
    private final JTextField serial = FieldSkin.field("e.g. SN-EOSR6-002");
    private final JTextField value = FieldSkin.field("e.g. 145000");
    private final JTextField warranty = FieldSkin.field("e.g. care@institution.edu");
    private final JTextField interval = FieldSkin.field("e.g. 90");
    private final JComboBox<String> category =
        UiFactory.comboBox("Category", CATEGORY_LABELS);
    private final JPanel kindSlot = new JPanel();
    private Runnable onCategoryChanged;

    /** @param existing the item being edited, or null when adding */
    EquipmentFields(Equipment existing) {
        this.existing = existing;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        kindSlot.setOpaque(false);
        kindSlot.setLayout(new BoxLayout(kindSlot, BoxLayout.Y_AXIS));
        kindSlot.setAlignmentX(0f);
        add(row("Name", name));
        add(row("Category", category));
        add(row("Serial number", serial));
        add(row("Replacement value (₱)", value));
        add(kindSlot);
        fill(existing);
        category.addActionListener(e -> {
            refreshKindField();
            if (onCategoryChanged != null) {
                onCategoryChanged.run();
            }
        });
        refreshKindField();
    }

    /** Notified when the category changes, so the photo well can re-tile. */
    void setOnCategoryChanged(Runnable listener) {
        onCategoryChanged = listener;
    }

    /** The collected values; numeric fields stay raw text (controller parses). */
    EquipmentForm form() {
        return new EquipmentForm(text(name), selectedCategory(), text(serial), text(value),
            text(warranty), text(interval));
    }

    /** Inline validation before anything reaches the service. */
    Optional<String> validationError() {
        if (text(name).isBlank()) {
            return Optional.of("Name is required.");
        }
        if (text(serial).isBlank()) {
            return Optional.of("Serial number is required.");
        }
        if (!isNumber(text(value))) {
            return Optional.of("Replacement value must be a number, zero or more.");
        }
        if (!text(interval).isBlank() && !isWholeNumber(text(interval))) {
            return Optional.of("Maintenance interval must be a whole number of days.");
        }
        return Optional.empty();
    }

    EquipmentCategory selectedCategory() {
        int index = Math.max(0, Math.min(CATEGORIES.length - 1, category.getSelectedIndex()));
        return CATEGORIES[index];
    }

    /** The kind field currently on screen: true when it is the warranty field. */
    boolean showsWarranty() {
        return kindSlot.getComponentCount() > 0 && (Boolean) kindSlot.getClientProperty("warranty");
    }

    private void fill(Equipment item) {
        if (item == null) {
            return;
        }
        name.setText(item.getName());
        serial.setText(item.getSerialNumber());
        value.setText(Formats.money(item.getReplacementValue()).substring(1));
        category.setSelectedIndex(indexOf(item.getCategory()));
        if (item instanceof DigitalEquipment digital) {
            warranty.setText(digital.getWarrantyContact().orElse(""));
        } else if (item instanceof MechanicalTool tool) {
            interval.setText(tool.getMaintenanceIntervalDays() == 0
                ? "" : String.valueOf(tool.getMaintenanceIntervalDays()));
        }
    }

    /** Add mode follows the category; edit mode follows the existing kind (D19). */
    private void refreshKindField() {
        kindSlot.removeAll();
        boolean warrantyField = existing == null
            ? selectedCategory() != EquipmentCategory.TOOL
            : existing instanceof DigitalEquipment;
        kindSlot.putClientProperty("warranty", warrantyField);
        kindSlot.add(warrantyField
            ? row("Warranty contact (optional)", warranty)
            : row("Maintenance interval in days (optional)", interval));
        kindSlot.revalidate();
        kindSlot.repaint();
    }

    private static JPanel row(String label, JComponent field) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(0f);
        javax.swing.JLabel text = UiFactory.label(label, "secondary");
        text.setAlignmentX(0f);
        field.setAlignmentX(0f);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.add(text);
        row.add(Box.createVerticalStrut(4));
        row.add(field);
        row.add(Box.createVerticalStrut(12));
        return row;
    }

    private static String text(JTextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private static boolean isNumber(String raw) {
        try {
            return !raw.isBlank() && Double.parseDouble(raw.replace("₱", "")
                .replace(",", "").trim()) >= 0;
        } catch (NumberFormatException invalid) {
            return false;
        }
    }

    private static boolean isWholeNumber(String raw) {
        try {
            return Integer.parseInt(raw.trim()) >= 0;
        } catch (NumberFormatException invalid) {
            return false;
        }
    }

    private static int indexOf(EquipmentCategory category) {
        for (int index = 0; index < CATEGORIES.length; index++) {
            if (CATEGORIES[index] == category) {
                return index;
            }
        }
        return CATEGORIES.length - 1;
    }
}
