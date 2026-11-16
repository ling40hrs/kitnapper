package kitnapper.view;

import java.awt.Frame;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Dialog-lifecycle motion checks (plan §8.3; found by the M6 WP5 interactive
 * pass): {@link Motion#openDialog} and {@link Motion#closeDialog} must never
 * throw on a real peer, and the close callback must always run — on a platform
 * without window opacity it <em>is</em> the dispose path, so losing it strands
 * an undismissable ghost dialog on screen.
 *
 * <p>The original defect: the capability guard asked for
 * {@code PERPIXEL_TRANSLUCENT}, which plain Xvfb reports as supported while
 * refusing {@code setOpacity(<1)} with {@code TRANSLUCENT translucency is not
 * supported}. The first fade frame threw out of {@code windowOpened}, aborting
 * the caller, and the dialog was never disposed. These checks fail on that
 * build and pass now, on both kinds of platform: where opacity works the fade
 * completes, and where it does not the instant path runs.
 */
final class MotionDialogChecks {

    private MotionDialogChecks() { }

    /** Runs the dialog lifecycle; the map keeps the report stable. */
    static Map<String, Boolean> results() throws Exception {
        Map<String, Boolean> results = new LinkedHashMap<>();
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        AtomicBoolean closed = new AtomicBoolean();

        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog((Frame) null, "motion probe", true);
            dialog.setUndecorated(true);
            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            dialog.setSize(240, 160);
            dialog.setLocation(40, 40);
            try {
                Motion.openDialog(dialog);
            } catch (RuntimeException failed) {
                thrown.set(failed);
                return;
            }
            // Scheduled before the modal pump starts, so it lands inside it.
            SwingUtilities.invokeLater(() -> Motion.closeDialog(dialog, () -> {
                closed.set(true);
                dialog.dispose();
            }));
            try {
                dialog.setVisible(true);
            } catch (RuntimeException failed) {
                thrown.set(failed);
            }
        });

        long deadline = System.currentTimeMillis() + 5000;
        while (!closed.get() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        results.put("dialog-open-no-throw", thrown.get() == null);
        results.put("dialog-close-runs-callback", closed.get());
        return results;
    }
}
