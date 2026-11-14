package kitnapper.view;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BooleanSupplier;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

/**
 * 8 px edge/corner resize bands (design.md §11): invisible strips on the
 * frame's layered pane carry the resize cursors and drag math, and enforce
 * the frame minimum size. Disabled while maximized.
 */
final class ResizeBands {

    private static final int BAND = 8;

    private final JFrame frame;
    private final BooleanSupplier maximized;
    private final Point pressPoint = new Point();
    private final Rectangle pressBounds = new Rectangle();
    private Edge edge;

    ResizeBands(JFrame frame, BooleanSupplier maximized) {
        this.frame = frame;
        this.maximized = maximized;
    }

    void install() {
        JLayeredPane pane = frame.getLayeredPane();
        MouseAdapter band = bandAdapter();
        for (Side side : Side.values()) {
            JComponent strip = new BandStrip();
            strip.addMouseListener(band);
            strip.addMouseMotionListener(band);
            strip.putClientProperty("side", side);
            pane.add(strip, JLayeredPane.PALETTE_LAYER);
        }
        Component[] strips = pane.getComponentsInLayer(JLayeredPane.PALETTE_LAYER);
        pane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                layout(strips, pane);
            }
        });
        layout(strips, pane);
    }

    private void layout(Component[] strips, JLayeredPane pane) {
        int w = pane.getWidth();
        int h = pane.getHeight();
        for (Component strip : strips) {
            Side side = (Side) ((JComponent) strip).getClientProperty("side");
            switch (side) {
                case NORTH -> strip.setBounds(0, 0, w, BAND);
                case SOUTH -> strip.setBounds(0, h - BAND, w, BAND);
                case WEST -> strip.setBounds(0, BAND, BAND, h - 2 * BAND);
                case EAST -> strip.setBounds(w - BAND, BAND, BAND, h - 2 * BAND);
            }
        }
    }

    private MouseAdapter bandAdapter() {
        return new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                edge = edgeAt(e);
                ((Component) e.getSource()).setCursor(
                    edge == null ? Cursor.getDefaultCursor() : edge.cursor);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (maximized.getAsBoolean()) {
                    return;
                }
                if (edge != null) {
                    pressPoint.setLocation(e.getLocationOnScreen());
                    pressBounds.setBounds(frame.getBounds());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (maximized.getAsBoolean()) {
                    return;
                }
                if (edge != null) {
                    resizeTo(e.getLocationOnScreen());
                }
            }
        };
    }

    private Edge edgeAt(MouseEvent e) {
        JLayeredPane pane = frame.getLayeredPane();
        // Convert from the strip's coordinate space to the pane's, since
        // e.getX()/e.getY() are relative to the strip, not the pane.
        Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), pane);
        int x = p.x;
        int y = p.y;
        boolean n = y < BAND;
        boolean s = y >= pane.getHeight() - BAND;
        boolean w = x < BAND;
        boolean east = x >= pane.getWidth() - BAND;
        if (n && w) {
            return Edge.NW;
        }
        if (n && east) {
            return Edge.NE;
        }
        if (s && w) {
            return Edge.SW;
        }
        if (s && east) {
            return Edge.SE;
        }
        if (n) {
            return Edge.N;
        }
        if (s) {
            return Edge.S;
        }
        if (w) {
            return Edge.W;
        }
        if (east) {
            return Edge.E;
        }
        return null;
    }

    private void resizeTo(Point screen) {
        int dx = screen.x - pressPoint.x;
        int dy = screen.y - pressPoint.y;
        int minW = frame.getMinimumSize().width;
        int minH = frame.getMinimumSize().height;
        int x = pressBounds.x;
        int y = pressBounds.y;
        int w = pressBounds.width;
        int h = pressBounds.height;
        if (edge.west) {
            int shift = Math.min(dx, pressBounds.width - minW);
            x = pressBounds.x + shift;
            w = pressBounds.width - shift;
        }
        if (edge.east) {
            w = Math.max(minW, pressBounds.width + dx);
        }
        if (edge.north) {
            int shift = Math.min(dy, pressBounds.height - minH);
            y = pressBounds.y + shift;
            h = pressBounds.height - shift;
        }
        if (edge.south) {
            h = Math.max(minH, pressBounds.height + dy);
        }
        frame.setBounds(x, y, w, h);
    }

    private enum Side { NORTH, SOUTH, WEST, EAST }

    private enum Edge {
        N(false, false, true, false, Cursor.N_RESIZE_CURSOR),
        S(false, false, false, true, Cursor.S_RESIZE_CURSOR),
        W(true, false, false, false, Cursor.W_RESIZE_CURSOR),
        E(false, true, false, false, Cursor.E_RESIZE_CURSOR),
        NW(true, false, true, false, Cursor.NW_RESIZE_CURSOR),
        NE(false, true, true, false, Cursor.NE_RESIZE_CURSOR),
        SW(true, false, false, true, Cursor.SW_RESIZE_CURSOR),
        SE(false, true, false, true, Cursor.SE_RESIZE_CURSOR);

        final boolean west;
        final boolean east;
        final boolean north;
        final boolean south;
        final Cursor cursor;

        Edge(boolean west, boolean east, boolean north, boolean south, int cursorType) {
            this.west = west;
            this.east = east;
            this.north = north;
            this.south = south;
            this.cursor = Cursor.getPredefinedCursor(cursorType);
        }
    }

    /** Strip body: paints BG_APP so it blends into the window. */
    private static final class BandStrip extends JComponent {

        private static final long serialVersionUID = 1L;

        BandStrip() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Theme.bgApp);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
