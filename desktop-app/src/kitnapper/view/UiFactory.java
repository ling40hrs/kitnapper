package kitnapper.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
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

    /** §6 filter combo: 32 px, {@code BG_CONTROL}, themed cell renderer. */
    public static JComboBox<String> comboBox(String accessibleName, String... items) {
        JComboBox<String> combo = new JComboBox<>(items);
        styleCombo(combo);
        combo.getAccessibleContext().setAccessibleName(accessibleName);
        combo.setPreferredSize(new Dimension(combo.getPreferredSize().width + 24, 32));
        return combo;
    }

    /** Shared combo skin; {@link DatePicker} reuses it for its date renderer. */
    static void styleCombo(JComboBox<?> combo) {
        combo.setFont(Fonts.text(13, java.awt.Font.PLAIN));
        combo.setBackground(Theme.bgControl);
        combo.setForeground(Theme.textPrimary);
        combo.setBorder(BorderFactory.createLineBorder(Theme.borderSubtle));
        combo.setRenderer(new ComboCell());
        paintArrow(combo);
    }

    /** The LAF arrow button sits inside the combo; keep it on the surface token. */
    private static void paintArrow(JComboBox<?> combo) {
        for (Component child : combo.getComponents()) {
            child.setBackground(Theme.bgControl);
            child.setForeground(Theme.textPrimary);
        }
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
        if (root instanceof JTextField field) {
            field.setForeground(Theme.textPrimary);
            field.setCaretColor(Theme.textPrimary);
        }
        if (root instanceof JComboBox<?> combo) {
            combo.setBackground(Theme.bgControl);
            combo.setForeground(Theme.textPrimary);
            combo.setBorder(BorderFactory.createLineBorder(Theme.borderSubtle));
            paintArrow(combo);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                refresh(child);
            }
        }
        root.repaint();
    }

    static Color colorFor(String role) {
        return switch (role) {
            case "title" -> Theme.textPrimary;
            case "subtitle", "secondary" -> Theme.textSecondary;
            case "muted" -> Theme.textMuted;
            case "error" -> Theme.danger;
            case "accent" -> Theme.accentText;
            default -> Theme.textPrimary;
        };
    }

    /** Themed combo cell: {@code BG_CONTROL} closed, elevated popup, accent tint selection. */
    private static final class ComboCell extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setFont(Fonts.text(13, java.awt.Font.PLAIN));
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 8));
            setOpaque(true);
            if (index < 0) {
                setBackground(Theme.bgControl);
            } else {
                setBackground(isSelected ? Theme.accentTint : Theme.bgElevated);
            }
            setForeground(Theme.textPrimary);
            return this;
        }
    }
}
