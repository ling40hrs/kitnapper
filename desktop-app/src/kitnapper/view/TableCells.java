package kitnapper.view;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import kitnapper.model.EquipmentStatus;

/**
 * Cell renderers for the §6 table: text, monospace, right-aligned money, and the
 * {@link Badges} status pill. Each cell is non-opaque and paints its own
 * {@code ACCENT_TINT} selection plus the 1 px row separator, so the separator
 * stays continuous across columns.
 */
final class TableCells {

    private TableCells() { }

    static TableCellRenderer text() {
        return new Cell(false, SwingConstants.LEFT);
    }

    static TableCellRenderer mono() {
        return new Cell(true, SwingConstants.LEFT);
    }

    static TableCellRenderer money() {
        return new Cell(true, SwingConstants.RIGHT);
    }

    static TableCellRenderer status() {
        return new StatusCell();
    }

    /** Shared row background + separator so every column lines up. */
    static void paintRowBackground(Graphics2D g, Component c, boolean selected) {
        if (selected) {
            g.setColor(Theme.accentTint);
            g.fillRect(0, 0, c.getWidth(), c.getHeight());
        }
        g.setColor(Theme.borderSubtle);
        g.drawLine(0, c.getHeight() - 1, c.getWidth(), c.getHeight() - 1);
    }

    /** Text cell; monospace for IDs and serials, right aligned for money. */
    private static final class Cell extends JLabel implements TableCellRenderer {

        private static final long serialVersionUID = 1L;

        private final boolean money;
        private boolean selected;

        Cell(boolean mono, int alignment) {
            this.money = alignment == SwingConstants.RIGHT;
            setOpaque(false);
            setFont(mono ? Fonts.mono(14, java.awt.Font.PLAIN) : Fonts.text(14, java.awt.Font.PLAIN));
            setHorizontalAlignment(alignment);
            setBorder(BorderFactory.createEmptyBorder(0, TableSkins.CELL_PAD, 0,
                TableSkins.CELL_PAD));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
            selected = isSelected;
            setText(textFor(value));
            setForeground(Theme.textPrimary);
            return this;
        }

        private String textFor(Object value) {
            if (value == null) {
                return "";
            }
            if (money && value instanceof Number number) {
                return Formats.money(number.doubleValue());
            }
            return String.valueOf(value);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            paintRowBackground(g2, this, selected);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Status cell: delegates to {@link Badges} so the pill is identical everywhere. */
    private static final class StatusCell extends JComponent implements TableCellRenderer {

        private static final long serialVersionUID = 1L;

        private EquipmentStatus status;
        private boolean selected;

        StatusCell() {
            setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
            status = value instanceof EquipmentStatus s ? s : null;
            selected = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            paintRowBackground(g2, this, selected);
            if (status != null) {
                Badges.paint(g2, TableSkins.CELL_PAD, (getHeight() - 20) / 2, status);
            }
            g2.dispose();
        }
    }
}
