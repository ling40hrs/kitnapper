package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 48 px omnibar row (design.md §5): breadcrumb (Home returns to the role's
 * default screen), a command slot screens fill through {@link #setCommands},
 * and the right-aligned search field (240 px, widening to 320 px on focus).
 * Screens attach their own document listeners; the catalog filters on keystroke.
 */
public final class Omnibar extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int HEIGHT = 48;
    private static final int SEARCH_WIDTH = 240;
    private static final int SEARCH_FOCUS_WIDTH = 320;

    private final JLabel crumb = UiFactory.label("", "accent");
    private final javax.swing.JTextField search = UiFactory.textField("Search");
    private final JPanel commands = new JPanel();

    public Omnibar() {
        setOpaque(false);
        setLayout(new BorderLayout(12, 0));
        setPreferredSize(new Dimension(0, HEIGHT));

        JPanel breadcrumb = new JPanel();
        breadcrumb.setOpaque(false);
        breadcrumb.setLayout(new BoxLayout(breadcrumb, BoxLayout.X_AXIS));
        javax.swing.JButton home = UiFactory.subtleButton("Home");
        home.setPreferredSize(new Dimension(home.getPreferredSize().width, 28));
        home.addActionListener(e -> fireHome());
        JLabel chevron = UiFactory.label(Fonts.glyph("\uE76C", "›"), "secondary");
        chevron.setFont(Fonts.icons(10));
        breadcrumb.add(home);
        breadcrumb.add(javax.swing.Box.createHorizontalStrut(4));
        breadcrumb.add(chevron);
        breadcrumb.add(javax.swing.Box.createHorizontalStrut(4));
        breadcrumb.add(crumb);
        add(breadcrumb, BorderLayout.WEST);

        // Command slot: screens mount their contextual commands here (M3).
        commands.setOpaque(false);
        add(commands, BorderLayout.CENTER);

        search.setPreferredSize(new Dimension(SEARCH_WIDTH, 32));
        search.getAccessibleContext().setAccessibleName("Search");
        search.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                search.setPreferredSize(new Dimension(SEARCH_FOCUS_WIDTH, 32));
                revalidate();
            }

            @Override
            public void focusLost(FocusEvent e) {
                search.setPreferredSize(new Dimension(SEARCH_WIDTH, 32));
                revalidate();
            }
        });
        search.setToolTipText("Search the catalog");
        JPanel right = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 8));
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        right.add(search);
        add(right, BorderLayout.EAST);
    }

    /** Current screen name as the active (amber) crumb. */
    public void setScreen(String name) {
        crumb.setText(name == null ? "" : name);
    }

    /** Navigating back to the role's default screen via the Home crumb. */
    public void setOnHome(Runnable onHome) {
        this.onHome = onHome;
    }

    /** M3 mounts contextual commands into this slot. */
    public JPanel commandSlot() {
        return commands;
    }

    /** Replaces the contextual commands; null clears the slot. */
    public void setCommands(JComponent commands) {
        this.commands.removeAll();
        if (commands != null) {
            this.commands.add(commands);
        }
        this.commands.revalidate();
        this.commands.repaint();
    }

    /** The search field, for the Ctrl+F binding and M3 filtering. */
    public javax.swing.JTextField searchField() {
        return search;
    }

    private void fireHome() {
        if (onHome != null) {
            onHome.run();
        }
    }

    private Runnable onHome;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Theme.bgApp);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Theme.borderSubtle);
        g.fillRect(0, getHeight() - 1, getWidth(), 1);
    }
}
