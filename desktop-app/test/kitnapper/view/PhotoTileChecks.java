package kitnapper.view;

import static kitnapper.view.PhotoProbeSupport.find;
import static kitnapper.view.PhotoProbeSupport.magentaCount;
import static kitnapper.view.PhotoProbeSupport.onEdt;
import static kitnapper.view.PhotoProbeSupport.onEdtRun;
import static kitnapper.view.PhotoProbeSupport.paintOf;
import static kitnapper.view.PhotoProbeSupport.waitFor;

import java.awt.GridLayout;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Photo-tile and skeleton checks (plan §9.3, design §12–§13): the fallback
 * when there is no photo, a fast decode that shows no skeleton, a slow decode
 * that does, the failure path, the cache, the reduced-motion switch, and the
 * pane/cards wiring. Every read runs on the EDT.
 */
final class PhotoTileChecks {

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
        UiProbeSupport.check("the cache clears", onEdt(() -> {
            PhotoCache.clear();
            return PhotoCache.size() == 0;
        }));

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
    }

    /** Tears the fixture down; must run on the EDT. */
    void dispose() {
        if (frame != null) {
            frame.dispose();
        }
    }
}
