package kitnapper.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.border.AbstractBorder;

/**
 * Undecorated-frame behavior (design.md §11, D9): root border, minimum
 * size, drag region, and manual maximize with restore bounds (sized to the
 * desktop work area so the taskbar stays visible). Resize bands live in
 * {@link ResizeBands}.
 */
public final class WindowChrome {

    private final JFrame frame;
    private final Rectangle restoreBounds = new Rectangle();
    private boolean maximized;
    private Runnable onMaximizeChange;

    private WindowChrome(JFrame frame) {
        this.frame = frame;
    }

    /** Makes the frame undecorated, installs the border and resize bands. */
    public static WindowChrome install(JFrame frame) {
        frame.setUndecorated(true);
        frame.setMinimumSize(new Dimension(1024, 640));
        frame.getRootPane().setBorder(rootBorder());
        new ResizeBands(frame).install();
        return new WindowChrome(frame);
    }

    private static AbstractBorder rootBorder() {
        return new AbstractBorder() {
            private static final long serialVersionUID = 1L;

            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                g.setColor(Theme.borderSubtle);
                g.drawRect(x, y, w - 1, h - 1);
            }

            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(1, 1, 1, 1);
            }

            @Override
            public boolean isBorderOpaque() {
                return true;
            }
        };
    }

    /** Registers a component (typically the title bar) as the drag region. */
    public void attachDragRegion(JComponent region) {
        MouseAdapter drag = new MouseAdapter() {
            private final Point offset = new Point();

            @Override
            public void mousePressed(MouseEvent e) {
                offset.setLocation(e.getLocationOnScreen().x - frame.getX(),
                    e.getLocationOnScreen().y - frame.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!maximized) {
                    frame.setLocation(e.getLocationOnScreen().x - offset.x,
                        e.getLocationOnScreen().y - offset.y);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    toggleMaximize();
                }
            }
        };
        region.addMouseListener(drag);
        region.addMouseMotionListener(drag);
    }

    /** Manual maximize: desktop work area, restore bounds kept. */
    public void toggleMaximize() {
        if (maximized) {
            frame.setBounds(restoreBounds);
            maximized = false;
        } else {
            restoreBounds.setBounds(frame.getBounds());
            frame.setBounds(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds());
            maximized = true;
        }
        if (onMaximizeChange != null) {
            onMaximizeChange.run();
        }
    }

    public void iconify() {
        frame.setExtendedState(java.awt.Frame.ICONIFIED);
    }

    public void close() {
        frame.dispose();
    }

    public boolean isMaximized() {
        return maximized;
    }

    public void onMaximizeChange(Runnable callback) {
        onMaximizeChange = callback;
    }
}
