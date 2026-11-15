package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.util.Optional;
import java.util.function.Function;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Login screen (design.md §5): the shared title bar, no omnibar/sidebar/
 * status, and the centered sign-in card on BG_APP. The view holds no service
 * logic — the controller's sign-in callback answers. Card layout lives in
 * {@link LoginCard}.
 */
public final class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    /** What the user typed; the controller runs the auth check on it. */
    public record Credentials(String email, String password) { }

    private final LoginCard card = new LoginCard();
    private Function<Credentials, Optional<String>> onSignIn;
    private Runnable onThemeToggle;

    public LoginFrame() {
        WindowChrome chrome = WindowChrome.install(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        TitleBar titleBar = new TitleBar(chrome, this::fireThemeToggle);

        JPanel center = new JPanel(new GridBagLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.bgApp);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        center.add(card);
        setLayout(new BorderLayout());
        add(titleBar, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        card.signInButton().addActionListener(e -> submit());
        getRootPane().setDefaultButton(card.signInButton());
        setTitle("KitNapper — Sign in");
        setSize(1024, 640);
        setLocationRelativeTo(null);
        addWindowFocusListener(new java.awt.event.WindowAdapter() {
            private static final long serialVersionUID = 1L;

            @Override
            public void windowGainedFocus(java.awt.event.WindowEvent e) {
                card.email().requestFocusInWindow();
            }
        });
    }

    public void setOnSignIn(Function<Credentials, Optional<String>> handler) {
        onSignIn = handler;
    }

    public void setOnThemeToggle(Runnable callback) {
        onThemeToggle = callback;
    }

    /** Re-applies token colors after a Theme switch. */
    public void refreshTheme() {
        UiFactory.refresh(this);
        repaint();
    }

    private void submit() {
        card.error().setText("");
        String mail = card.email().getText().strip();
        String pass = new String(card.password().getPassword());
        if (mail.isEmpty() || pass.isEmpty()) {
            card.error().setText("Enter your email and password.");
            repaint();
            return;
        }
        if (onSignIn != null) {
            onSignIn.apply(new Credentials(mail, pass)).ifPresent(message -> {
                card.error().setText(message);
                repaint();
            });
        }
    }

    private void fireThemeToggle() {
        if (onThemeToggle != null) {
            onThemeToggle.run();
        }
    }
}
