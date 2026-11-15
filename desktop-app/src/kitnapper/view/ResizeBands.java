package kitnapper.view;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
 * the frame minimum size. Disabled while maximized. Edge geometry lives in
 * {@link ResizeEdge}.
 */
final class ResizeBands {

    private static final int BAND = 8;

    private final JFrame frame;
    private final BooleanSupplier maximized;
    private final Point pressPoint = new Point();
    private final Rectangle pressBounds = new Rectangle();
    private ResizeEdge edge;

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
        pane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
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

    private ResizeEdge edgeAt(MouseEvent e) {
        JLayeredPane pane = frame.getLayeredPane();
        // Convert from the strip's coordinate space to the pane's, since
        // e.getX()/e.getY() are relative to the strip, not the pane.
        Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), pane);
        return ResizeEdge.at(p.x, p.y, pane.getWidth(), pane.getHeight(), BAND);
    }

    private void resizeTo(Point screen) {
        Rectangle next = edge.dragTo(pressBounds,
            screen.x - pressPoint.x, screen.y - pressPoint.y,
            frame.getMinimumSize().width, frame.getMinimumSize().height);
        frame.setBounds(next);
    }

    private enum Side { NORTH, SOUTH, WEST, EAST }

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
