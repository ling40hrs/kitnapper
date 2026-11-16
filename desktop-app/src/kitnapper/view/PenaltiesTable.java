package kitnapper.view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

/**
 * The §8.6 penalties table: the {@link TableSkins} look plus the two things
 * only a {@code JTable} subclass can do — a {@code HOVER_ROW} band painted
 * beneath the cells (the cells are non-opaque on purpose, so the band shows
 * through), and a silent selection API the screen uses to keep table and
 * preview in sync. Columns, renderers and widths come from
 * {@link PenaltiesTableModel}; hovering and sorting behave exactly like
 * {@link LoansTable}.
 */
final class PenaltiesTable extends JTable {

    private static final long serialVersionUID = 1L;

    private final PenaltiesTableModel model;
    private Consumer<PenaltyRow> onSelect = row -> { };
    private boolean syncing;
    private int hoverRow = -1;

    PenaltiesTable() {
        super(new PenaltiesTableModel());
        model = (PenaltiesTableModel) getModel();
        TableSkins.install(this);
        // The hover band is painted by this class, so the table paints its own fill.
        setOpaque(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setAutoCreateRowSorter(false);
        installColumns();
        installSorter();
        installSelection();
        installHover();
    }

    /** Replaces the rows without reporting a selection change to the screen. */
    void setItems(List<PenaltyRow> rows) {
        syncing = true;
        model.setItems(rows);
        clearSelection();
        syncing = false;
        hoverRow = -1;
    }

    void setOnSelect(Consumer<PenaltyRow> listener) {
        onSelect = listener == null ? row -> { } : listener;
    }

    /** Selects the row holding {@code penaltyId}, silently; no-op when absent. */
    void selectRow(String penaltyId) {
        int viewRow = viewRowOf(penaltyId);
        if (viewRow < 0) {
            return;
        }
        syncing = true;
        setRowSelectionInterval(viewRow, viewRow);
        syncing = false;
        scrollRectToVisible(getCellRect(viewRow, 0, true));
    }

    /** The penalty at a view row (post-sort), or null. */
    PenaltyRow rowAt(int viewRow) {
        return viewRow < 0 ? null : model.rowAt(convertRowIndexToModel(viewRow));
    }

    /** The table inside its §8.1 scroll surface. */
    JScrollPane inScrollPane() {
        JScrollPane pane = new JScrollPane(this);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setOpaque(false);
        pane.getViewport().setBackground(Theme.bgSurface);
        pane.getVerticalScrollBar().setUnitIncrement(TableSkins.ROW_HEIGHT);
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

    /** One renderer family per column kind; widths come from the model. */
    private void installColumns() {
        setDefaultRenderer(Object.class, TableSkins.textRenderer());
        for (int column = 0; column < model.getColumnCount(); column++) {
            getColumnModel().getColumn(column).setCellRenderer(rendererFor(model.kindOf(column)));
            getColumnModel().getColumn(column).setPreferredWidth(model.widthOf(column));
        }
    }

    private static TableCellRenderer rendererFor(PenaltiesTableModel.Kind kind) {
        return switch (kind) {
            case TEXT -> TableSkins.textRenderer();
            case MONO -> TableSkins.monoRenderer();
            case DATE -> TableCells.date();
            case MONEY -> TableSkins.moneyRenderer();
            case STATUS -> TableCells.penaltyStatus();
        };
    }

    /** Typed column classes let the sorter compare dates and money by meaning. */
    private void installSorter() {
        setRowSorter(new TableRowSorter<>(model));
    }

    private void installSelection() {
        getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || syncing) {
                return;
            }
            PenaltyRow row = rowAt(getSelectedRow());
            if (row != null) {
                onSelect.accept(row);
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

    private int viewRowOf(String penaltyId) {
        for (int viewRow = 0; viewRow < getRowCount(); viewRow++) {
            PenaltyRow row = rowAt(viewRow);
            if (row != null && row.penaltyId().equals(penaltyId)) {
                return viewRow;
            }
        }
        return -1;
    }
}
