package kitnapper.view;

import java.awt.Dimension;

import javax.swing.JPanel;

import kitnapper.model.Equipment;

/**
 * The preview pane's 16:9 media slot (design §5, §12), extracted from
 * {@link PreviewPane} at M5 WP2 so the pane stays clear of the line cap. It
 * owns one {@link PhotoTile} at the 640 px decode target and swaps it when the
 * selection changes; nothing selected leaves the slot empty.
 *
 * <p>{@link MediaRatioLayout} gives the slot its real width and the 16:9 height
 * that width implies: the info column narrows when the pane grows a vertical
 * scrollbar, and a fixed-height tile would be cropped at the right and bottom.
 * The slot is left-aligned and capped so the {@code BoxLayout} column cannot
 * stretch it past its row either.</p>
 */
final class PreviewMedia extends JPanel {

    private static final long serialVersionUID = 1L;

    private final int width;
    private final PhotoSource photos;
    private PhotoTile tile;

    /** A media slot {@code width} px wide; the tile keeps the 16:9 ratio. */
    PreviewMedia(int width, PhotoSource photos) {
        super(new MediaRatioLayout());
        this.width = width;
        this.photos = photos == null ? PhotoSource.NONE : photos;
        setOpaque(false);
        setAlignmentX(LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(width, MediaRatioLayout.heightFor(width)));
    }

    /** Shows the item's photo, skeleton, or fallback tile. */
    void show(Equipment item) {
        removeAll();
        tile = new PhotoTile(item, width, photos, PhotoTile.PREVIEW_EDGE_PX);
        add(tile);
        revalidate();
        repaint();
    }

    /** Nothing selected: drop the tile and its pending decode. */
    void clear() {
        removeAll();
        tile = null;
        revalidate();
        repaint();
    }

    void refreshTheme() {
        if (tile != null) {
            tile.refreshTheme();
        }
        repaint();
    }
}
