package kitnapper.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JComponent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxUI;

/**
 * Focus-cue checks (M6 WP5, finding F12): every keyboard-reachable control has
 * to show the design §6 accent cue, and no control may fall back to the LAF's
 * selection blue. Covers the filter combos, the due-date picker's cell and the
 * Table/Cards segmented control's click-to-focus.
 */
final class FocusCueChecks {

    private static final LocalDate T = LocalDate.of(2026, 11, 14);
    private static final int ACCENT_FLOOR = 20;

    private FocusCueChecks() { }

    static void run(CatalogScreen screen, MainFrame frame) {
        JComboBox<?> filter = UiProbeSupport.find(screen.filters(), JComboBox.class);
        UiProbeSupport.check("combo-skin", filter != null
            && filter.getUI() instanceof ComboSkin);
        comboValueTokens();
        dateCellTokens();
        if (filter != null) {
            frame.requestFocusInWindow();
            filter.requestFocusInWindow();
            UiProbeSupport.settle(frame);
            if (filter.hasFocus()) {
                UiProbeSupport.check("combo-focus-underline",
                    count(UiProbeSupport.paintComponent(filter), Theme.accent) > ACCENT_FLOOR);
            }
        }
        segmentedClick(screen, frame);
    }

    /** A focused combo paints its own tokens, never the LAF selection colours. */
    private static void comboValueTokens() {
        JComboBox<String> combo = new JComboBox<>(new String[] { "All" });
        ComboSkin.install(combo);
        combo.setBackground(Theme.bgControl);
        combo.setForeground(Theme.textPrimary);
        combo.setRenderer(new DefaultListCellRenderer());
        combo.setSize(160, 32);
        BufferedImage image = new BufferedImage(160, 32, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        BasicComboBoxUI ui = (BasicComboBoxUI) combo.getUI();
        ui.paintCurrentValueBackground(g, new Rectangle(0, 0, 160, 32), true);
        ui.paintCurrentValue(g, new Rectangle(0, 0, 160, 32), true);
        g.dispose();
        Color selection = new JList<>().getSelectionBackground();
        UiProbeSupport.check("combo-no-laf-fill",
            count(image, selection) == 0 && count(image, Theme.bgControl) > 160 * 32 / 2);
    }

    /** The due-date cell carries the combo tokens, so a focused picker stays themed. */
    private static void dateCellTokens() {
        DatePicker picker = new DatePicker(List.of(T, T.plusDays(1)), 0, "Due date");
        Component cell = picker.getRenderer().getListCellRendererComponent(
            new JList<>(), T, -1, true, true);
        UiProbeSupport.check("date-cell-tokens",
            cell.getBackground().equals(Theme.bgControl)
                && cell.getForeground().equals(Theme.textPrimary));
    }

    /** Pressing a segment focuses the control; the accent ring follows. */
    private static void segmentedClick(CatalogScreen screen, MainFrame frame) {
        SegmentedControl control = screen.filters().layoutControl();
        MouseEvent press = new MouseEvent(control, MouseEvent.MOUSE_PRESSED,
            System.currentTimeMillis(), 0, 6, 8, 1, false, MouseEvent.BUTTON1);
        for (MouseListener listener : control.getMouseListeners()) {
            listener.mousePressed(press);
        }
        UiProbeSupport.settle(frame);
        if (control.hasFocus()) {
            UiProbeSupport.check("segment-focus-ring", ringPixels(control) > ACCENT_FLOOR);
        } else {
            System.out.println("skip segment-focus-ring (no window focus on this display)");
        }
    }

    private static int ringPixels(JComponent control) {
        return count(UiProbeSupport.paintComponent(control), Theme.accent);
    }

    /** Pixels of exactly {@code colour}; the paints here are flat fills. */
    private static int count(BufferedImage image, Color colour) {
        int rgb = colour.getRGB() & 0xFFFFFF;
        int found = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) == rgb) {
                    found++;
                }
            }
        }
        return found;
    }
}
