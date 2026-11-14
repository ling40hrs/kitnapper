package kitnapper.view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

/**
 * Shared surface card (design.md §6): flat fill, 8 px radius, 1 px
 * BORDER_SUBTLE outline — no gradient, no shadow. Used by the login card
 * (surface) and the dialog shell (elevated).
 */
final class RoundedPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final boolean elevated;
    private final int radius;

    RoundedPanel(boolean elevated, int radius) {
        this.elevated = elevated;
        this.radius = radius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(elevated ? Theme.bgElevated : Theme.bgSurface);
        int w = getWidth();
        int h = getHeight();
        g2.fill(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, radius, radius));
        g2.setColor(Theme.borderSubtle);
        g2.draw(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, radius, radius));
        g2.dispose();
        super.paintComponent(g);
    }
}
