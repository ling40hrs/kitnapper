package kitnapper.view;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.IntConsumer;

import javax.swing.JPanel;

/**
 * The §6 segmented control: 32 px, radius 4 container in {@code BG_CONTROL},
 * selected segment {@code BG_ELEVATED} + 1 px {@code BORDER_STRONG}, 2 px gap,
 * flat hover. Mouse, arrow keys and Space select; the §6 focus rectangle stays
 * visible on keyboard focus (design §8).
 */
final class SegmentedControl extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int HEIGHT = 32;
    private static final int INSET = 2;
    private static final int GAP = 2;
    private static final int SEGMENT_PAD = 12;
    private static final int RADIUS = 4;

    private final String[] labels;
    private int selected;
    private int hovered = -1;
    private IntConsumer onChange = index -> { };

    SegmentedControl(String... labels) {
        if (labels.length < 2) {
            throw new IllegalArgumentException("a segmented control needs 2+ segments");
        }
        this.labels = labels.clone();
        setOpaque(false);
        setFocusable(true);
        setFont(Fonts.text(12, Font.PLAIN));
        getAccessibleContext().setAccessibleName("View layout");
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                choose(indexAt(e.getX()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = -1;
                repaint();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int next = indexAt(e.getX());
                if (next != hovered) {
                    hovered = next;
                    repaint();
                }
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_UP) {
                    choose(Math.max(0, selected - 1));
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT
                    || e.getKeyCode() == KeyEvent.VK_DOWN) {
                    choose(Math.min(labels.length - 1, selected + 1));
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    choose(selected);
                }
            }
        });
    }

    int getSelectedIndex() {
        return selected;
    }

    void setSelectedIndex(int index) {
        if (index < 0 || index >= labels.length) {
            throw new IllegalArgumentException("segment index " + index + " is out of range");
        }
        if (selected != index) {
            selected = index;
            repaint();
            onChange.accept(index);
        }
    }

    void addChangeListener(IntConsumer listener) {
        onChange = listener;
    }

    private void choose(int index) {
        if (index >= 0 && index < labels.length && index != selected) {
            setSelectedIndex(index);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrics = getFontMetrics(getFont());
        int width = INSET * 2 + GAP * (labels.length - 1);
        for (String label : labels) {
            width += metrics.stringWidth(label) + SEGMENT_PAD * 2;
        }
        return new Dimension(width, HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    /** Segment geometry; shared by hit testing and painting so they cannot drift. */
    private RoundRectangle2D[] boxes() {
        int span = getWidth() - INSET * 2 - GAP * (labels.length - 1);
        int width = span / labels.length;
        RoundRectangle2D[] boxes = new RoundRectangle2D[labels.length];
        int x = INSET;
        for (int i = 0; i < labels.length; i++) {
            int w = i == labels.length - 1 ? span - width * (labels.length - 1) : width;
            boxes[i] = new RoundRectangle2D.Double(x, INSET, w, getHeight() - INSET * 2 - 1,
                RADIUS, RADIUS);
            x += w + GAP;
        }
        return boxes;
    }

    private int indexAt(int x) {
        RoundRectangle2D[] boxes = boxes();
        for (int i = 0; i < boxes.length; i++) {
            if (x >= boxes[i].getX() && x <= boxes[i].getX() + boxes[i].getWidth()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Theme.bgControl);
        g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1,
            RADIUS, RADIUS));
        RoundRectangle2D[] boxes = boxes();
        for (int i = 0; i < labels.length; i++) {
            RoundRectangle2D box = boxes[i];
            if (i == selected) {
                g2.setColor(Theme.bgElevated);
                g2.fill(box);
                g2.setColor(Theme.borderStrong);
                g2.draw(box);
            } else if (i == hovered) {
                g2.setColor(Theme.bgControlHover);
                g2.fill(box);
            }
            g2.setFont(getFont());
            g2.setColor(i == selected ? Theme.textPrimary : Theme.textSecondary);
            FontMetrics metrics = g2.getFontMetrics();
            int textX = (int) box.getX() + ((int) box.getWidth()
                - metrics.stringWidth(labels[i])) / 2;
            int textY = (int) box.getY() + ((int) box.getHeight() - metrics.getHeight()) / 2
                + metrics.getAscent();
            g2.drawString(labels[i], textX, textY);
        }
        if (isFocusOwner()) {
            g2.setColor(Theme.accent);
            g2.setStroke(new BasicStroke(2));
            g2.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 3, getHeight() - 3,
                RADIUS, RADIUS));
        }
        g2.dispose();
    }
}
