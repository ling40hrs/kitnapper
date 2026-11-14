package kitnapper.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;

/**
 * Factory for controls and labels (design.md §6). Painting lives in
 * {@link FlatButton} and {@link FieldSkin}; this file only builds and
 * theme-refreshes role-marked components.
 */
public final class UiFactory {

    /** Client property key marking a label's token role for refresh(). */
    public static final String ROLE = "kitnapper.role";

    private UiFactory() { }

    public static JButton primaryButton(String text) {
        return new FlatButton(text, FlatButton.Role.PRIMARY);
    }

    public static JButton secondaryButton(String text) {
        return new FlatButton(text, FlatButton.Role.SECONDARY);
    }

    public static JButton subtleButton(String text) {
        return new FlatButton(text, FlatButton.Role.SUBTLE);
    }

    public static JButton destructiveButton(String text) {
        return new FlatButton(text, FlatButton.Role.DESTRUCTIVE);
    }

    public static JTextField textField(String placeholder) {
        return FieldSkin.field(placeholder);
    }

    public static JPasswordField passwordField() {
        return FieldSkin.password();
    }

    public static JLabel titleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Fonts.display(20, java.awt.Font.BOLD));
        label.putClientProperty(ROLE, "title");
        return label;
    }

    public static JLabel subtitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Fonts.text(12, java.awt.Font.PLAIN));
        label.putClientProperty(ROLE, "subtitle");
        return label;
    }

    public static JLabel label(String text, String role) {
        JLabel label = new JLabel(text);
        label.setFont(Fonts.text(12, java.awt.Font.PLAIN));
        label.putClientProperty(ROLE, role);
        return label;
    }

    /** Re-applies token colors to role-marked labels and repaints the tree. */
    public static void refresh(Component root) {
        if (root instanceof JLabel label) {
            Object role = label.getClientProperty(ROLE);
            if (role != null) {
                label.setForeground(colorFor((String) role));
            }
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                refresh(child);
            }
        }
        root.repaint();
    }

    private static Color colorFor(String role) {
        return switch (role) {
            case "title" -> Theme.textPrimary;
            case "subtitle", "secondary" -> Theme.textSecondary;
            case "muted" -> Theme.textMuted;
            case "error" -> Theme.danger;
            case "accent" -> Theme.accentText;
            default -> Theme.textPrimary;
        };
    }
}
