package kitnapper.view;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * The §6 field skin: 32 px, radius 4, BG_CONTROL fill, 1 px subtle
 * outline, BORDER_STRONG bottom that becomes 2 px ACCENT on focus, and a
 * muted placeholder while empty and unfocused.
 *
 * <p>The fill is painted by the field itself, <em>before</em> the UI
 * delegate draws the text — painting it from a Border would cover the text,
 * because Swing paints the border after the component.</p>
 */
final class FieldSkin {

    private FieldSkin() { }

    static JTextField field(String placeholder) {
        JTextField f = new PlaceholderField(placeholder);
        style(f);
        return f;
    }

    static JPasswordField password() {
        JPasswordField f = new SkinnedPasswordField();
        style(f);
        return f;
    }

    /**
     * The skin is derived from focus at paint time — the 2 px accent underline
     * and the muted placeholder — but the text UI repaints only the caret's own
     * strip on a focus change. Without an explicit repaint an unfocused field
     * keeps the previous frame's underline and a newly focused field keeps its
     * placeholder under the caret (D52).
     */
    private static void style(JTextField f) {
        f.setOpaque(false);
        f.setBorder(new EmptyBorder(1, 10, 4, 10));
        f.setForeground(Theme.textPrimary);
        f.setCaretColor(Theme.textPrimary);
        f.setPreferredSize(new Dimension(f.getPreferredSize().width, 32));
        f.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                f.repaint();
            }

            @Override
            public void focusLost(FocusEvent event) {
                f.repaint();
            }
        });
    }

    /** Rounded fill + outline + focus underline, drawn beneath the text. */
    private static void paintSkin(Graphics g, JTextField field) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = field.getWidth();
        int h = field.getHeight();
        g2.setColor(Theme.bgControl);
        g2.fill(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 4, 4));
        g2.setColor(Theme.borderSubtle);
        g2.draw(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 4, 4));
        if (field.hasFocus()) {
            g2.setColor(Theme.accent);
            g2.fillRect(1, h - 3, w - 2, 2);
        } else {
            g2.setColor(Theme.borderStrong);
            g2.fillRect(1, h - 2, w - 2, 1);
        }
        g2.dispose();
    }

    /** Muted placeholder drawn on top of the empty field. */
    private static void paintPlaceholder(Graphics g, JTextField field, String placeholder) {
        if (placeholder == null || !field.getText().isEmpty() || field.hasFocus()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Theme.textMuted);
        g2.setFont(field.getFont());
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(placeholder, 10,
            (field.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
        g2.dispose();
    }

    private static final class PlaceholderField extends JTextField {

        private static final long serialVersionUID = 1L;
        private final String placeholder;

        PlaceholderField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintSkin(g, this);
            super.paintComponent(g);
            paintPlaceholder(g, this, placeholder);
        }
    }

    private static final class SkinnedPasswordField extends JPasswordField {

        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g) {
            paintSkin(g, this);
            super.paintComponent(g);
        }
    }
}
