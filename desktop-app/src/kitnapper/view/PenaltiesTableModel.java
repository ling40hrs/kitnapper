package kitnapper.view;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import kitnapper.model.PenaltyStatus;

/**
 * The §8.6 penalties model: seven typed columns (String, Double,
 * {@link PenaltyStatus}, {@link LocalDate}) so {@code TableRowSorter} sorts by
 * meaning instead of text. Rows arrive newest assessed first from the
 * controller; the model never re-sorts or mutates them — the screen replaces
 * the snapshot after every query or settlement.
 */
final class PenaltiesTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    /** Renderer family per column; the type alone cannot tell an id from a name. */
    enum Kind { TEXT, MONO, DATE, MONEY, STATUS }

    private enum Field { ID, LOAN, ITEM, BORROWER, AMOUNT, STATUS, ASSESSED }

    private record Column(String name, Class<?> type, Kind kind, int width, Field field) { }

    private static final Column[] COLUMNS = {
        new Column("Penalty ID", String.class, Kind.MONO, 96, Field.ID),
        new Column("Loan", String.class, Kind.MONO, 76, Field.LOAN),
        new Column("Item", String.class, Kind.TEXT, 180, Field.ITEM),
        new Column("Borrower", String.class, Kind.TEXT, 130, Field.BORROWER),
        new Column("Amount", Double.class, Kind.MONEY, 96, Field.AMOUNT),
        new Column("Status", PenaltyStatus.class, Kind.STATUS, 100, Field.STATUS),
        new Column("Assessed", LocalDate.class, Kind.DATE, 104, Field.ASSESSED),
    };

    private final List<PenaltyRow> rows = new ArrayList<>();

    /** Renderer family for a column, read by {@link PenaltiesTable}. */
    Kind kindOf(int column) {
        return COLUMNS[column].kind();
    }

    /** Preferred column width in pixels (§8.6). */
    int widthOf(int column) {
        return COLUMNS[column].width();
    }

    /** Replaces the snapshot; the selection is re-applied by the caller. */
    void setItems(List<PenaltyRow> next) {
        rows.clear();
        rows.addAll(next);
        fireTableDataChanged();
    }

    /** The row at a model row, or null when the row is out of range. */
    PenaltyRow rowAt(int modelRow) {
        return modelRow >= 0 && modelRow < rows.size() ? rows.get(modelRow) : null;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column].name();
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return COLUMNS[column].type();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public Object getValueAt(int row, int column) {
        return value(rows.get(row), COLUMNS[column].field());
    }

    /** The typed value behind a column (§8.6). */
    private static Object value(PenaltyRow penalty, Field field) {
        return switch (field) {
            case ID -> penalty.penaltyId();
            case LOAN -> penalty.loanId();
            case ITEM -> penalty.itemName();
            case BORROWER -> penalty.borrowerName();
            case AMOUNT -> penalty.amount();
            case STATUS -> penalty.status();
            case ASSESSED -> penalty.assessedDate();
        };
    }
}
