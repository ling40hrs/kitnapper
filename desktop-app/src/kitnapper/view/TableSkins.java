package kitnapper.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * The §6 table look: 40 px rows, 36 px header in 12 px secondary text, 1 px
 * {@code BORDER_SUBTLE} separators, no vertical gridlines, {@code ACCENT_TINT}
 * selection and a chevron on the active sort column. Cell renderers live in
 * {@link TableCells}; row hover lives in {@code CatalogTable}, which owns the
 * {@code JTable} subclass that can paint it.
 */
final class TableSkins {

    static final int ROW_HEIGHT = 40;
    static final int HEADER_HEIGHT = 36;
    static final int CELL_PAD = 8;

    private TableSkins() { }

    /** Applies the shared look. Cell renderers are assigned per column. */
    static void install(JTable table) {
        table.setRowHeight(ROW_HEIGHT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Theme.bgSurface);
        table.setForeground(Theme.textPrimary);
        table.setFont(Fonts.text(14, Font.PLAIN));
        table.setSelectionBackground(Theme.accentTint);
        table.setSelectionForeground(Theme.textPrimary);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        applyHeader(table);
    }

    /** 36 px header, left aligned, with a chevron on the active sort column. */
    static void applyHeader(JTable table) {
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, HEADER_HEIGHT));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderCell());
    }

    static TableCellRenderer textRenderer() {
        return TableCells.text();
    }

    static TableCellRenderer monoRenderer() {
        return TableCells.mono();
    }

    static TableCellRenderer moneyRenderer() {
        return TableCells.money();
    }

    /** Paints the §3.3 badge, so status is dot + wrench + word, never colour alone. */
    static TableCellRenderer statusRenderer() {
        return TableCells.status();
    }

    /** Header cell: surface fill, separator, 12 px secondary text, sort chevron. */
    private static final class HeaderCell extends JLabel implements TableCellRenderer {

        private static final long serialVersionUID = 1L;

        private String indicator = "";

        HeaderCell() {
            setOpaque(false);
            setFont(Fonts.text(12, Font.PLAIN));
            setForeground(Theme.textSecondary);
            setBorder(BorderFactory.createEmptyBorder(0, CELL_PAD, 0, CELL_PAD));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : String.valueOf(value));
            indicator = sortIndicator(table, column);
            return this;
        }

        private static String sortIndicator(JTable table, int column) {
            if (table.getRowSorter() == null) {
                return "";
            }
            int viewColumn = table.convertColumnIndexToView(column);
            for (var key : table.getRowSorter().getSortKeys()) {
                if (key.getColumn() == viewColumn) {
                    return key.getSortOrder() == SortOrder.DESCENDING ? "\u25BC" : "\u25B2";
                }
            }
            return "";
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Theme.bgSurface);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Theme.borderSubtle);
            g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            g2.dispose();
            super.paintComponent(g);
            if (!indicator.isEmpty()) {
                Graphics2D chevron = (Graphics2D) g.create();
                chevron.setFont(Fonts.text(9, Font.PLAIN));
                chevron.setColor(Theme.textMuted);
                int width = chevron.getFontMetrics().stringWidth(indicator);
                chevron.drawString(indicator, getWidth() - width - CELL_PAD, getHeight() / 2 + 4);
                chevron.dispose();
            }
        }
    }
}
