package kitnapper.view;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import kitnapper.model.Equipment;

/**
 * The preview pane's 16:9 media slot (design §5, §12), extracted from
 * {@link PreviewPane} at M5 WP2 so the pane stays clear of the line cap. It
 * owns one {@link PhotoTile} at the 640 px decode target and swaps it when the
 * selection changes; nothing selected leaves the slot empty.
 */
final class PreviewMedia extends JPanel {

    private static final long serialVersionUID = 1L;

    private final int width;
    private final PhotoSource photos;
    private PhotoTile tile;

    /** A media slot {@code width} px wide; the tile keeps the 16:9 ratio. */
    PreviewMedia(int width, PhotoSource photos) {
        super(new BorderLayout());
        this.width = width;
        this.photos = photos == null ? PhotoSource.NONE : photos;
        setOpaque(false);
    }

    /** Shows the item's photo, skeleton, or fallback tile. */
    void show(Equipment item) {
        removeAll();
        tile = new PhotoTile(item, width, photos, PhotoTile.PREVIEW_EDGE_PX);
        add(tile, BorderLayout.CENTER);
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
