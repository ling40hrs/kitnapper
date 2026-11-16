package kitnapper.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

/**
 * The §8.4 assertions behind {@link GeometryProbe}: preview geometry, table
 * width mode, toast clearance and the Settings fit-or-scroll rule. Split out of
 * the probe so neither file approaches the 200-line cap.
 */
final class GeometryChecks {

    private GeometryChecks() { }

    /** Preview rows inside the pane, the action bar pinned, rows reachable. */
    static void preview(String tag, JComponent screen) {
        JComponent preview = preview(screen);
        if (preview == null) {
            return;
        }
        Rectangle bar = rectIn(preview, actionBar(preview));
        UiProbeSupport.check(tag + "-action-inside", bar.width > 0 && bar.height > 0
            && bar.x >= 0 && bar.y >= 0
            && bar.x + bar.width <= preview.getWidth()
            && bar.y + bar.height <= preview.getHeight());
        Rectangle info = rectIn(preview, infoScroll(preview));
        UiProbeSupport.check(tag + "-no-overlap", info.y + info.height <= bar.y);
        boolean inside = true;
        for (Component child : descendants(preview)) {
            if (child instanceof JLabel label && label.isShowing()) {
                Rectangle r = rectIn(preview, label);
                if (r.x < 0 || r.y < 0 || r.x + r.width > preview.getWidth()
                        || r.y + r.height > preview.getHeight()) {
                    inside = false;
                }
            }
        }
        UiProbeSupport.check(tag + "-label-inside", inside);
        JScrollPane scroll = UiProbeSupport.find(preview, JScrollPane.class);
        if (scroll != null) {
            int need = scroll.getViewport().getView().getPreferredSize().height;
            int have = scroll.getViewport().getExtentSize().height;
            UiProbeSupport.check(tag + "-scroll-guard", have <= 0 || need <= have
                || scroll.getVerticalScrollBar().isShowing());
        }
    }

    /** A narrow table scrolls at its preferred widths instead of clipping. */
    static void table(String tag, JComponent screen) {
        JTable table = UiProbeSupport.find(screen, JTable.class);
        if (table == null) {
            return;
        }
        JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(
            JScrollPane.class, table);
        int total = TableWidthPolicy.total(TableWidthPolicy.preferredWidths(table));
        boolean fits = pane.getViewport().getWidth() >= total;
        UiProbeSupport.check(tag + "-table-mode", table.getAutoResizeMode() == (fits
            ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS : JTable.AUTO_RESIZE_OFF));
        JScrollBar bar = pane.getHorizontalScrollBar();
        UiProbeSupport.check(tag + "-table-hbar", bar.isShowing() != fits);
        statusColumn(tag, table);
        if (!fits) {
            int actual = 0;
            for (int column = 0; column < table.getColumnCount(); column++) {
                actual += table.getColumnModel().getColumn(column).getWidth();
            }
            UiProbeSupport.check(tag + "-table-preferred", actual >= total);
        }
    }

    /**
     * The status column is sized from the widest badge, so "Decommissioned"
     * and "Under maintenance" fit whole (WP5 F11). Only equipment tables carry
     * an {@code EquipmentStatus} column.
     */
    private static void statusColumn(String tag, JTable table) {
        int column = -1;
        for (int index = 0; index < table.getColumnCount(); index++) {
            if (table.getColumnClass(index) == kitnapper.model.EquipmentStatus.class) {
                column = index;
            }
        }
        if (column < 0) {
            return;
        }
        UiProbeSupport.check(tag + "-status-column-fits",
            table.getColumnModel().getColumn(column).getWidth()
                >= Badges.widestPillWidth() + TableSkins.CELL_PAD * 2);
    }

    /** Toasts never cover the status bar or the pinned preview action (D43). */
    static void toast(String tag, MainFrame frame, JComponent screen) {
        Rectangle status = rectIn(frame.getContentPane(),
            UiProbeSupport.find(frame, StatusBar.class));
        JComponent preview = preview(screen);
        Rectangle action = preview == null ? null
            : rectIn(frame.getContentPane(), actionBar(preview));
        for (Component toast : frame.getLayeredPane().getComponentsInLayer(
                javax.swing.JLayeredPane.POPUP_LAYER)) {
            Rectangle r = rectIn(frame.getContentPane(), toast);
            UiProbeSupport.check(tag + "-toast-clear", !r.intersects(status)
                && (action == null || !r.intersects(action)));
        }
    }

    /** Every Settings label stays inside the page or is reachable by scrolling. */
    static void settingsFit(String tag, JComponent screen) {
        boolean inside = true;
        for (Component child : descendants(screen)) {
            if (child instanceof JLabel label && label.isShowing()) {
                Rectangle r = rectIn(screen, label);
                if (r.y + r.height > screen.getHeight()) {
                    inside = false;
                }
            }
        }
        JScrollPane scroll = UiProbeSupport.find(screen, JScrollPane.class);
        UiProbeSupport.check(tag + "-settings-fit", inside || (scroll != null
            && scroll.getVerticalScrollBar().isShowing()));
    }

    private static JComponent preview(JComponent screen) {
        for (Component child : descendants(screen)) {
            if (child.getClass().getSimpleName().endsWith("PreviewPane")) {
                return (JComponent) child;
            }
        }
        return null;
    }

    private static JComponent infoScroll(JComponent preview) {
        return (JComponent) detail(preview).getComponents()[0];
    }

    private static JComponent actionBar(JComponent preview) {
        return (JComponent) detail(preview).getComponents()[1];
    }

    private static JComponent detail(JComponent preview) {
        Container stack = (Container) preview.getComponents()[0];
        return (JComponent) stack.getComponents()[0];
    }

    private static Rectangle rectIn(Component root, Component child) {
        return SwingUtilities.convertRectangle(child.getParent(), child.getBounds(), root);
    }

    private static java.util.List<Component> descendants(Container root) {
        java.util.List<Component> all = new java.util.ArrayList<>();
        for (Component child : root.getComponents()) {
            all.add(child);
            if (child instanceof Container container) {
                all.addAll(descendants(container));
            }
        }
        return all;
    }
}
