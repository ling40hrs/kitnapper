package kitnapper.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * Tree-walking, paint and tally helpers for {@link CatalogUiProbe} (plan §9.3).
 * The probe reads the rendered catalog through the component tree and
 * accessible names instead of production getters, matching the FrameProbe
 * convention; painting runs offscreen so it works under xvfb.
 */
final class UiProbeSupport {

    private static int passed;
    private static int failed;

    private UiProbeSupport() { }

    static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }

    static void finish() {
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** First descendant of {@code type}, depth-first; null when absent. */
    static <T extends Component> T find(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T nested = find(container, type);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    /** True when any label under {@code root} shows exactly {@code text}. */
    static boolean hasLabel(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label && text.equals(label.getText())) {
                return true;
            }
            if (child instanceof Container container && hasLabel(container, text)) {
                return true;
            }
        }
        return false;
    }

    /** True when any component under {@code root} carries this accessible name. */
    static boolean hasAccessibleName(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (child.getAccessibleContext() != null
                && name.equals(child.getAccessibleContext().getAccessibleName())) {
                return true;
            }
            if (child instanceof Container container && hasAccessibleName(container, name)) {
                return true;
            }
        }
        return false;
    }

    /** The first button with this label, visible or not; null when absent. */
    static JButton button(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (child instanceof Container container) {
                JButton nested = button(container, text);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    /**
     * Validates and paints once into a throwaway image: the first paint of a
     * freshly realized frame can be blank under xvfb while fonts resolve.
     * Must be called on the EDT (the probe body owns it).
     */
    static void settle(JFrame frame) {
        frame.validate();
        BufferedImage warm = new BufferedImage(Math.max(1, frame.getWidth()),
            Math.max(1, frame.getHeight()), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = warm.createGraphics();
        frame.paint(g);
        g.dispose();
        java.awt.Toolkit.getDefaultToolkit().sync();
    }

    /** The whole frame rendered offscreen at its current size. */
    static BufferedImage paint(JFrame frame) {
        BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(),
            BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        frame.paint(g);
        g.dispose();
        return image;
    }

    /** Sampled pixels that leave the corner color; 0 means a flat fill. */
    static int nonBlank(BufferedImage image) {
        int corner = image.getRGB(0, 0);
        int different = 0;
        for (int y = 0; y < image.getHeight(); y += 2) {
            for (int x = 0; x < image.getWidth(); x += 2) {
                if (image.getRGB(x, y) != corner) {
                    different++;
                }
            }
        }
        return different;
    }

    /** True when the first label with this text is realized and on screen. */
    static boolean labelShowing(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label && text.equals(label.getText())) {
                return label.isShowing();
            }
            if (child instanceof Container container && labelShowing(container, text)) {
                return true;
            }
        }
        return false;
    }
}
