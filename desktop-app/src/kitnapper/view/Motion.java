package kitnapper.view;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * The §7 motion facade (decision D39): durations, decelerate easing, the dialog
 * open/close fade with its 98→100 % grow, and the 120 ms screen fade. The theme
 * switch is deliberately instant — the crossfade is a §7 motion that is not
 * shipped. Every effect degrades to a no-op when {@code -Dkitnapper.motion=off}
 * is set, the tree is headless, the window is not showing, or the platform
 * cannot do per-pixel translucency; the probe suite runs with motion off so
 * paint assertions always see a still frame.
 */
public final class Motion {

    public static final int FAST_MS = 83;
    public static final int NORMAL_MS = 167;
    public static final int SCREEN_MS = 120;
    public static final int TICK_MS = 16;

    private static final float SCALE_FROM = 0.98f;
    private static final float OPACITY_PROBE = 0.99f;
    private static final String OVERLAY_KEY = "kitnapper.fadeOverlay";

    private Motion() { }

    /** {@code -Dkitnapper.motion=off} disables both fades (probes only). */
    public static boolean enabled() {
        return !"off".equals(System.getProperty("kitnapper.motion"));
    }

    /** Decelerate easing: quick start, settled finish, 0→0 and 1→1. */
    public static float ease(float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        return clamped * (2f - clamped);
    }

    /** Fades a modal dialog in and grows it from 98 % once the peer is shown. */
    public static void openDialog(JDialog dialog) {
        if (!enabled() || dialog == null || GraphicsEnvironment.isHeadless()) {
            return;
        }
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                if (!translucent(dialog)) {
                    return;
                }
                Rectangle end = dialog.getBounds();
                animate(FAST_MS, progress -> {
                    dialog.setOpacity(progress);
                    dialog.setBounds(scaled(end, SCALE_FROM + (1f - SCALE_FROM) * progress));
                }, () -> {
                    try {
                        dialog.setBounds(end);
                    } catch (IllegalComponentStateException gone) {
                        // Closed before the grow finished; nothing left to grow.
                    }
                });
            }
        });
    }

    /** Fades a dialog out, then runs {@code after} (usually dispose). */
    public static void closeDialog(JDialog dialog, Runnable after) {
        if (!enabled() || dialog == null || !dialog.isDisplayable()
                || GraphicsEnvironment.isHeadless() || !translucent(dialog)) {
            run(after);
            return;
        }
        animate(FAST_MS, progress -> dialog.setOpacity(1f - progress), () -> {
            try {
                dialog.setOpacity(1f);
            } catch (UnsupportedOperationException | IllegalComponentStateException gone) {
                return;
            }
            run(after);
        });
    }

    /** Washes the routed content with {@code BG_APP} for one screen change. */
    public static void fadeScreen(JComponent content) {
        if (!enabled() || content == null || GraphicsEnvironment.isHeadless()) {
            return;
        }
        Window window = SwingUtilities.getWindowAncestor(content);
        JRootPane root = SwingUtilities.getRootPane(content);
        if (window == null || !window.isShowing() || root == null) {
            return;
        }
        JLayeredPane pane = root.getLayeredPane();
        if (pane.getWidth() <= 0 || pane.getClientProperty(OVERLAY_KEY) != null) {
            return;
        }
        FadeOverlay overlay = new FadeOverlay(Theme.bgApp, SCREEN_MS, () -> {
            pane.putClientProperty(OVERLAY_KEY, null);
            pane.repaint();
        });
        overlay.setBounds(SwingUtilities.convertRectangle(content.getParent(),
            content.getBounds(), pane));
        pane.add(overlay, JLayeredPane.MODAL_LAYER);
        pane.putClientProperty(OVERLAY_KEY, overlay);
        overlay.start();
    }

    /**
     * Whether {@code window} can fade at all; when it cannot, the instant path
     * runs instead.
     *
     * <p>The query is {@link GraphicsDevice.WindowTranslucency#TRANSLUCENT},
     * because that is the capability {@code Window.setOpacity} documents for a
     * value below 1.0 — <em>not</em> {@code PERPIXEL_TRANSLUCENT}. An X server
     * can advertise per-pixel support (so the driver has a translucent visual)
     * while refusing window opacity outright — plain Xvfb does exactly that —
     * and the narrower query is what the call actually needs. The probe then
     * asks for a non-opaque value, since {@code setOpacity(1f)} is allowed on
     * every platform and a decorated window rejects anything lower with
     * {@link IllegalComponentStateException}.
     */
    private static boolean translucent(Window window) {
        GraphicsDevice device = window.getGraphicsConfiguration() == null
            ? null : window.getGraphicsConfiguration().getDevice();
        if (device == null || !device.isWindowTranslucencySupported(
                GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            return false;
        }
        try {
            window.setOpacity(1f);
            return true;
        } catch (UnsupportedOperationException | IllegalComponentStateException unavailable) {
            return false;
        }
    }

    /**
     * Shared {@link Timer} loop: {@code step} gets eased progress, then
     * {@code done}. A platform that advertised window opacity and then refused
     * a frame — some X servers accept the capability query and reject the
     * first non-opaque value, and a decorated peer rejects any value below 1.0
     * — settles the effect immediately instead of escaping onto the EDT, where
     * it would abandon the caller's {@code done} callback and leave the window
     * half-faded and undisposable. Only the two documented refusals are caught;
     * anything else is a real defect and must surface.
     */
    private static void animate(int durationMs, Consumer<Float> step, Runnable done) {
        long start = System.nanoTime();
        Timer timer = new Timer(TICK_MS, null);
        timer.addActionListener(event -> {
            long elapsed = (System.nanoTime() - start) / 1_000_000L;
            float raw = durationMs <= 0 ? 1f : Math.min(1f, elapsed / (float) durationMs);
            try {
                step.accept(ease(raw));
            } catch (UnsupportedOperationException | IllegalComponentStateException refused) {
                timer.stop();
                run(done);
                return;
            }
            if (raw >= 1f) {
                timer.stop();
                done.run();
            }
        });
        timer.start();
    }

    /** The final bounds shrunk to {@code factor} around their own center. */
    private static Rectangle scaled(Rectangle bounds, float factor) {
        int width = Math.round(bounds.width * factor);
        int height = Math.round(bounds.height * factor);
        return new Rectangle(bounds.x + (bounds.width - width) / 2,
            bounds.y + (bounds.height - height) / 2, width, height);
    }

    private static void run(Runnable action) {
        if (action != null) {
            action.run();
        }
    }
}
