package kitnapper.view;

import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 * Timing and plumbing half of the loading skeleton (design §13.2, §13.6):
 * runs the work off the EDT, shows the skeleton only when the work outlives
 * {@link #DELAY_MS}, keeps it visible for {@link #MIN_VISIBLE_MS}, fades it
 * over {@link #FADE_MS}, and caps every run with a {@link #WATCHDOG_MS}
 * watchdog so a skeleton can never outlive its work. The host region carries
 * the accessible loading name; nothing here touches the service layer.
 */
final class SkeletonGate {

    /** A result arriving inside this window shows no skeleton at all. */
    static final int DELAY_MS = 150;

    /** Minimum time a painted skeleton stays up. */
    static final int MIN_VISIBLE_MS = 300;

    /** Crossfade to the content. */
    static final int FADE_MS = 167;

    /** Hard ceiling for any gated run. */
    static final int WATCHDOG_MS = 10_000;

    private static final String LOADING_NAME = "Loading photo";

    private SkeletonGate() {
    }

    /** The component that swaps the skeleton for the result. */
    interface Slot {
        void showSkeleton(SkeletonPanel skeleton);

        void showResult(Optional<BufferedImage> image);
    }

    /**
     * Starts a gated load.
     *
     * @param host     the region that announces the loading state
     * @param slot     swaps skeleton and result in the host's layout
     * @param skeleton the blocks to show, sized by the caller
     * @param work     runs off the EDT; may throw, which means "no result"
     * @return a cancel handle that suppresses every later callback
     */
    static Runnable run(JComponent host, Slot slot, SkeletonPanel skeleton,
                        Supplier<Optional<BufferedImage>> work) {
        Run run = new Run(host, slot, skeleton, work);
        run.start();
        return run::cancel;
    }

    /** One gated load; every field is confined to the EDT. */
    private static final class Run {

        private final JComponent host;
        private final Slot slot;
        private final SkeletonPanel skeleton;
        private final Supplier<Optional<BufferedImage>> work;
        private final Timer appear;
        private final Timer watchdog;
        private final SwingWorker<Optional<BufferedImage>, Void> worker;
        private boolean shown;
        private boolean finished;
        private boolean cancelled;
        private long shownAt;

        Run(JComponent host, Slot slot, SkeletonPanel skeleton,
            Supplier<Optional<BufferedImage>> work) {
            this.host = host;
            this.slot = slot;
            this.skeleton = skeleton;
            this.work = work;
            this.appear = new Timer(DELAY_MS, event -> reveal());
            this.appear.setRepeats(false);
            this.watchdog = new Timer(WATCHDOG_MS, event -> timeout());
            this.watchdog.setRepeats(false);
            this.worker = new SwingWorker<>() {
                @Override
                protected Optional<BufferedImage> doInBackground() {
                    return load();
                }

                @Override
                protected void done() {
                    finish(result());
                }
            };
        }

        void start() {
            appear.start();
            watchdog.start();
            worker.execute();
        }

        void cancel() {
            cancelled = true;
            appear.stop();
            watchdog.stop();
            worker.cancel(true);
        }

        private Optional<BufferedImage> load() {
            try {
                Optional<BufferedImage> image = work.get();
                return image == null ? Optional.empty() : image;
            } catch (RuntimeException failure) {
                return Optional.empty();
            }
        }

        private Optional<BufferedImage> result() {
            try {
                return worker.get();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            } catch (java.util.concurrent.ExecutionException | java.util.concurrent.CancellationException failed) {
                return Optional.empty();
            }
        }

        private void reveal() {
            if (cancelled || shown) {
                return;
            }
            shown = true;
            shownAt = System.nanoTime();
            slot.showSkeleton(skeleton);
            host.getAccessibleContext().setAccessibleName(LOADING_NAME);
        }

        private void timeout() {
            worker.cancel(true);
            finish(Optional.empty());
        }

        private void finish(Optional<BufferedImage> image) {
            if (cancelled || finished) {
                return;
            }
            finished = true;
            appear.stop();
            watchdog.stop();
            if (!shown) {
                slot.showResult(image);
                return;
            }
            long elapsedMs = (System.nanoTime() - shownAt) / 1_000_000;
            long hold = Math.max(0, MIN_VISIBLE_MS - elapsedMs);
            Timer delay = new Timer((int) hold, event -> fade(image));
            delay.setRepeats(false);
            delay.start();
        }

        private void fade(Optional<BufferedImage> image) {
            if (cancelled) {
                return;
            }
            skeleton.fadeOut(Theme.animateSkeletons ? FADE_MS : 0, () -> {
                if (!cancelled) {
                    slot.showResult(image);
                    host.getAccessibleContext().setAccessibleName(null);
                }
            });
        }
    }
}
