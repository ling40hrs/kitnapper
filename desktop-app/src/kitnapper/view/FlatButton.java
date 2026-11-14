package kitnapper.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;

/**
 * The §6 button: 32 px, radius 4, flat single-step hover/press, 2 px ACCENT
 * focus rectangle with a 1 px inner gap. Reads Theme at paint time.
 */
final class FlatButton extends JButton {

    private static final long serialVersionUID = 1L;

    enum Role { PRIMARY, SECONDARY, SUBTLE, DESTRUCTIVE }

    private final Role role;

    FlatButton(String text, Role role) {
        super(text);
        this.role = role;
        setFont(Fonts.text(14, Font.PLAIN));
        setForeground(Theme.textPrimary);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setPreferredSize(new Dimension(getPreferredSize().width, 32));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 4, 4);
        Color fill = fillFor();
        if (fill != null) {
            g2.setColor(fill);
            g2.fill(shape);
        }
        if (role == Role.SECONDARY || role == Role.DESTRUCTIVE) {
            g2.setColor(role == Role.DESTRUCTIVE ? Theme.danger : Theme.borderSubtle);
            g2.draw(shape);
        }
        FontMetrics metrics = g2.getFontMetrics();
        g2.setColor(isEnabled() ? textFor() : Theme.textDisabled);
        g2.drawString(getText(), (w - metrics.stringWidth(getText())) / 2,
            (h - metrics.getHeight()) / 2 + metrics.getAscent());
        if (isFocusOwner()) {
            g2.setColor(role == Role.PRIMARY ? Theme.onAccent : Theme.accent);
            g2.setStroke(new BasicStroke(2));
            g2.draw(new RoundRectangle2D.Double(1, 1, w - 3, h - 3, 4, 4));
        }
        g2.dispose();
    }

    private Color fillFor() {
        boolean hover = getModel().isRollover();
        return switch (role) {
            case PRIMARY -> getModel().isPressed() ? Theme.accentPressed
                : hover ? Theme.accentHover : Theme.accent;
            case SECONDARY -> hover ? Theme.bgControlHover : Theme.bgControl;
            case SUBTLE, DESTRUCTIVE -> hover ? Theme.bgElevated : null;
        };
    }

    private Color textFor() {
        return switch (role) {
            case PRIMARY -> Theme.onAccent;
            case DESTRUCTIVE -> Theme.danger;
            default -> Theme.textPrimary;
        };
    }
}
