package kitnapper.view;

import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Skeleton audit (design §13, plan §7 WP4): the §13 promises the photo-tile
 * checks do not cover — skeletons never take focus, the pulse stops when the
 * component is hidden, the accessible loading name is announced once and
 * cleared once, and the 10 s watchdog replaces a stuck skeleton instead of
 * letting it outlive its work. Needs a display; every read runs on the EDT.
 */
final class SkeletonAuditChecks {

    private static final int SIZE_W = 320;
    private static final int SIZE_H = 220;

    private static JFrame frame;
    private static JPanel host;

    private SkeletonAuditChecks() { }

    /** Runs the whole audit; safe to call off the EDT. */
    static void run() {
        PhotoProbeSupport.onEdtRun(SkeletonAuditChecks::build);
        focus();
        pulseStopsWhenHidden();
        accessibleName();
        watchdog();
        PhotoProbeSupport.onEdtRun(() -> {
            Theme.animateSkeletons = true;
            frame.dispose();
        });
    }

    private static void build() {
        Theme.apply(true);
        Theme.animateSkeletons = true;
        frame = new JFrame("skeleton audit");
        frame.setUndecorated(true);
        host = new JPanel();
        frame.setContentPane(host);
        frame.setSize(SIZE_W, SIZE_H);
        frame.setVisible(true);
        frame.validate();
    }

    private static void focus() {
        SkeletonPanel panel = new SkeletonPanel(SkeletonPanel.Variant.CARD);
        PhotoProbeSupport.onEdtRun(() -> {
            host.removeAll();
            host.add(panel);
            host.revalidate();
            frame.validate();
        });
        UiProbeSupport.check("skeletons are not focusable",
            PhotoProbeSupport.onEdt(() -> !panel.isFocusable()
                && !panel.isRequestFocusEnabled()
                && !panel.requestFocusInWindow()));
    }

    private static void pulseStopsWhenHidden() {
        SkeletonPanel panel = PhotoProbeSupport.onEdt(() ->
            (SkeletonPanel) host.getComponent(0));
        PhotoProbeSupport.waitFor("the pulse runs while showing", 2000, panel::pulseRunning);
        PhotoProbeSupport.onEdtRun(() -> frame.setVisible(false));
        PhotoProbeSupport.waitFor("the pulse stops while hidden", 2000,
            () -> !panel.pulseRunning());
        PhotoProbeSupport.onEdtRun(() -> frame.setVisible(true));
    }

    private static void accessibleName() {
        AtomicInteger named = new AtomicInteger();
        AtomicInteger cleared = new AtomicInteger();
        Slot slot = new Slot();
        PhotoProbeSupport.onEdtRun(() -> {
            host.removeAll();
            host.revalidate();
            slot.reset();
            host.getAccessibleContext().addPropertyChangeListener(event -> {
                if (!AccessibleContext.ACCESSIBLE_NAME_PROPERTY.equals(
                        event.getPropertyName())) {
                    return;
                }
                if (event.getNewValue() == null) {
                    cleared.incrementAndGet();
                } else {
                    named.incrementAndGet();
                }
            });
            SkeletonGate.run(host, slot, new SkeletonPanel(SkeletonPanel.Variant.PREVIEW),
                decode(300));
        });
        PhotoProbeSupport.waitFor("a gated load completes", 4000,
            () -> slot.results.get() == 1);
        UiProbeSupport.check("the loading name is announced once", named.get() == 1);
        UiProbeSupport.check("the loading name is cleared once", cleared.get() == 1);
        UiProbeSupport.check("the gated load paints one skeleton", slot.skeletons.get() == 1);
    }

    private static void watchdog() {
        Slot slot = new Slot();
        PhotoProbeSupport.onEdtRun(() -> {
            host.removeAll();
            host.revalidate();
            slot.reset();
            SkeletonGate.run(host, slot, new SkeletonPanel(SkeletonPanel.Variant.MEDIA),
                decode(30_000));
        });
        PhotoProbeSupport.waitFor("the watchdog replaces a stuck skeleton", 15_000,
            () -> slot.results.get() == 1);
        UiProbeSupport.check("the watchdog takes the failure path", slot.emptyResult);
        UiProbeSupport.check("no skeleton outlives its work",
            slot.skeletons.get() == 1
                && PhotoProbeSupport.onEdt(() -> PhotoProbeSupport.find(host,
                    SkeletonPanel.class) == null));
        UiProbeSupport.check("the watchdog clears the loading name",
            PhotoProbeSupport.onEdt(() -> host.getAccessibleContext()
                .getAccessibleName() == null));
    }

    /**
     * A decode that takes {@code ms} milliseconds. The watchdog case uses 30 s
     * so the gate has to time it out; {@code cancel(true)} interrupts it.
     */
    private static Supplier<Optional<BufferedImage>> decode(int ms) {
        return () -> {
            PhotoProbeSupport.sleep(ms);
            return Optional.of(PhotoProbeSupport.photo());
        };
    }

    /** Records what the gate swapped in; confined to the EDT. */
    private static final class Slot implements SkeletonGate.Slot {
        private final AtomicInteger skeletons = new AtomicInteger();
        private final AtomicInteger results = new AtomicInteger();
        private boolean emptyResult;

        void reset() {
            skeletons.set(0);
            results.set(0);
            emptyResult = false;
        }

        @Override
        public void showSkeleton(SkeletonPanel skeleton) {
            skeletons.incrementAndGet();
            host.removeAll();
            host.add(skeleton);
            host.revalidate();
            host.repaint();
        }

        @Override
        public void showResult(Optional<BufferedImage> image) {
            results.incrementAndGet();
            emptyResult = image.isEmpty();
            host.removeAll();
            host.revalidate();
            host.repaint();
        }
    }
}
