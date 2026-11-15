package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import kitnapper.model.UserRole;

/**
 * The signed-in shell (design.md §5): title bar, omnibar, sidebar, content
 * CardLayout, status bar. Screens other than M2's routing targets render as
 * real page headers + a muted empty state — no fake controls. All behavior is
 * controller callbacks; the view holds no logic. Route metadata lives in
 * {@link ScreenCatalog}, placeholder surfaces in {@link PlaceholderScreen}.
 */
public final class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final UserRole role;
    private final TitleBar titleBar;
    private final Omnibar omnibar;
    private final StatusBar statusBar;
    private final Sidebar sidebar;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);

    public MainFrame(UserRole role, String userLabel, String statusLeft,
        Consumer<String> onNavigate, Runnable onSignOut, Runnable onThemeToggle) {
        this.role = role;
        WindowChrome chrome = WindowChrome.install(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        titleBar = new TitleBar(chrome, onThemeToggle);
        omnibar = new Omnibar();
        omnibar.setOnHome(() -> onNavigate.accept(ScreenCatalog.defaultKey(role)));
        statusBar = new StatusBar();
        statusBar.setLeft(statusLeft);
        sidebar = new Sidebar(role, userLabel, ScreenCatalog.titles(),
            ScreenCatalog.navKeys(role),
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
        for (String key : ScreenCatalog.keys()) {
            content.add(PlaceholderScreen.create(ScreenCatalog.meta(key)), key);
        }

        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        bindCtrlF();
        showScreen(ScreenCatalog.defaultKey(role));
        setSize(1280, 800);
        setLocationRelativeTo(null);
        SwingUtilities.invokeLater(() -> {
            String key = ScreenCatalog.defaultKey(role);
            JButton first = sidebar.defaultButton(key);
            if (first != null) {
                first.requestFocusInWindow();
            }
        });
    }

    public void showScreen(String key) {
        ScreenCatalog.Meta meta = ScreenCatalog.meta(key);
        if (meta == null) {
            return;
        }
        cards.show(content, key);
        titleBar.setScreenTitle(meta.title());
        omnibar.setScreen(meta.title());
        statusBar.setRight("");
        sidebar.select(key);
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

    /** Re-applies token colors after a Theme switch. */
    public void refreshTheme() {
        content.setBackground(Theme.bgApp);
        getContentPane().setBackground(Theme.bgApp);
        UiFactory.refresh(this);
        repaint();
    }
}
