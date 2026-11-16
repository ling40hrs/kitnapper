package kitnapper.view;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDate;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import kitnapper.model.EquipmentStatus;
import kitnapper.model.LoanStatus;
import kitnapper.model.PenaltyStatus;

/**
 * Cell renderers for the §6 tables: text, monospace, right-aligned money, the
 * one date format, and the status pill families from {@link Badges} and
 * {@link StatusBadges}. Each cell is non-opaque and paints its own
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

    /** Dates shown as {@code yyyy-MM-dd} while the model keeps their type. */
    static TableCellRenderer date() {
        TableCellRenderer base = new Cell(false, SwingConstants.LEFT);
        return (JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
            int column) -> base.getTableCellRendererComponent(table,
                value instanceof LocalDate date ? Formats.date(date) : value,
                isSelected, hasFocus, row, column);
    }

    /** Equipment status pill; the M3 catalog renderer. */
    static TableCellRenderer status() {
        return new PillCell((g, x, y, value) -> {
            if (value instanceof EquipmentStatus status) {
                Badges.paint(g, x, y, status);
            }
        });
    }

    /** Loan status pill (§8.1 mode tables). */
    static TableCellRenderer loanStatus() {
        return new PillCell((g, x, y, value) -> {
            if (value instanceof LoanStatus status) {
                StatusBadges.paintLoan(g, x, y, status);
            }
        });
    }

    /** Penalty status pill (penalties table, WP4). */
    static TableCellRenderer penaltyStatus() {
        return new PillCell((g, x, y, value) -> {
            if (value instanceof PenaltyStatus status) {
                StatusBadges.paintPenalty(g, x, y, status);
            }
        });
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

    /** Pill cell: one class, one paint hook per status family. */
    private static final class PillCell extends JComponent implements TableCellRenderer {

        private static final long serialVersionUID = 1L;

        private final PillPainter painter;
        private Object value;
        private boolean selected;

        PillCell(PillPainter painter) {
            this.painter = painter;
            setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
            this.value = value;
            selected = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            paintRowBackground(g2, this, selected);
            if (value != null) {
                painter.paint(g2, TableSkins.CELL_PAD,
                    (getHeight() - Badges.PILL_HEIGHT) / 2, value);
            }
            g2.dispose();
        }
    }

    /** Draws one pill into a cell at the given origin. */
    @FunctionalInterface
    private interface PillPainter {
        void paint(Graphics2D g, int x, int y, Object value);
    }
}
