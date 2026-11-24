package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

/**
 * The borderless scroll guard shared by the four preview panes (design §5,
 * decision D42). A plain {@code JPanel} view is not {@link Scrollable}, so
 * {@code ViewportLayout} stretches it to the viewport's height; a
 * {@code BoxLayout} column then hands that leftover height to its tallest
 * child, which stretched the 16:9 media block past the pane and clipped it.
 * Tracking the viewport's width but never its height keeps the column at its
 * preferred height: nothing scrolls at 1280×800 and wider, and the metadata
 * rows stay reachable at the 1024×640 minimum.
 */
final class PreviewScroll extends JPanel implements Scrollable {

    private static final long serialVersionUID = 1L;

    private static final int UNIT_INCREMENT = 16;

    private PreviewScroll(JComponent content) {
        super(new BorderLayout());
        setOpaque(false);
        add(content, BorderLayout.CENTER);
    }

    /** A vertical-only scroll pane around {@code content}, with no border. */
    static JScrollPane wrap(JComponent content) {
        JScrollPane pane = new JScrollPane(new PreviewScroll(content),
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        pane.getVerticalScrollBar().setUnitIncrement(UNIT_INCREMENT);
        return pane;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return UNIT_INCREMENT;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(UNIT_INCREMENT, visible.height);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
