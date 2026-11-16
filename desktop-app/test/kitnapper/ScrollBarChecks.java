package kitnapper;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JScrollBar;

import kitnapper.view.Theme;
import kitnapper.view.ThinScrollBarUI;

/**
 * Scrollbar section of the skin probe (plan §8.2, decision D38): the 8 px
 * lane, the arrow-less thumb, the 4 → 8 px hover widening and the paint-time
 * theme tokens. Headless — the bar is painted into an image, never shown, so
 * the lane is measured from pixels rather than from layout.
 */
final class ScrollBarChecks {

    private static final int LANE = 8;
    private static final int THUMB = 4;
    private static final int BAR_LENGTH = 100;

    private ScrollBarChecks() {
    }

    /** Runs every scrollbar check in order; the map keeps the report stable. */
    static Map<String, Boolean> results() {
        Map<String, Boolean> results = new LinkedHashMap<>();
        Theme.apply(true);
        JScrollBar vertical = bar(JScrollBar.VERTICAL);
        JScrollBar horizontal = bar(JScrollBar.HORIZONTAL);
        results.put("scrollbar-ui-installed",
            vertical.getUI() instanceof ThinScrollBarUI
                && horizontal.getUI() instanceof ThinScrollBarUI);
        results.put("scrollbar-lane",
            vertical.getPreferredSize().width == LANE
                && horizontal.getPreferredSize().height == LANE);
        results.put("scrollbar-no-arrows", noArrows(vertical) && noArrows(horizontal));
        results.put("scrollbar-thumb-paints",
            thumbColumns(vertical) == THUMB && trackTransparent(vertical));
        results.put("scrollbar-hover-widens",
            hoverColumns(vertical) == LANE && thumbColumns(vertical) == THUMB);
        int darkThumb = Theme.scrollThumb.getRGB();
        Theme.apply(false);
        results.put("scrollbar-light-theme",
            Theme.scrollThumb.getRGB() != darkThumb && thumbColumns(vertical) == THUMB);
        return results;
    }

    private static JScrollBar bar(int orientation) {
        JScrollBar bar = new JScrollBar(orientation);
        if (orientation == JScrollBar.VERTICAL) {
            bar.setSize(LANE, BAR_LENGTH);
        } else {
            bar.setSize(BAR_LENGTH, LANE);
        }
        bar.doLayout();
        return bar;
    }

    private static boolean noArrows(JScrollBar bar) {
        int buttons = 0;
        for (Component child : bar.getComponents()) {
            if (child instanceof JButton) {
                buttons++;
                if (child.isFocusable()
                        || !child.getPreferredSize().equals(new Dimension(0, 0))) {
                    return false;
                }
            }
        }
        return buttons == 2;
    }

    /** Enters the bar, reads the widened thumb, then leaves it again. */
    private static int hoverColumns(JScrollBar bar) {
        dispatch(bar, MouseEvent.MOUSE_ENTERED);
        int columns = thumbColumns(bar);
        dispatch(bar, MouseEvent.MOUSE_EXITED);
        return columns;
    }

    private static void dispatch(JScrollBar bar, int id) {
        bar.dispatchEvent(new MouseEvent(bar, id, System.currentTimeMillis(), 0, 1, 1, 0, false));
    }

    /** Columns holding at least one exact {@code Theme.scrollThumb} pixel. */
    private static int thumbColumns(JScrollBar bar) {
        BufferedImage image = paint(bar);
        int columns = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if (image.getRGB(x, y) == Theme.scrollThumb.getRGB()) {
                    columns++;
                    break;
                }
            }
        }
        return columns;
    }

    /** The lane edge keeps the surface fill: the track paints nothing. */
    private static boolean trackTransparent(JScrollBar bar) {
        BufferedImage image = paint(bar);
        return new Color(image.getRGB(0, image.getHeight() / 2)).equals(Theme.bgSurface);
    }

    private static BufferedImage paint(JScrollBar bar) {
        BufferedImage image = new BufferedImage(bar.getWidth(), bar.getHeight(),
            BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Theme.bgSurface);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        bar.printAll(g);
        g.dispose();
        return image;
    }
}
