package kitnapper.view;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.Border;

/**
 * The §6 field skin: 32 px, radius 4, BG_CONTROL fill, 1 px subtle
 * outline, BORDER_STRONG bottom that becomes 2 px ACCENT on focus, and a
 * muted placeholder while empty and unfocused.
 */
final class FieldSkin {

    private FieldSkin() { }

    static JTextField field(String placeholder) {
        JTextField f = new PlaceholderField(placeholder);
        style(f);
        return f;
    }

    static JPasswordField password() {
        JPasswordField f = new JPasswordField();
        style(f);
        return f;
    }

    private static void style(JTextField f) {
        f.setOpaque(false);
        f.setBorder(new FieldBorder());
        f.setForeground(Theme.textPrimary);
        f.setCaretColor(Theme.textPrimary);
        f.setPreferredSize(new Dimension(f.getPreferredSize().width, 32));
    }

    private static final class FieldBorder implements Border {

        private static final long serialVersionUID = 1L;

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.bgControl);
            g2.fill(new RoundRectangle2D.Double(x, y, w - 1, h - 1, 4, 4));
            g2.setColor(Theme.borderSubtle);
            g2.draw(new RoundRectangle2D.Double(x, y, w - 1, h - 1, 4, 4));
            JTextField field = (JTextField) c;
            if (field.hasFocus()) {
                g2.setColor(Theme.accent);
                g2.fillRect(x + 1, y + h - 3, w - 2, 2);
            } else {
                g2.setColor(Theme.borderStrong);
                g2.fillRect(x + 1, y + h - 2, w - 2, 1);
            }
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 10, 4, 10);
        }

        @Override
        public boolean isBorderOpaque() {
            return true;
        }
    }

    private static final class PlaceholderField extends JTextField {

        private static final long serialVersionUID = 1L;
        private final String placeholder;

        PlaceholderField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !hasFocus() && placeholder != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.textMuted);
                g2.setFont(getFont());
                FontMetrics metrics = g2.getFontMetrics();
                g2.drawString(placeholder, 10,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
                g2.dispose();
            }
        }
    }
}
