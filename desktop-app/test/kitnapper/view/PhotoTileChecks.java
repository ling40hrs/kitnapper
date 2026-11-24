package kitnapper.view;

import static kitnapper.view.PhotoProbeSupport.find;
import static kitnapper.view.PhotoProbeSupport.magentaCount;
import static kitnapper.view.PhotoProbeSupport.onEdt;
import static kitnapper.view.PhotoProbeSupport.onEdtRun;
import static kitnapper.view.PhotoProbeSupport.paintOf;
import static kitnapper.view.PhotoProbeSupport.waitFor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import kitnapper.model.EquipmentCategory;

/**
 * Photo-tile and skeleton checks (plan §9.3, design §12–§13): the fallback
 * when there is no photo, a fast decode that shows no skeleton, a slow decode
 * that does, the failure path, the cache, the reduced-motion switch, and the
 * pane/cards wiring. Every read runs on the EDT.
 */
final class PhotoTileChecks {

    private static final int CLIP_WIDTH = 320;
    private static final int CLIP_SENTINEL = 0x00FF00;

    private JFrame frame;
    private PhotoTile fastTile;
    private PhotoTile missingTile;
    private PhotoTile slowTile;
    private PhotoTile failingTile;
    private SkeletonPanel skeleton;

    /** Builds the fixture; must run on the EDT. */
    void build() {
        Theme.apply(true);
        Theme.animateSkeletons = true;
        PhotoCache.clear();
        frame = new JFrame("photo probe");
        frame.setUndecorated(true);
        JPanel host = new JPanel(new GridLayout(1, 5, 8, 8));
        fastTile = PhotoProbeSupport.tile("Fast", "e-011.jpg", PhotoProbeSupport.photoSource());
        missingTile = PhotoProbeSupport.tile("Missing", null, PhotoSource.NONE);
        slowTile = PhotoProbeSupport.tile("Slow", "e-012.jpg", PhotoProbeSupport.slowSource(400));
        failingTile = PhotoProbeSupport.tile("Failing", "e-013.jpg",
            PhotoProbeSupport.failingSource());
        skeleton = new SkeletonPanel(SkeletonPanel.Variant.CARD);
        host.add(fastTile);
        host.add(missingTile);
        host.add(slowTile);
        host.add(failingTile);
        host.add(skeleton);
        frame.setContentPane(host);
        frame.setSize(1200, 320);
        frame.setVisible(true);
        frame.validate();
    }

    /** Runs the checks; safe to call off the EDT. */
    void run() {
        UiProbeSupport.check("delay is 150 ms", SkeletonGate.DELAY_MS == 150);
        UiProbeSupport.check("minimum visible is 300 ms", SkeletonGate.MIN_VISIBLE_MS == 300);
        UiProbeSupport.check("fade is 167 ms", SkeletonGate.FADE_MS == 167);
        UiProbeSupport.check("watchdog is 10 s", SkeletonGate.WATCHDOG_MS == 10_000);

        UiProbeSupport.check("no photo falls back immediately",
            onEdt(() -> find(missingTile, GradientTile.class) != null));
        UiProbeSupport.check("no photo never shows a skeleton",
            onEdt(() -> find(missingTile, SkeletonPanel.class) == null));

        waitFor("a fast decode paints the photo", 3000, () -> magentaCount(fastTile) > 100);
        UiProbeSupport.check("a fast decode shows no skeleton",
            onEdt(() -> find(fastTile, SkeletonPanel.class) == null));

        waitFor("a slow decode shows a skeleton", 1500,
            () -> find(slowTile, SkeletonPanel.class) != null);
        UiProbeSupport.check("the skeleton paints blocks",
            onEdt(() -> UiProbeSupport.nonBlank(paintOf(slowTile)) > 50));
        waitFor("the slow decode resolves to the photo", 4000,
            () -> find(slowTile, SkeletonPanel.class) == null && magentaCount(slowTile) > 100);

        waitFor("a failed decode falls back", 3000,
            () -> find(failingTile, GradientTile.class) != null);
        UiProbeSupport.check("a failed decode leaves no skeleton",
            onEdt(() -> find(failingTile, SkeletonPanel.class) == null));

        UiProbeSupport.check("decoded photos are cached",
            onEdt(() -> PhotoCache.get("e-011.jpg", PhotoTile.CARD_EDGE_PX) != null));
        // D54: the card media is 200 logical px and the tile must stay sharp at
        // uiScale 1.5, so the decode target carries at least 300 device pixels.
        UiProbeSupport.check("the card decode target carries 1.5x device pixels",
            PhotoTile.CARD_EDGE_PX >= 300);
        UiProbeSupport.check("the preview target stays above the card target",
            PhotoTile.PREVIEW_EDGE_PX > PhotoTile.CARD_EDGE_PX);
        UiProbeSupport.check("the cache clears", onEdt(() -> {
            PhotoCache.clear();
            return PhotoCache.size() == 0;
        }));

        inheritedClip();

        UiProbeSupport.check("the pulse runs while showing", onEdt(skeleton::pulseRunning));
        onEdtRun(() -> Theme.animateSkeletons = false);
        waitFor("reduced motion stops the pulse", 1000, () -> !skeleton.pulseRunning());
        onEdtRun(() -> Theme.animateSkeletons = true);

        UiProbeSupport.check("the preview pane wires a photo tile", onEdt(() -> {
            PreviewPane pane = new PreviewPane(item -> { }, PhotoProbeSupport.photoSource());
            pane.show(PhotoProbeSupport.item("Wired", "e-020.jpg"), false, false, null);
            return find(pane, PhotoTile.class) != null;
        }));
        UiProbeSupport.check("the cards layout wires a photo tile", onEdt(() -> {
            CatalogCards cards = new CatalogCards(PhotoProbeSupport.photoSource());
            cards.setItems(List.of(PhotoProbeSupport.item("Wired", "e-020.jpg")));
            return find(cards, PhotoTile.class) != null && cards.cardCount() == 1;
        }));

        AtomicBoolean faded = new AtomicBoolean();
        onEdtRun(() -> skeleton.fadeOut(0, () -> faded.set(true)));
        UiProbeSupport.check("a zero-length fade completes at once", faded.get());

        SkeletonAuditChecks.run();
    }

    /**
     * D52: a tile must never paint outside the clip Swing hands down. Replacing
     * the clip (rather than intersecting it) lets a partly scrolled tile paint
     * over the header above the scroll pane.
     */
    private void inheritedClip() {
        onEdtRun(() -> {
            PhotoCache.put("clip.jpg", PhotoTile.CARD_EDGE_PX, PhotoProbeSupport.photo());
            PhotoTile photo = new PhotoTile(PhotoProbeSupport.item("Clip", "clip.jpg"),
                CLIP_WIDTH, PhotoProbeSupport.photoSource(), PhotoTile.CARD_EDGE_PX);
            GradientTile fallback = new GradientTile(EquipmentCategory.OTHER, CLIP_WIDTH);
            UiProbeSupport.check("a photo tile honours the inherited clip",
                paintsOutsideClip(photo) == 0);
            UiProbeSupport.check("a fallback tile honours the inherited clip",
                paintsOutsideClip(fallback) == 0);
        });
    }

    /** Paints under a bottom-half clip; counts pixels landing above it. */
    private static int paintsOutsideClip(JComponent tile) {
        int height = MediaRatioLayout.heightFor(CLIP_WIDTH);
        tile.setSize(CLIP_WIDTH, height);
        BufferedImage image = new BufferedImage(CLIP_WIDTH, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(new Color(CLIP_SENTINEL));
        g2.fillRect(0, 0, CLIP_WIDTH, height);
        g2.setClip(0, height / 2, CLIP_WIDTH, height - height / 2);
        tile.paint(g2);
        g2.dispose();
        int outside = 0;
        for (int y = 0; y < height / 2; y++) {
            for (int x = 0; x < CLIP_WIDTH; x++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) != CLIP_SENTINEL) {
                    outside++;
                }
            }
        }
        return outside;
    }

    /** Tears the fixture down; must run on the EDT. */
    void dispose() {
        if (frame != null) {
            frame.dispose();
        }
    }
}
