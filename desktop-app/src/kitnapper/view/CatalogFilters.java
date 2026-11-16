package kitnapper.view;

import java.awt.FlowLayout;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import kitnapper.model.EquipmentCategory;
import kitnapper.model.EquipmentStatus;

/**
 * The §8.5 catalog command row mounted in the omnibar: category combo, status
 * combo, and the Table/Cards segmented control. It owns only view state —
 * {@link CatalogScreen} reads the current filter values and runs the query.
 */
final class CatalogFilters extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JComboBox<String> category;
    private final JComboBox<String> status;
    private final SegmentedControl layout;
    private Runnable onFilterChanged = () -> { };
    private IntConsumer onLayoutChanged = index -> { };
    private boolean adjusting;

    CatalogFilters() {
        super(new FlowLayout(FlowLayout.LEFT, 8, 8));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        category = UiFactory.comboBox("Category filter", categoryItems());
        status = UiFactory.comboBox("Status filter", statusItems());
        layout = new SegmentedControl("Table", "Cards");
        add(category);
        add(status);
        add(layout);
        category.addActionListener(e -> filterChanged());
        status.addActionListener(e -> filterChanged());
        layout.addChangeListener(index -> onLayoutChanged.accept(index));
    }

    /** Null means "any category". */
    EquipmentCategory categoryFilter() {
        int index = category.getSelectedIndex();
        return index <= 0 ? null : EquipmentCategory.values()[index - 1];
    }

    /** Null means "any status". */
    EquipmentStatus statusFilter() {
        int index = status.getSelectedIndex();
        return index <= 0 ? null : EquipmentStatus.values()[index - 1];
    }

    int layoutIndex() {
        return layout.getSelectedIndex();
    }

    /** The Table/Cards control, for the probe's click-to-focus check (WP5 F12). */
    SegmentedControl layoutControl() {
        return layout;
    }

    void setOnFilterChanged(Runnable listener) {
        onFilterChanged = listener == null ? () -> { } : listener;
    }

    void setOnLayoutChanged(IntConsumer listener) {
        onLayoutChanged = listener == null ? index -> { } : listener;
    }

    /** Combo index into the filter list (0 = any), used by Clear filters and probes. */
    void selectCategory(EquipmentCategory value) {
        adjusting = true;
        category.setSelectedIndex(value == null ? 0 : value.ordinal() + 1);
        adjusting = false;
        filterChanged();
    }

    void selectStatus(EquipmentStatus value) {
        adjusting = true;
        status.setSelectedIndex(value == null ? 0 : value.ordinal() + 1);
        adjusting = false;
        filterChanged();
    }

    /** Switches Table/Cards exactly like a click, including the change event. */
    void selectLayout(int index) {
        layout.setSelectedIndex(index);
    }

    void refreshTheme() {
        UiFactory.refresh(this);
        repaint();
    }

    private void filterChanged() {
        if (!adjusting) {
            onFilterChanged.run();
        }
    }

    private static String[] categoryItems() {
        EquipmentCategory[] values = EquipmentCategory.values();
        String[] items = new String[values.length + 1];
        items[0] = "Any category";
        for (int i = 0; i < values.length; i++) {
            items[i + 1] = Formats.category(values[i]);
        }
        return items;
    }

    private static String[] statusItems() {
        EquipmentStatus[] values = EquipmentStatus.values();
        String[] items = new String[values.length + 1];
        items[0] = "Any status";
        for (int i = 0; i < values.length; i++) {
            items[i + 1] = Badges.wordOf(values[i]);
        }
        return items;
    }
}
