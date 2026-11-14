package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 48 px omnibar row (design.md §5): breadcrumb (Home returns to the role's
 * default screen), a command slot for M3, and the right-aligned search
 * field (240 px, widening to 320 px on focus). Search is an inert
 * placeholder in M2; filtering arrives with the catalog in M3.
 */
public final class Omnibar extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int HEIGHT = 48;
    private static final int SEARCH_WIDTH = 240;
    private static final int SEARCH_FOCUS_WIDTH = 320;

    private final JLabel crumb = UiFactory.label("", "accent");
    private final javax.swing.JTextField search = UiFactory.textField("Search");

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

        // Command slot: M3 mounts "+ New" and "Filters" here.
        JPanel commands = new JPanel();
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
        search.setToolTipText("Search arrives with the catalog");
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
        return (JPanel) getComponent(1);
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
        g.setColor(Theme.bgApp);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Theme.borderSubtle);
        g.fillRect(0, getHeight() - 1, getWidth(), 1);
        super.paintComponent(g);
    }
}
