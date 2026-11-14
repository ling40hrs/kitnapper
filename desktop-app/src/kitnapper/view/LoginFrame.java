package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.util.Optional;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Login screen (design.md §5): the shared title bar, no omnibar/sidebar/
 * status, and a centered 420 px card on BG_APP with email, password, an
 * inline error line, and a collapsed "Demo accounts" hint. The view holds
 * no service logic — the controller's sign-in callback answers.
 */
public final class LoginFrame extends javax.swing.JFrame {

    private static final long serialVersionUID = 1L;

    /** What the user typed; the controller runs the auth check on it. */
    public record Credentials(String email, String password) { }

    private final javax.swing.JTextField email = UiFactory.textField("");
    private final javax.swing.JPasswordField password = UiFactory.passwordField();
    private final JLabel error = UiFactory.label("", "error");
    private final RoundedPanel card = new RoundedPanel(false, 8);
    private final JPanel demoPanel = demoAccounts();
    private Function<Credentials, Optional<String>> onSignIn;
    private Runnable onThemeToggle;

    public LoginFrame() {
        WindowChrome chrome = WindowChrome.install(this);
        setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        TitleBar titleBar = new TitleBar(chrome, this::fireThemeToggle);

        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(28, 32, 24, 32));
        JLabel wordmark = UiFactory.titleLabel("KitNapper");
        stretch(wordmark);
        JLabel tagline = UiFactory.label("Every kit, accounted for.", "muted");
        stretch(tagline);
        card.add(wordmark);
        card.add(Box.createVerticalStrut(4));
        card.add(tagline);
        card.add(Box.createVerticalStrut(20));
        card.add(fieldRow("Email", email));
        card.add(Box.createVerticalStrut(12));
        card.add(fieldRow("Password", password));
        card.add(Box.createVerticalStrut(8));
        JPanel errorLine = new JPanel(new BorderLayout());
        errorLine.setOpaque(false);
        errorLine.setPreferredSize(new Dimension(0, 18));
        errorLine.add(error, BorderLayout.WEST);
        stretch(errorLine);
        card.add(errorLine);
        card.add(Box.createVerticalStrut(4));
        JButton signIn = UiFactory.primaryButton("Sign in");
        stretch(signIn);
        signIn.addActionListener(e -> submit());
        card.add(signIn);
        card.add(Box.createVerticalStrut(16));
        JButton demoToggle = UiFactory.subtleButton("Demo accounts");
        stretch(demoToggle);
        demoToggle.addActionListener(e -> toggleDemo());
        card.add(demoToggle);
        demoPanel.setVisible(false);
        card.add(demoPanel);

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

        getRootPane().setDefaultButton(signIn);
        setTitle("KitNapper — Sign in");
        card.setPreferredSize(new Dimension(420, 350));
        setSize(1024, 640);
        setLocationRelativeTo(null);
        addWindowFocusListener(new java.awt.event.WindowAdapter() {
            private static final long serialVersionUID = 1L;

            @Override
            public void windowGainedFocus(java.awt.event.WindowEvent e) {
                email.requestFocusInWindow();
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
        error.setText("");
        String mail = email.getText().strip();
        String pass = new String(password.getPassword());
        if (mail.isEmpty() || pass.isEmpty()) {
            error.setText("Enter your email and password.");
            repaint();
            return;
        }
        if (onSignIn != null) {
            onSignIn.apply(new Credentials(mail, pass)).ifPresent(message -> {
                error.setText(message);
                repaint();
            });
        }
    }

    private void toggleDemo() {
        demoPanel.setVisible(!demoPanel.isVisible());
        card.setPreferredSize(new Dimension(420, demoPanel.isVisible() ? 416 : 350));
        revalidate();
        repaint();
    }

    /** Left-aligns a child in the Y axis card and lets it span the card. */
    private static void stretch(javax.swing.JComponent component) {
        component.setAlignmentX(0f);
        component.setMaximumSize(new Dimension(
            Integer.MAX_VALUE, component.getPreferredSize().height));
    }

    private void fireThemeToggle() {
        if (onThemeToggle != null) {
            onThemeToggle.run();
        }
    }

    private JPanel fieldRow(String labelText, javax.swing.JComponent field) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        JLabel label = UiFactory.label(labelText, "secondary");
        stretch(label);
        field.setAlignmentX(0f);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.add(label);
        row.add(Box.createVerticalStrut(4));
        row.add(field);
        stretch(row);
        return row;
    }

    private JPanel demoAccounts() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createVerticalStrut(12));
        panel.add(credLine("Manager    admin@institution.edu / admin123"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(credLine("Borrower   student1@institution.edu / student123"));
        stretch(panel);
        return panel;
    }

    private JLabel credLine(String text) {
        JLabel line = UiFactory.label(text, "muted");
        line.setFont(Fonts.mono(12, Font.PLAIN));
        stretch(line);
        return line;
    }

}
