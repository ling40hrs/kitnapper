package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Optional;

import javax.swing.JPanel;

import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;

/**
 * The §12 media block: 16:9, 8 px radius, an uploaded photo cover-scaled into
 * it, or the category fallback tile when there is none. Decoding happens off
 * the EDT behind {@link SkeletonGate}; the memo in {@link PhotoCache} means a
 * second view of the same file paints with no loading state at all. A missing,
 * corrupt, or timed-out photo is the fallback tile, never an error (NFR-08).
 */
final class PhotoTile extends JPanel implements SkeletonGate.Slot {

    private static final long serialVersionUID = 1L;

    /** Card thumbnail decode target (design §12). */
    static final int CARD_EDGE_PX = 160;

    /** Preview-pane and dialog-well decode target (design §12). */
    static final int PREVIEW_EDGE_PX = 640;

    private static final int RADIUS = 8;

    private final PhotoSource source;
    private final int edge;
    private final int width;
    private EquipmentCategory category;
    private String photoFile;
    private BufferedImage image;
    private Runnable cancel = () -> { };

    /** A 16:9 tile {@code width} px wide; height follows the aspect ratio. */
    PhotoTile(Equipment item, int width, PhotoSource source, int edge) {
        this.source = source == null ? PhotoSource.NONE : source;
        this.edge = edge;
        this.width = width;
        setOpaque(false);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(width, Math.max(1, Math.round(width * 9f / 16f))));
        load(item);
    }

    @Override
    public void showSkeleton(SkeletonPanel skeleton) {
        removeAll();
        add(skeleton, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    @Override
    public void showResult(Optional<BufferedImage> result) {
        removeAll();
        if (result != null && result.isPresent()) {
            image = result.get();
            PhotoCache.put(photoFile, edge, image);
        } else {
            image = null;
            add(new GradientTile(category, width), BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    /** Re-applies token colors after a Theme switch. */
    void refreshTheme() {
        repaint();
    }

    @Override
    public void removeNotify() {
        cancel.run();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (image == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int boxWidth = getWidth();
        int boxHeight = getHeight();
        g2.setClip(new RoundRectangle2D.Double(0, 0, boxWidth, boxHeight, RADIUS, RADIUS));
        double scale = Math.max(boxWidth / (double) image.getWidth(),
            boxHeight / (double) image.getHeight());
        int drawWidth = (int) Math.round(image.getWidth() * scale);
        int drawHeight = (int) Math.round(image.getHeight() * scale);
        g2.drawImage(image, (boxWidth - drawWidth) / 2, (boxHeight - drawHeight) / 2,
            drawWidth, drawHeight, null);
        g2.dispose();
    }

    private void load(Equipment item) {
        if (item == null) {
            return;
        }
        category = item.getCategory();
        getAccessibleContext().setAccessibleName("Photo of " + item.getName());
        photoFile = item.getPhotoFile().orElse(null);
        if (photoFile == null) {
            showResult(Optional.empty());
            return;
        }
        BufferedImage cached = PhotoCache.get(photoFile, edge);
        if (cached != null) {
            image = cached;
            return;
        }
        cancel = SkeletonGate.run(this, this,
            new SkeletonPanel(SkeletonPanel.Variant.MEDIA),
            () -> source.load(photoFile, edge));
    }
}
