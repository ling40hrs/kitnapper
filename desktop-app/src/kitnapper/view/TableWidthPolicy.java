package kitnapper.view;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * The §9 width policy for every table (decision D41): a column never shrinks
 * below the width its content needs. When the viewport is narrower than the
 * summed column widths the table scrolls horizontally and the columns keep
 * their preferred widths; once the viewport is wide enough the columns are
 * restored and then stretch to fill. Generalizes the loans-only policy that
 * shipped in M4, so catalog, inventory, penalties and loans share one rule.
 */
final class TableWidthPolicy {

    private TableWidthPolicy() {
    }

    /** Installs the policy using each column's current preferred width. */
    static void install(JTable table, JScrollPane pane) {
        install(table, preferredWidths(table), pane);
    }

    /** Installs the policy with explicit widths (a model's §8.1 column set). */
    static void install(JTable table, int[] widths, JScrollPane pane) {
        evaluate(table, widths, pane);
        pane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                evaluate(table, widths, pane);
            }
        });
    }

    /** Reads the preferred widths before any stretch has widened them. */
    static int[] preferredWidths(JTable table) {
        int[] widths = new int[table.getColumnCount()];
        for (int column = 0; column < widths.length; column++) {
            widths[column] = table.getColumnModel().getColumn(column).getPreferredWidth();
        }
        return widths;
    }

    /** Restores the preferred widths after a stretch, so narrowing scrolls fairly. */
    static void applyPreferred(JTable table, int[] widths) {
        for (int column = 0; column < widths.length && column < table.getColumnCount();
                column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        }
    }

    /** The table's natural width: the sum of the preferred column widths. */
    static int total(int[] widths) {
        int total = 0;
        for (int width : widths) {
            total += width;
        }
        return total;
    }

    private static void evaluate(JTable table, int[] widths, JScrollPane pane) {
        boolean fits = pane.getViewport().getWidth() >= total(widths);
        applyPreferred(table, widths);
        table.setAutoResizeMode(fits
            ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
            : JTable.AUTO_RESIZE_OFF);
    }
}
