package kitnapper.view;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * The §9 width policy for the shared loans table. The fixed 280 px preview
 * leaves the table about 670 px of a 1280 px window, and the 1024 px minimum
 * leaves less, so a column must never shrink below its preferred width — dates,
 * badges and money would clip. When the columns do not fit the table scrolls
 * horizontally; when the viewport is wide enough the columns stretch to fill.
 */
final class LoansWidthPolicy {

    private LoansWidthPolicy() {
    }

    /** Applies the policy to a table inside its scroll surface. */
    static void install(JTable table, LoansTableModel model, JScrollPane pane) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        pane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                if (pane.getViewport().getWidth() >= totalWidth(model)) {
                    table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
                } else {
                    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                    applyPreferred(table, model);
                }
            }
        });
    }

    /** Restores the §8.1 widths after a stretch, so narrowing re-scrolls fairly. */
    static void applyPreferred(JTable table, LoansTableModel model) {
        for (int column = 0; column < model.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(model.widthOf(column));
        }
    }

    /** The table's natural width: the sum of the model's preferred widths. */
    static int totalWidth(LoansTableModel model) {
        int total = 0;
        for (int column = 0; column < model.getColumnCount(); column++) {
            total += model.widthOf(column);
        }
        return total;
    }
}
