package kitnapper.view;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * The §7 screen fade wash (decision D39): one translucent {@code BG_APP} layer
 * that thins to nothing over the duration, then calls back so its owner can
 * remove it. It never takes input — {@link #contains} is always false — so a
 * click during the fade reaches the screen underneath. Mounted on
 * {@code MODAL_LAYER}: above content, below toasts.
 */
final class FadeOverlay extends JComponent {

    private static final long serialVersionUID = 1L;

    private final Color wash;
    private final int durationMs;
    private final Runnable done;
    private final Timer timer;
    private final long startedAt = System.nanoTime();
    private float alpha = 1f;

    FadeOverlay(Color wash, int durationMs, Runnable done) {
        this.wash = wash;
        this.durationMs = durationMs;
        this.done = done;
        setOpaque(false);
        setFocusable(false);
        timer = new Timer(Motion.TICK_MS, event -> tick());
    }

    void start() {
        timer.start();
    }

    private void tick() {
        long elapsed = (System.nanoTime() - startedAt) / 1_000_000L;
        float raw = durationMs <= 0 ? 1f : Math.min(1f, elapsed / (float) durationMs);
        alpha = 1f - Motion.ease(raw);
        repaint();
        if (raw >= 1f) {
            timer.stop();
            Container parent = getParent();
            if (parent != null) {
                parent.remove(this);
            }
            if (done != null) {
                done.run();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (alpha <= 0f) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(wash);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    /** Clicks pass through the wash to the screen beneath (plan §5.2). */
    @Override
    public boolean contains(int x, int y) {
        return false;
    }
}
