package kitnapper.view;

import java.awt.Component;
import java.time.LocalDate;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import kitnapper.model.EquipmentCategory;
import kitnapper.model.LoanStatus;

/**
 * Layout probe for the shared loans table (§9): the fixed 280 px preview leaves
 * the table about 670 px of a 1280 px window. At that width a column must never
 * shrink below the width its content needs — dates, badges and money would clip
 * — so the table scrolls instead, and stretches to fill once the viewport is
 * wide enough. Guards the M4 clipping regression on All Loans and Dashboard.
 */
public final class LoansLayoutProbe {

    /** 1280 window − 240 sidebar − 280 preview − padding. */
    private static final int NARROW = 678;
    private static final int WIDE = 1020;
    private static final int HEIGHT = 400;

    private LoansLayoutProbe() { }

    public static void main(String[] args) throws Exception {
        Theme.apply(true);
        for (LoansTableModel.Mode mode : LoansTableModel.Mode.values()) {
            checkMode(mode);
        }
        UiProbeSupport.finish();
    }

    /**
     * Builds, sizes and measures one mode. Every step is its own EDT task and
     * the queue is drained in between: a viewport resize is a queued
     * ComponentEvent, so the width policy only runs once the EDT is free.
     */
    private static void checkMode(LoansTableModel.Mode mode) throws Exception {
        String label = mode.name().toLowerCase();
        JFrame[] holder = new JFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame();
            frame.setUndecorated(true);
            LoansTableSection section = new LoansTableSection(mode);
            section.setRows(rows(), "No loans yet.", "Checkouts appear here.");
            frame.setContentPane(section);
            frame.setSize(NARROW, HEIGHT);
            frame.setVisible(true);
            holder[0] = frame;
        });
        settle(holder[0]);

        SwingUtilities.invokeAndWait(() -> {
            JTable table = tableOf(holder[0]);
            LoansTableModel model = (LoansTableModel) table.getModel();
            boolean fits = LoansWidthPolicy.totalWidth(model) <= NARROW;
            UiProbeSupport.check(label + "-narrow-no-clipping", !clips(table));
            UiProbeSupport.check(label + "-narrow-mode", table.getAutoResizeMode()
                == (fits ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS : JTable.AUTO_RESIZE_OFF));
            holder[0].setSize(WIDE, HEIGHT);
        });
        settle(holder[0]);

        SwingUtilities.invokeAndWait(() -> {
            JTable table = tableOf(holder[0]);
            UiProbeSupport.check(label + "-wide-fills", table.getAutoResizeMode()
                == JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
            UiProbeSupport.check(label + "-wide-no-clipping", !clips(table));
            holder[0].dispose();
        });
    }

    /** Lets queued layout events run, then validates and paints once. */
    private static void settle(JFrame frame) throws Exception {
        Thread.sleep(120);
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> UiProbeSupport.settle(frame));
        Thread.sleep(60);
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static JTable tableOf(JFrame frame) {
        return ((LoansTableSection) frame.getContentPane()).table();
    }

    /** True when any column is narrower than the widest cell it must render. */
    private static boolean clips(JTable table) {
        int spacing = table.getIntercellSpacing().width;
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = table.getColumnModel().getColumn(column).getWidth();
            for (int row = 0; row < table.getRowCount(); row++) {
                Component cell = table.prepareRenderer(
                    table.getCellRenderer(row, column), row, column);
                if (cell.getPreferredSize().width + spacing > width) {
                    System.out.println("  clipped " + table.getColumnName(column)
                        + ": width=" + width + " needs="
                        + (cell.getPreferredSize().width + spacing));
                    return true;
                }
            }
        }
        return false;
    }

    /** Three rows long enough to expose narrow columns. */
    private static List<LoanRow> rows() {
        LocalDate t = LocalDate.of(2026, 11, 14);
        return List.of(
            new LoanRow("L-001", "Sony A7 IV", EquipmentCategory.CAMERA, "Student One",
                t.minusDays(3), t.plusDays(4), null, LoanStatus.ACTIVE, 0, 0.0, null),
            new LoanRow("L-002", "MacBook Pro M3 (14\")", EquipmentCategory.LAPTOP,
                "Student Two", t.minusDays(7), t, null, LoanStatus.ACTIVE, 0, 0.0, null),
            new LoanRow("L-003", "Rode Wireless PRO kit", EquipmentCategory.AUDIO,
                "Student One", t.minusDays(12), t.minusDays(2), null,
                LoanStatus.OVERDUE, 2, 100.0, null));
    }
}
