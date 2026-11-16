package kitnapper.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Visual half of the loading skeleton (design §13.3, §13.7): flat rounded
 * blocks that mirror the content they replace — media 16:9 at full token
 * alpha, text bars at 70 %. No gradient, no shimmer, no accent. The pulse
 * runs only while the panel is showing and {@link Theme#animateSkeletons} is
 * on; timing and swapping live in {@link SkeletonGate}.
 */
final class SkeletonPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Which content the blocks stand in for. */
    enum Variant { MEDIA, CARD, PREVIEW, TABLE }

    private static final int RADIUS = 4;
    private static final int BAR = 12;
    private static final int GAP = 8;
    private static final int PULSE_TICK_MS = 40;
    private static final int PULSE_PERIOD_MS = 1000;
    private static final float MIN_ALPHA = 0.55f;
    private static final float BAR_ALPHA = 0.7f;

    private final Variant variant;
    private final Timer pulse;
    private long pulseStarted;
    private float alpha = 1f;

    SkeletonPanel(Variant variant) {
        this.variant = variant;
        setOpaque(false);
        // design §13.5: a skeleton is never focusable and never in the tab
        // order — stated here rather than inherited from a JVM default.
        setFocusable(false);
        setRequestFocusEnabled(false);
        getAccessibleContext().setAccessibleName("Loading photo");
        pulse = new Timer(PULSE_TICK_MS, event -> tick());
        pulse.setCoalesce(true);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        startPulse();
    }

    @Override
    public void removeNotify() {
        stopPulse();
        super.removeNotify();
    }

    /** Starts the pulse when animation is allowed; a no-op otherwise. */
    void startPulse() {
        if (!Theme.animateSkeletons || pulse.isRunning()) {
            return;
        }
        pulseStarted = System.nanoTime();
        pulse.start();
    }

    void stopPulse() {
        if (pulse.isRunning()) {
            pulse.stop();
        }
        alpha = 1f;
    }

    /** True while the pulse timer is alive (probe seam, design §13.2). */
    boolean pulseRunning() {
        return pulse.isRunning();
    }

    /**
     * Crossfades the blocks away over {@code millis} and then runs
     * {@code done}. A reduced-motion client passes 0 for an instant swap.
     */
    void fadeOut(int millis, Runnable done) {
        stopPulse();
        if (millis <= 0) {
            alpha = 0f;
            repaint();
            done.run();
            return;
        }
        long started = System.nanoTime();
        Timer fade = new Timer(PULSE_TICK_MS, null);
        fade.addActionListener(event -> {
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;
            alpha = 1f - Math.min(1f, elapsedMs / (float) millis);
            repaint();
            if (alpha <= 0f) {
                fade.stop();
                done.run();
            }
        });
        fade.start();
    }

    private void tick() {
        if (!isShowing() || !Theme.animateSkeletons) {
            stopPulse();
            repaint();
            return;
        }
        long elapsedMs = (System.nanoTime() - pulseStarted) / 1_000_000;
        double phase = (elapsedMs % PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS;
        float wave = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * phase));
        alpha = MIN_ALPHA + (1f - MIN_ALPHA) * wave;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        for (Block block : blocks()) {
            g2.setColor(veil(Theme.skeletonBlock, alpha * block.alpha()));
            g2.fill(block.shape());
        }
        g2.dispose();
    }

    private List<Block> blocks() {
        List<Block> result = new ArrayList<>();
        int width = Math.max(1, getWidth() - 1);
        int height = Math.max(1, getHeight() - 1);
        switch (variant) {
            case MEDIA -> result.add(block(0, 0, width, height, 1f));
            case CARD -> {
                int media = Math.max(1, Math.round(width * 9f / 16f));
                result.add(block(0, 0, width, media, 1f));
                int y = media + GAP;
                result.add(block(0, y, (int) (width * 0.6f), BAR, BAR_ALPHA));
                result.add(block(0, y + BAR + 6, (int) (width * 0.4f), BAR, BAR_ALPHA));
            }
            case PREVIEW -> {
                int media = Math.max(1, Math.round(width * 9f / 16f));
                result.add(block(0, 0, width, media, 1f));
                int y = media + GAP + 4;
                float[] widths = { 0.8f, 0.6f, 0.7f, 0.5f };
                for (float share : widths) {
                    result.add(block(0, y, (int) (width * share), BAR, BAR_ALPHA));
                    y += BAR + 10;
                }
            }
            case TABLE -> {
                result.add(block(0, 0, width, 36, BAR_ALPHA));
                int y = 36 + GAP;
                for (int row = 0; row < 6; row++) {
                    result.add(block(0, y, width, 24, BAR_ALPHA));
                    y += 40;
                }
            }
            default -> result.add(block(0, 0, width, height, 1f));
        }
        return result;
    }

    private static Block block(int x, int y, int width, int height, float blockAlpha) {
        return new Block(new RoundRectangle2D.Double(x, y, Math.max(1, width),
            Math.max(1, height), RADIUS, RADIUS), blockAlpha);
    }

    /** Same RGB with a scaled alpha, so tokens stay in {@link Theme}. */
    private static Color veil(Color base, float factor) {
        int value = Math.round(Math.max(0f, Math.min(1f, factor)) * 255);
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), value);
    }

    private record Block(Shape shape, float alpha) { }
}
