package kitnapper.view;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import kitnapper.model.LoanStatus;

/**
 * The §8.1 shared loans model: three column sets over {@link LoanRow}, with
 * typed values (String, {@link LoanStatus}, {@link Long}, {@link Double},
 * {@link LocalDate}) so {@code TableRowSorter} sorts by meaning instead of
 * text. Rows arrive already in service order; the model never re-sorts or
 * mutates them — a controller replaces the snapshot after every query.
 */
final class LoansTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    /** Which screen's columns to show (§8.1). */
    enum Mode { MY, ALL, OVERDUE }

    /** Renderer family per column; the type alone cannot tell an id from a name. */
    enum Kind { TEXT, MONO, DATE, MONEY, STATUS }

    private enum Field { ID, ITEM, BORROWER, BORROWED, DUE, RETURNED, STATUS, DAYS_LATE, FEE }

    private record Column(String name, Class<?> type, Kind kind, int width, Field field) { }

    private static final Column[] MY_COLUMNS = {
        new Column("ID", String.class, Kind.MONO, 76, Field.ID),
        new Column("Item", String.class, Kind.TEXT, 200, Field.ITEM),
        new Column("Borrowed", LocalDate.class, Kind.DATE, 104, Field.BORROWED),
        new Column("Due", LocalDate.class, Kind.DATE, 104, Field.DUE),
        new Column("Status", LoanStatus.class, Kind.STATUS, 104, Field.STATUS),
        new Column("Fee", Double.class, Kind.MONEY, 96, Field.FEE),
    };

    private static final Column[] ALL_COLUMNS = {
        new Column("ID", String.class, Kind.MONO, 76, Field.ID),
        new Column("Item", String.class, Kind.TEXT, 180, Field.ITEM),
        new Column("Borrower", String.class, Kind.TEXT, 130, Field.BORROWER),
        new Column("Borrowed", LocalDate.class, Kind.DATE, 100, Field.BORROWED),
        new Column("Due", LocalDate.class, Kind.DATE, 100, Field.DUE),
        new Column("Returned", LocalDate.class, Kind.DATE, 100, Field.RETURNED),
        new Column("Status", LoanStatus.class, Kind.STATUS, 100, Field.STATUS),
        new Column("Fee", Double.class, Kind.MONEY, 90, Field.FEE),
    };

    private static final Column[] OVERDUE_COLUMNS = {
        new Column("ID", String.class, Kind.MONO, 76, Field.ID),
        new Column("Item", String.class, Kind.TEXT, 190, Field.ITEM),
        new Column("Borrower", String.class, Kind.TEXT, 130, Field.BORROWER),
        new Column("Due", LocalDate.class, Kind.DATE, 100, Field.DUE),
        new Column("Days late", Long.class, Kind.TEXT, 90, Field.DAYS_LATE),
        new Column("Accrued fee", Double.class, Kind.MONEY, 110, Field.FEE),
    };

    private final Mode mode;
    private final Column[] columns;
    private final List<LoanRow> rows = new ArrayList<>();

    LoansTableModel(Mode mode) {
        this.mode = mode;
        this.columns = switch (mode) {
            case MY -> MY_COLUMNS;
            case ALL -> ALL_COLUMNS;
            case OVERDUE -> OVERDUE_COLUMNS;
        };
    }

    Mode mode() {
        return mode;
    }

    /** Renderer family for a column, read by {@link LoansTable}. */
    Kind kindOf(int column) {
        return columns[column].kind();
    }

    /** Preferred column width in pixels (§8.1). */
    int widthOf(int column) {
        return columns[column].width();
    }

    /** Replaces the snapshot; the selection is re-applied by the caller. */
    void setItems(List<LoanRow> next) {
        rows.clear();
        rows.addAll(next);
        fireTableDataChanged();
    }

    /** The row at a model row, or null when the row is out of range. */
    LoanRow rowAt(int modelRow) {
        return modelRow >= 0 && modelRow < rows.size() ? rows.get(modelRow) : null;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column].name();
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return columns[column].type();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public Object getValueAt(int row, int column) {
        return value(rows.get(row), columns[column].field());
    }

    /** The typed value behind a column (§8.1); null only for an open return. */
    private static Object value(LoanRow loan, Field field) {
        return switch (field) {
            case ID -> loan.loanId();
            case ITEM -> loan.itemName();
            case BORROWER -> loan.borrowerName();
            case BORROWED -> loan.borrowDate();
            case DUE -> loan.dueDate();
            case RETURNED -> loan.returnDate();
            case STATUS -> loan.status();
            case DAYS_LATE -> loan.daysLate();
            case FEE -> loan.accruedFee();
        };
    }
}
