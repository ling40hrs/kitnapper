package kitnapper.view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;

/**
 * The §8.2 details table: {@link TableSkins} look plus the two things only a
 * {@code JTable} subclass can do — a {@code HOVER_ROW} band painted beneath the
 * cells (the cells are non-opaque on purpose, so the band shows through), and
 * a silent selection API the screen uses to keep table and cards in sync.
 * Clicking a row reports the selected item through {@link #setOnSelect}.
 */
final class CatalogTable extends JTable {

    private static final long serialVersionUID = 1L;

    /** Status column sized from the widest pill (D41): no clipped words. */
    private static final int STATUS_WIDTH = Badges.widestPillWidth()
        + TableSkins.CELL_PAD * 2 + 4;
    private static final int[] COLUMN_WIDTHS = { 185, 90, STATUS_WIDTH, 125, 145 };

    private final CatalogTableModel model;
    private Consumer<Equipment> onSelect = item -> { };
    private boolean syncing;
    private int hoverRow = -1;

    CatalogTable() {
        super(new CatalogTableModel());
        model = (CatalogTableModel) getModel();
        TableSkins.install(this);
        // The hover band is painted by this class, so the table paints its own fill.
        setOpaque(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        installColumns();
        installSorter();
        installSelection();
        installHover();
    }

    /** Replaces the rows without reporting a selection change to the screen. */
    void setItems(java.util.List<Equipment> items) {
        syncing = true;
        model.setItems(items);
        clearSelection();
        syncing = false;
        hoverRow = -1;
    }

    void setOnSelect(Consumer<Equipment> listener) {
        onSelect = listener == null ? item -> { } : listener;
    }

    /** Selects the row holding {@code id}, silently; no-op when it is not shown. */
    void selectItem(String id) {
        int viewRow = viewRowOf(id);
        if (viewRow < 0) {
            return;
        }
        syncing = true;
        setRowSelectionInterval(viewRow, viewRow);
        syncing = false;
        scrollRectToVisible(getCellRect(viewRow, 0, true));
    }

    /** The item at a view row (post-sort), or null. */
    Equipment itemAtView(int viewRow) {
        return viewRow < 0 ? null : model.itemAt(convertRowIndexToModel(viewRow));
    }

    /** The table inside its §8.1 scroll surface. */
    JScrollPane inScrollPane() {
        JScrollPane pane = new JScrollPane(this);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setOpaque(false);
        pane.getViewport().setBackground(Theme.bgSurface);
        pane.getVerticalScrollBar().setUnitIncrement(TableSkins.ROW_HEIGHT);
        TableWidthPolicy.install(this, pane);
        return pane;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Theme.bgSurface);
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (hoverRow >= 0 && hoverRow < getRowCount() && !isRowSelected(hoverRow)) {
            Rectangle band = getCellRect(hoverRow, 0, true);
            g2.setColor(Theme.hoverRow);
            g2.fillRect(0, band.y, getWidth(), band.height);
        }
        g2.dispose();
        super.paintComponent(g);
    }

    private void installColumns() {
        TableCellRenderer category = categoryRenderer();
        setDefaultRenderer(Object.class, TableSkins.textRenderer());
        getColumnModel().getColumn(CatalogTableModel.COL_NAME)
            .setCellRenderer(TableSkins.textRenderer());
        getColumnModel().getColumn(CatalogTableModel.COL_CATEGORY).setCellRenderer(category);
        getColumnModel().getColumn(CatalogTableModel.COL_STATUS)
            .setCellRenderer(TableSkins.statusRenderer());
        getColumnModel().getColumn(CatalogTableModel.COL_SERIAL)
            .setCellRenderer(TableSkins.monoRenderer());
        getColumnModel().getColumn(CatalogTableModel.COL_VALUE)
            .setCellRenderer(TableSkins.moneyRenderer());
        for (int column = 0; column < COLUMN_WIDTHS.length; column++) {
            getColumnModel().getColumn(column).setPreferredWidth(COLUMN_WIDTHS[column]);
        }
        setAutoCreateRowSorter(false);
    }

    private void installSorter() {
        TableRowSorter<CatalogTableModel> sorter = new TableRowSorter<>(model);
        sorter.setComparator(CatalogTableModel.COL_CATEGORY,
            (EquipmentCategory a, EquipmentCategory b) ->
                Formats.category(a).compareTo(Formats.category(b)));
        setRowSorter(sorter);
    }

    private void installSelection() {
        getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || syncing) {
                return;
            }
            Equipment item = itemAtView(getSelectedRow());
            if (item != null) {
                onSelect.accept(item);
            }
        });
    }

    private void installHover() {
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setHoverRow(rowAtPoint(e.getPoint()));
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                setHoverRow(-1);
            }
        });
    }

    private void setHoverRow(int row) {
        if (row != hoverRow) {
            hoverRow = row;
            repaint();
        }
    }

    private int viewRowOf(String id) {
        for (int viewRow = 0; viewRow < getRowCount(); viewRow++) {
            Equipment item = itemAtView(viewRow);
            if (item != null && item.getId().equals(id)) {
                return viewRow;
            }
        }
        return -1;
    }

    /** Display word for the typed category value; sorting keeps the enum. */
    private static TableCellRenderer categoryRenderer() {
        TableCellRenderer base = TableSkins.textRenderer();
        return (JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
            int column) -> base.getTableCellRendererComponent(table,
                value instanceof EquipmentCategory category ? Formats.category(category) : value,
                isSelected, hasFocus, row, column);
    }
}
