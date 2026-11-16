package kitnapper.view;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.EquipmentStatus;

/**
 * The inventory table's model (design §5 "Inventory"): the same five typed
 * columns the catalog table uses, so {@code TableRowSorter} sorts by meaning
 * rather than by rendered text. The model never mutates items;
 * {@link InventoryScreen} replaces the snapshot after every query or change.
 */
final class InventoryTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    static final int COL_NAME = 0;
    static final int COL_CATEGORY = 1;
    static final int COL_STATUS = 2;
    static final int COL_SERIAL = 3;
    static final int COL_VALUE = 4;

    private static final String[] COLUMNS = {
        "Name", "Category", "Status", "Serial", "Replacement value"
    };

    private static final Class<?>[] TYPES = {
        String.class, EquipmentCategory.class, EquipmentStatus.class, String.class, Double.class
    };

    private final List<Equipment> items = new ArrayList<>();

    void setItems(List<Equipment> next) {
        items.clear();
        items.addAll(next);
        fireTableDataChanged();
    }

    Equipment itemAt(int modelRow) {
        return modelRow >= 0 && modelRow < items.size() ? items.get(modelRow) : null;
    }

    @Override
    public int getRowCount() {
        return items.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return TYPES[column];
    }

    @Override
    public Object getValueAt(int row, int column) {
        Equipment item = items.get(row);
        return switch (column) {
            case COL_NAME -> item.getName();
            case COL_CATEGORY -> item.getCategory();
            case COL_STATUS -> item.getStatus();
            case COL_SERIAL -> item.getSerialNumber();
            case COL_VALUE -> item.getReplacementValue();
            default -> null;
        };
    }
}
