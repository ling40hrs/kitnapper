package kitnapper.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * The §12 media ratio: lays its children out at the full available width and
 * the 16:9 height that width implies. The preview panes' info column can
 * narrow (a vertical scrollbar takes 10 px at the minimum window size), and a
 * tile with a fixed preferred height would stretch to a non-16:9 shape — a
 * cover-scaled photo would then be cropped and the fallback glyph off-center.
 * Deriving the height from the real width keeps the block 16:9 at every size.
 */
final class MediaRatioLayout implements LayoutManager {

    private static final int WIDTH = 16;
    private static final int HEIGHT = 9;

    /** The 16:9 height for {@code width} px, never below one pixel. */
    static int heightFor(int width) {
        return Math.max(1, Math.round(width * (float) HEIGHT / WIDTH));
    }

    @Override
    public void addLayoutComponent(String name, Component component) {
        // No per-child constraints: every child is the media block itself.
    }

    @Override
    public void removeLayoutComponent(Component component) {
        // Nothing to release.
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        int width = 0;
        for (Component child : parent.getComponents()) {
            width = Math.max(width, child.getPreferredSize().width);
        }
        Insets insets = parent.getInsets();
        return new Dimension(width + insets.left + insets.right,
            heightFor(width) + insets.top + insets.bottom);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        Insets insets = parent.getInsets();
        Rectangle bounds = new Rectangle(insets.left, insets.top,
            Math.max(0, parent.getWidth() - insets.left - insets.right),
            Math.max(0, parent.getHeight() - insets.top - insets.bottom));
        int height = Math.min(bounds.height, heightFor(bounds.width));
        for (Component child : parent.getComponents()) {
            child.setBounds(bounds.x, bounds.y, bounds.width, height);
        }
    }
}
