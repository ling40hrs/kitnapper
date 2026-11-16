package kitnapper.view;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * One toast surface. Paints itself (no child labels) so the fade composite
 * covers the text too, and animates the §7 slide-up 8 px + fade over 167 ms on
 * open and close. Colours are read from {@link Theme} at paint time.
 *
 * <p>A message wraps onto a second line before it ellipsises, so a checkout
 * toast keeps the equipment name and its due date, and a return toast keeps the
 * frozen fee (design §6 amended in M6).
 */
final class ToastView extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int PAD = 12;
    private static final int RADIUS = 8;
    private static final int ICON_SIZE = 16;
    private static final int SLIDE = 8;
    private static final int LINE_HEIGHT = 18;
    static final int MAX_LINES = 2;
    private static final String CHECK = Fonts.glyph("\uE73E", "\u2713");

    private final List<String> lines;
    private float phase;
    private boolean closing;
    private Runnable onClosed;
    private Timer animator;

    ToastView(String message) {
        this.lines = wrap(getFontMetrics(Fonts.text(14, java.awt.Font.PLAIN)), message);
        setOpaque(false);
        setFocusable(false);
        getAccessibleContext().setAccessibleName(message);
    }

    /** One line keeps the 48 px toast; a second line grows it by one line box. */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Toasts.WIDTH,
            Toasts.HEIGHT + (lines.size() - 1) * LINE_HEIGHT);
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

    /**
     * Wraps to at most {@link #MAX_LINES} lines of the toast text budget; the
     * second line ellipsises when even two lines cannot hold the message. The
     * accessible name keeps the full text.
     */
    private static List<String> wrap(FontMetrics metrics, String text) {
        int space = Toasts.WIDTH - PAD * 3 - ICON_SIZE;
        List<String> all = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && metrics.stringWidth(candidate) > space) {
                all.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        if (line.length() > 0) {
            all.add(line.toString());
        }
        if (all.size() <= MAX_LINES) {
            return all;
        }
        String remainder = String.join(" ", all.subList(1, all.size()));
        return List.of(all.get(0), ellipsise(metrics, remainder, space));
    }

    private static String ellipsise(FontMetrics metrics, String text, int space) {
        String cut = text;
        while (!cut.isEmpty() && metrics.stringWidth(cut + "\u2026") > space) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "\u2026";
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
        int block = lines.size() * LINE_HEIGHT;
        int top = (getHeight() - block) / 2;
        for (int i = 0; i < lines.size(); i++) {
            g2.drawString(lines.get(i), PAD * 2 + ICON_SIZE,
                top + i * LINE_HEIGHT + (LINE_HEIGHT - text.getHeight()) / 2 + text.getAscent());
        }
        g2.dispose();
    }
}
