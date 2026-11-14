package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import kitnapper.model.UserRole;

/**
 * The signed-in shell (design.md §5): title bar, omnibar, sidebar, content
 * CardLayout, status bar. Screens other than M2's routing targets render
 * as real page headers + a muted empty state — no fake controls. All
 * behavior is controller callbacks; the view holds no logic.
 */
public final class MainFrame extends javax.swing.JFrame {

    private static final long serialVersionUID = 1L;

    /** Screen metadata for routing titles and placeholder surfaces. */
    private record ScreenMeta(String title, String subtitle) { }

    private static final Map<String, ScreenMeta> SCREENS = new LinkedHashMap<>();

    private static void put(String key, String title, String subtitle) {
        SCREENS.put(key, new ScreenMeta(title, subtitle));
    }

    static {
        put("catalog", "Catalog", "Browse the lending catalog.");
        put("myloans", "My Loans", "Your active and past checkouts.");
        put("dashboard", "Dashboard", "Overdue loans and unpaid penalties at a glance.");
        put("allloans", "All Loans", "Every loan across all borrowers.");
        put("overdue", "Overdue", "Loans past their due date, longest first.");
        put("penalties", "Penalties", "Late fees waiting to be settled.");
        put("inventory", "Inventory", "The full equipment inventory.");
        put("settings", "Settings / Data", "Theme, save file, and demo data.");
    }

    private final UserRole role;
    private final TitleBar titleBar;
    private final Omnibar omnibar;
    private final StatusBar statusBar;
    private final Sidebar sidebar;
    private final java.awt.CardLayout cards = new java.awt.CardLayout();
    private final JPanel content = new JPanel(cards);

    public MainFrame(UserRole role, String userLabel, String statusLeft,
        Consumer<String> onNavigate, Runnable onSignOut, Runnable onThemeToggle) {
        this.role = role;
        WindowChrome chrome = WindowChrome.install(this);
        setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        titleBar = new TitleBar(chrome, onThemeToggle);
        omnibar = new Omnibar();
        omnibar.setOnHome(() -> onNavigate.accept(defaultKey()));
        statusBar = new StatusBar();
        statusBar.setLeft(statusLeft);
        sidebar = new Sidebar(role, userLabel, titles(), navKeys(),
            new Sidebar.Callbacks() {
                public void navigate(String key) {
                    onNavigate.accept(key);
                }

                public void signOut() {
                    onSignOut.run();
                }
            });

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(titleBar);
        north.add(omnibar);

        content.setOpaque(false);
        for (Map.Entry<String, ScreenMeta> entry : SCREENS.entrySet()) {
            content.add(placeholder(entry.getValue()), entry.getKey());
        }

        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        bindCtrlF();
        showScreen(defaultKey());
        setSize(1280, 800);
        setLocationRelativeTo(null);
        javax.swing.SwingUtilities.invokeLater(() -> {
            JButton first = sidebar.defaultButton(defaultKey());
            if (first != null) {
                first.requestFocusInWindow();
            }
        });
    }

    public void showScreen(String key) {
        ScreenMeta meta = SCREENS.get(key);
        if (meta == null) {
            return;
        }
        cards.show(content, key);
        titleBar.setScreenTitle(meta.title());
        omnibar.setScreen(meta.title());
        statusBar.setRight("");
        sidebar.select(key);
    }

    private String defaultKey() {
        return role == UserRole.MANAGER ? "dashboard" : "catalog";
    }

    private List<String> navKeys() {
        return role == UserRole.MANAGER
            ? List.of("dashboard", "allloans", "overdue", "penalties", "inventory")
            : List.of("catalog", "myloans");
    }

    private Map<String, String> titles() {
        Map<String, String> titles = new LinkedHashMap<>();
        for (Map.Entry<String, ScreenMeta> entry : SCREENS.entrySet()) {
            titles.put(entry.getKey(), entry.getValue().title());
        }
        return titles;
    }

    /** Ctrl+F focuses the search field (design §8). */
    private void bindCtrlF() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("control F"), "focusSearch");
        getRootPane().getActionMap().put("focusSearch", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                omnibar.searchField().requestFocusInWindow();
            }
        });
    }

    /** Real page header + muted empty state for every non-M3 screen. */
    private JPanel placeholder(ScreenMeta meta) {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(24, 32, 8, 32));
        header.add(UiFactory.titleLabel(meta.title()));
        header.add(Box.createVerticalStrut(4));
        header.add(UiFactory.subtitleLabel(meta.subtitle()));
        javax.swing.JLabel message = UiFactory.label("No records to show yet.", "muted");
        message.setFont(Fonts.text(14, Font.PLAIN));
        message.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        page.add(header, BorderLayout.NORTH);
        page.add(message, BorderLayout.CENTER);
        return page;
    }

    /** Re-applies token colors after a Theme switch. */
    public void refreshTheme() {
        content.setBackground(Theme.bgApp);
        getContentPane().setBackground(Theme.bgApp);
        UiFactory.refresh(this);
        repaint();
    }
}
