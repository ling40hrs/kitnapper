package kitnapper.view;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import kitnapper.model.UserRole;

/**
 * The 240 px role-grouped sidebar (design.md §5): nav entries with a 3 px
 * amber bar + ACCENT_TINT when selected, hover BG_ELEVATED, and a footer
 * with the user chip and Sign out. Managers additionally get Settings.
 */
final class Sidebar extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int WIDTH = 240;

    /** View-side callbacks; the controller owns routing and sign-out. */
    interface Callbacks {
        void navigate(String key);

        void signOut();
    }

    private final UserRole role;
    private final Map<String, SidebarButton> buttons = new java.util.LinkedHashMap<>();

    Sidebar(UserRole role, String userLabel, Map<String, String> titles, List<String> keys,
        Callbacks callbacks) {
        this.role = role;
        setOpaque(false);
        setPreferredSize(new Dimension(WIDTH, 0));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(UiFactory.label(role == UserRole.MANAGER ? "Manager" : "Home", "muted"));
        add(Box.createVerticalStrut(8));
        for (String key : keys) {
            SidebarButton button = new SidebarButton(titles.get(key));
            button.addActionListener(e -> callbacks.navigate(key));
            buttons.put(key, button);
            add(button);
            add(Box.createVerticalStrut(2));
        }
        if (role == UserRole.MANAGER) {
            SidebarButton settings = new SidebarButton(titles.get("settings"));
            settings.addActionListener(e -> callbacks.navigate("settings"));
            buttons.put("settings", settings);
            add(settings);
        }
        add(Box.createVerticalGlue());
        add(chip(userLabel));
        add(Box.createVerticalStrut(8));
        add(signOut(callbacks));
    }

    /** Highlights the active screen's entry. */
    void select(String key) {
        for (Map.Entry<String, SidebarButton> entry : buttons.entrySet()) {
            entry.getValue().setActive(entry.getKey().equals(key));
        }
    }

    /** The default screen's entry, for the keyboard start point. */
    JButton defaultButton(String key) {
        return buttons.get(key);
    }

    private JPanel chip(String userLabel) {
        JPanel chip = new JPanel();
        chip.setOpaque(false);
        chip.setLayout(new BoxLayout(chip, BoxLayout.Y_AXIS));
        JLabel name = UiFactory.label(userLabel, "title");
        name.setFont(Fonts.text(14, Font.PLAIN));
        JLabel roleName = UiFactory.label(
            role == UserRole.MANAGER ? "Manager" : "Borrower", "secondary");
        for (JLabel label : new JLabel[] { name, roleName }) {
            label.setAlignmentX(0f);
            label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));
            chip.add(label);
        }
        return chip;
    }

    private JButton signOut(Callbacks callbacks) {
        JButton signOut = UiFactory.subtleButton("Sign out");
        signOut.setAlignmentX(0f);
        signOut.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        signOut.addActionListener(e -> callbacks.signOut());
        return signOut;
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(Theme.bgSurface);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Theme.borderSubtle);
        g.fillRect(getWidth() - 1, 0, 1, getHeight());
    }

    /** Nav entry: amber bar + tint when active, hover fill otherwise. */
    private static final class SidebarButton extends JButton {

        private static final long serialVersionUID = 1L;
        private boolean active;

        SidebarButton(String label) {
            super(label);
            setFont(Fonts.text(14, Font.PLAIN));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            setPreferredSize(new Dimension(WIDTH, 36));
            getAccessibleContext().setAccessibleName(label);
        }

        void setActive(boolean value) {
            active = value;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            if (active) {
                g.setColor(Theme.accentTint);
                g.fillRect(0, 0, w, h);
                g.setColor(Theme.accent);
                g.fillRect(0, 0, 3, h);
            } else if (getModel().isRollover()) {
                g.setColor(Theme.bgElevated);
                g.fillRect(0, 0, w, h);
            }
            g.setColor(Theme.textPrimary);
            java.awt.FontMetrics metrics = g.getFontMetrics();
            g.drawString(getText(), 12, (h - metrics.getHeight()) / 2 + metrics.getAscent());
            if (isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.accent);
                g2.setStroke(new java.awt.BasicStroke(2));
                g2.drawRect(1, 1, w - 3, h - 3);
                g2.dispose();
            }
        }
    }
}
