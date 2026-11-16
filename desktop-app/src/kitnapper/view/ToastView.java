package kitnapper.view;

import java.awt.AlphaComposite;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * One toast surface. Paints itself (no child labels) so the fade composite
 * covers the text too, and animates the §7 slide-up 8 px + fade over 167 ms on
 * open and close. Colours are read from {@link Theme} at paint time.
 */
final class ToastView extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int PAD = 12;
    private static final int RADIUS = 8;
    private static final int ICON_SIZE = 16;
    private static final int SLIDE = 8;
    private static final String CHECK = Fonts.glyph("\uE73E", "\u2713");

    private final String message;
    private float phase;
    private boolean closing;
    private Runnable onClosed;
    private Timer animator;

    ToastView(String message) {
        this.message = message;
        setOpaque(false);
        setFocusable(false);
        getAccessibleContext().setAccessibleName(message);
    }

    void open() {
        animateTo(1f);
    }

    boolean isClosing() {
        return closing;
    }

    void close(Runnable done) {
        closing = true;
        onClosed = done;
        animateTo(0f);
    }

    private void animateTo(float target) {
        if (animator != null) {
            animator.stop();
        }
        animator = new Timer(Motion.TICK_MS, null);
        animator.addActionListener(e -> {
            float step = (float) Motion.TICK_MS / Motion.NORMAL_MS;
            phase = target > phase ? Math.min(target, phase + step)
                : Math.max(target, phase - step);
            repaint();
            if (phase == target) {
                animator.stop();
                if (target == 0f && onClosed != null) {
                    onClosed.run();
                }
            }
        });
        animator.start();
    }

    /** Ellipsises to the toast width; the accessible name keeps the full text. */
    private static String fit(FontMetrics metrics, String text) {
        int space = Toasts.WIDTH - PAD * 3 - ICON_SIZE;
        if (metrics.stringWidth(text) <= space) {
            return text;
        }
        String ellipsis = "\u2026";
        String cut = text;
        while (!cut.isEmpty() && metrics.stringWidth(cut + ellipsis) > space) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + ellipsis;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        float alpha = Math.max(0f, Math.min(1f, phase));
        g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g2.translate(0, Math.round((1f - alpha) * SLIDE));
        RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1,
            RADIUS, RADIUS);
        g2.setColor(Theme.bgElevated);
        g2.fill(shape);
        g2.setColor(Theme.borderSubtle);
        g2.draw(shape);
        g2.setFont(Fonts.icons(14));
        g2.setColor(Theme.dotAvailable);
        FontMetrics icon = g2.getFontMetrics();
        g2.drawString(CHECK, PAD, (getHeight() - icon.getHeight()) / 2 + icon.getAscent());
        g2.setFont(Fonts.text(14, java.awt.Font.PLAIN));
        g2.setColor(Theme.textPrimary);
        FontMetrics text = g2.getFontMetrics();
        g2.drawString(fit(text, message), PAD * 2 + ICON_SIZE,
            (getHeight() - text.getHeight()) / 2 + text.getAscent());
        g2.dispose();
    }
}
