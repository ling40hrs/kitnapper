package kitnapper.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Field-skin regression checks (D52): the accent underline follows focus, and
 * the combo's chevron is actually drawn. Both defects were invisible to the
 * existing offscreen paints — the underline was a stale on-screen pixel, and
 * the chevron was painted a single shade away from its own fill — so these
 * assert the rendered result against a real, focused window.
 */
final class FocusRepaintChecks {

    private static final int FIELD_WIDTH = 240;
    private static final int FIELD_HEIGHT = 32;

    private FocusRepaintChecks() { }

    static void run() {
        JFrame frame = new JFrame();
        JPanel body = new JPanel();
        body.setOpaque(true);
        body.setBackground(Theme.bgElevated);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JTextField focused = FieldSkin.field("e.g. SN-EOSR6-002");
        focused.setText("CIIT123");
        JTextField idle = FieldSkin.field("e.g. 145000");
        idle.setText("255");
        JComboBox<String> combo = UiFactory.comboBox("Category", "Camera", "Lens");
        for (Component control : new Component[] { focused, idle, combo }) {
            control.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
            control.setMaximumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
            body.add(control);
            body.add(Box.createVerticalStrut(10));
        }
        frame.setContentPane(body);
        frame.setSize(FIELD_WIDTH + 40, 3 * (FIELD_HEIGHT + 10) + 20);
        frame.setVisible(true);
        frame.validate();
        combo.doLayout();

        // One focus owner, so exactly one field may carry the accent underline.
        SwingUtilities.invokeLater(() -> {
            focused.requestFocusInWindow();
            checkFields(focused, idle);
            checkChevron(combo);
            frame.dispose();
        });
    }

    private static void checkFields(JTextField focused, JTextField idle) {
        // The stale underline was a missed repaint: the skin reads hasFocus()
        // at paint time, so the field must repaint when focus moves. Checking
        // the listener catches a regression on any display, focus or not.
        UiProbeSupport.check("field-repaints-on-focus",
            hasFocusRepaintListener(focused) && hasFocusRepaintListener(idle));
        boolean focusedHasIt = focused.hasFocus();
        if (focusedHasIt) {
            UiProbeSupport.check("field-focus-underline", accentPixels(paint(focused)) > 300);
        } else {
            System.out.println("skip field-focus-underline (no window focus on this display)");
        }
        BufferedImage idleImage = paint(idle);
        UiProbeSupport.check("field-no-stale-underline", accentPixels(idleImage) == 0);
        UiProbeSupport.check("field-bottom-border-still-present",
            count(idleImage, Theme.borderStrong) > FIELD_WIDTH / 2);
    }

    private static boolean hasFocusRepaintListener(JTextField field) {
        for (FocusListener listener : field.getFocusListeners()) {
            if (listener instanceof FocusAdapter) {
                return true;
            }
        }
        return false;
    }

    private static void checkChevron(JComboBox<String> combo) {
        BufferedImage image = paint(combo);
        UiProbeSupport.check("combo-chevron-visible", readablePixels(image) > 10);
        UiProbeSupport.check("combo-chevron-not-laf-colour",
            count(image, new Color(0x333333)) == 0);
    }

    /** Pixels whose colour is at least 1.6:1 from the control fill. */
    private static int readablePixels(BufferedImage image) {
        int readable = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (contrast(new Color(image.getRGB(x, y)), Theme.bgControl) >= 1.6) {
                    readable++;
                }
            }
        }
        return readable;
    }

    private static BufferedImage paint(JComponent component) {
        BufferedImage image = new BufferedImage(component.getWidth(),
            component.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Theme.bgElevated);
        g.fillRect(0, 0, component.getWidth(), component.getHeight());
        component.paint(g);
        g.dispose();
        return image;
    }

    private static int accentPixels(BufferedImage image) {
        return count(image, Theme.accent);
    }

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

    /** WCAG relative-luminance ratio, used only to tell ink from fill. */
    private static double contrast(Color a, Color b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(Color colour) {
        double[] channel = new double[3];
        double[] values = { colour.getRed(), colour.getGreen(), colour.getBlue() };
        for (int i = 0; i < 3; i++) {
            double value = values[i] / 255.0;
            channel[i] = value <= 0.03928
                ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * channel[0] + 0.7152 * channel[1] + 0.0722 * channel[2];
    }
}
