package kitnapper.view;

import java.awt.Component;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import kitnapper.model.UserRole;

/**
 * Motion probe (plan §8.3, decision D39): the shared constants, the easing
 * endpoints and monotonicity, the {@code kitnapper.motion} switch (proved in a
 * child JVM, since the property is read once per JVM), and the screen-fade
 * overlay lifecycle on a real {@link MainFrame}: exactly one overlay per
 * navigation, never stacked, removed when the wash finishes, never created for
 * a hidden frame or a theme switch. Runs with motion **on**; every other
 * display probe runs with it off.
 */
public final class MotionProbe {

    private static int passed;
    private static int failed;

    private MotionProbe() { }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            System.exit(Motion.enabled() ? 1 : 0);
        }
        Fonts.load();
        Theme.apply(true);
        check("constants", Motion.FAST_MS == 83 && Motion.NORMAL_MS == 167
            && Motion.SCREEN_MS == 120 && Motion.TICK_MS == 16);
        check("ease-endpoints", Motion.ease(0f) == 0f && Motion.ease(1f) == 1f
            && Motion.ease(-1f) == 0f && Motion.ease(2f) == 1f);
        check("ease-monotone", monotone());
        check("enabled-default", Motion.enabled());
        check("enabled-off-property", childReportsDisabled());
        for (Map.Entry<String, Boolean> entry : MotionDialogChecks.results().entrySet()) {
            check(entry.getKey(), entry.getValue());
        }

        MainFrame frame = new MainFrame(UserRole.BORROWER, "Student One", "",
            key -> { }, () -> { }, () -> { });
        frame.setSize(1024, 640);
        SwingUtilities.invokeAndWait(() -> frame.setVisible(true));
        check("no-overlay-on-show", overlay(frame) == null && !hasOverlay(frame));

        SwingUtilities.invokeAndWait(() -> frame.showScreen("myloans"));
        FadeOverlay overlay = overlay(frame);
        check("screen-fade-one-overlay", overlay != null);
        check("screen-fade-click-through", overlay != null && !overlay.contains(4, 4));
        SwingUtilities.invokeAndWait(() -> frame.showScreen("dashboard"));
        check("screen-fade-not-stacked", count(frame) == 1);
        check("screen-fade-clears", waitGone(frame, Motion.SCREEN_MS * 5));

        SwingUtilities.invokeAndWait(() -> Theme.apply(false));
        check("theme-switch-no-overlay", !hasOverlay(frame));

        SwingUtilities.invokeAndWait(() -> frame.setVisible(false));
        SwingUtilities.invokeAndWait(() ->
            Motion.fadeScreen((JComponent) frame.getContentPane()));
        check("hidden-frame-no-overlay", !hasOverlay(frame));

        frame.dispose();
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** {@code ease} never decreases across 0…1 and stays inside 0…1. */
    private static boolean monotone() {
        float previous = -1f;
        for (int i = 0; i <= 20; i++) {
            float value = Motion.ease(i / 20f);
            if (value < previous || value < 0f || value > 1f) {
                return false;
            }
            previous = value;
        }
        return true;
    }

    /** A child JVM with {@code -Dkitnapper.motion=off} must report disabled. */
    private static boolean childReportsDisabled() throws Exception {
        String java = System.getProperty("java.home") + "/bin/java";
        ProcessBuilder builder = new ProcessBuilder(java, "-Dkitnapper.motion=off",
            "-cp", System.getProperty("java.class.path"),
            "kitnapper.view.MotionProbe", "child");
        builder.redirectErrorStream(true);
        Process child = builder.start();
        child.getInputStream().readAllBytes();
        return child.waitFor() == 0;
    }

    private static boolean waitGone(MainFrame frame, int timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (!hasOverlay(frame)) {
                return true;
            }
            Thread.sleep(20);
        }
        return !hasOverlay(frame);
    }

    private static boolean hasOverlay(MainFrame frame) throws Exception {
        return count(frame) > 0;
    }

    private static int count(MainFrame frame) throws Exception {
        int[] layers = new int[1];
        SwingUtilities.invokeAndWait(() -> layers[0] = frame.getLayeredPane()
            .getComponentsInLayer(JLayeredPane.MODAL_LAYER).length);
        return layers[0];
    }

    private static FadeOverlay overlay(MainFrame frame) throws Exception {
        FadeOverlay[] found = new FadeOverlay[1];
        SwingUtilities.invokeAndWait(() -> {
            for (Component child : frame.getLayeredPane()
                    .getComponentsInLayer(JLayeredPane.MODAL_LAYER)) {
                if (child instanceof FadeOverlay wash) {
                    found[0] = wash;
                }
            }
        });
        return found[0];
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
