package kitnapper.view;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * The §6 thin scrollbar (decision D38): an 8 px lane with a transparent
 * track, a 4 px rounded thumb that widens to the full lane while the pointer
 * rests on it, and no arrow buttons. A true zero-width overlay would need a
 * custom {@code JScrollPane}; the lane still occupies layout space, and that
 * deviation is recorded in design §6. Tokens are read at paint time, so a
 * theme switch needs no UI rebuild; {@code Theme.installDefaults()} installs
 * this class once through {@code UIManager}.
 */
public final class ThinScrollBarUI extends BasicScrollBarUI {

    static final int LANE = 8;
    static final int THUMB = 4;
    static final int THUMB_HOVER = 8;
    static final int RADIUS = 2;

    private final MouseAdapter hoverListener = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent event) {
            setHovered(true);
        }

        @Override
        public void mouseExited(MouseEvent event) {
            setHovered(false);
        }
    };
    private boolean hovered;

    /**
     * {@code UIManager} instantiates the UI reflectively and finds the static
     * {@code createUI} inherited from {@link BasicScrollBarUI}; without this
     * override that method would return a stock bar instead of this skin.
     */
    public static ComponentUI createUI(JComponent c) {
        return new ThinScrollBarUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        c.setOpaque(false);
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        scrollbar.addMouseListener(hoverListener);
    }

    @Override
    protected void uninstallListeners() {
        if (scrollbar != null) {
            scrollbar.removeMouseListener(hoverListener);
        }
        super.uninstallListeners();
    }

    /** Hover widens the thumb to the full lane (design §6); no animation. */
    private void setHovered(boolean value) {
        if (hovered == value) {
            return;
        }
        hovered = value;
        if (scrollbar != null) {
            scrollbar.repaint();
        }
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
        // Transparent by design: the surface behind the lane shows through.
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle bounds) {
        if (bounds.isEmpty()) {
            return;
        }
        boolean vertical = scrollbar.getOrientation() == JScrollBar.VERTICAL;
        int lane = Math.min(hovered ? THUMB_HOVER : THUMB,
            vertical ? bounds.width : bounds.height);
        if (lane <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.scrollThumb);
        g2.fill(thumbShape(bounds, lane, vertical));
        g2.dispose();
    }

    private static RoundRectangle2D thumbShape(Rectangle bounds, int lane, boolean vertical) {
        float arc = RADIUS * 2f;
        if (vertical) {
            float x = bounds.x + (bounds.width - lane) / 2f;
            return new RoundRectangle2D.Float(x, bounds.y, lane, bounds.height, arc, arc);
        }
        float y = bounds.y + (bounds.height - lane) / 2f;
        return new RoundRectangle2D.Float(bounds.x, y, bounds.width, lane, arc, arc);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return zeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return zeroButton();
    }

    private static JButton zeroButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        button.setFocusable(false);
        return button;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension size = super.getPreferredSize(c);
        if (((JScrollBar) c).getOrientation() == JScrollBar.VERTICAL) {
            return new Dimension(LANE, size.height);
        }
        return new Dimension(size.width, LANE);
    }
}
