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
 * The §8.1 shared loans table: the {@link TableSkins} look plus the two
 * things only a {@code JTable} subclass can do — a {@code HOVER_ROW} band
 * painted beneath the cells (the cells are non-opaque on purpose, so the band
 * shows through), and a silent selection API the screen uses to keep table
 * and preview in sync. Columns, renderers and widths come from
 * {@link LoansTableModel}, so one table serves My Loans, All Loans and
 * Overdue without per-screen skins.
 */
final class LoansTable extends JTable {

    private static final long serialVersionUID = 1L;

    private final LoansTableModel model;
    private Consumer<LoanRow> onSelect = row -> { };
    private boolean syncing;
    private int hoverRow = -1;

    LoansTable(LoansTableModel.Mode mode) {
        super(new LoansTableModel(mode));
        model = (LoansTableModel) getModel();
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
    void setItems(List<LoanRow> rows) {
        syncing = true;
        model.setItems(rows);
        clearSelection();
        syncing = false;
        hoverRow = -1;
    }

    void setOnSelect(Consumer<LoanRow> listener) {
        onSelect = listener == null ? row -> { } : listener;
    }

    /** Selects the row holding {@code loanId}, silently; no-op when it is not shown. */
    void selectRow(String loanId) {
        int viewRow = viewRowOf(loanId);
        if (viewRow < 0) {
            return;
        }
        syncing = true;
        setRowSelectionInterval(viewRow, viewRow);
        syncing = false;
        scrollRectToVisible(getCellRect(viewRow, 0, true));
    }

    /** The loan at a view row (post-sort), or null. */
    LoanRow rowAt(int viewRow) {
        return viewRow < 0 ? null : model.rowAt(convertRowIndexToModel(viewRow));
    }

    /** The table inside its §8.1 scroll surface. */
    JScrollPane inScrollPane() {
        JScrollPane pane = new JScrollPane(this);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setOpaque(false);
        pane.getViewport().setBackground(Theme.bgSurface);
        pane.getVerticalScrollBar().setUnitIncrement(TableSkins.ROW_HEIGHT);
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        LoansWidthPolicy.install(this, model, pane);
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
        }
        applyPreferredWidths();
    }

    /** Restores the §8.1 widths after a stretch, so narrowing re-scrolls fairly. */
    private void applyPreferredWidths() {
        LoansWidthPolicy.applyPreferred(this, model);
    }

    private static TableCellRenderer rendererFor(LoansTableModel.Kind kind) {
        return switch (kind) {
            case TEXT -> TableSkins.textRenderer();
            case MONO -> TableSkins.monoRenderer();
            case DATE -> TableCells.date();
            case MONEY -> TableSkins.moneyRenderer();
            case STATUS -> TableCells.loanStatus();
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
            LoanRow row = rowAt(getSelectedRow());
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

    private int viewRowOf(String loanId) {
        for (int viewRow = 0; viewRow < getRowCount(); viewRow++) {
            LoanRow row = rowAt(viewRow);
            if (row != null && row.loanId().equals(loanId)) {
                return viewRow;
            }
        }
        return -1;
    }
}
