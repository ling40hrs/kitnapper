package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * The centered 420 px sign-in card (design.md §5): wordmark, tagline, email
 * and password rows, an inline error line, the primary sign-in button, and a
 * collapsed "Demo accounts" hint. Layout only — {@link LoginFrame} owns the
 * window and {@link kitnapper.controller.AppController} answers the submit.
 */
final class LoginCard extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int WIDTH = 420;
    private static final int HEIGHT = 350;
    private static final int HEIGHT_EXPANDED = 416;

    private final JTextField email = UiFactory.textField("");
    private final JPasswordField password = UiFactory.passwordField();
    private final JLabel error = UiFactory.label("", "error");
    private final JButton signIn = UiFactory.primaryButton("Sign in");
    private final JPanel demoPanel = demoAccounts();
    private final RoundedPanel surface = new RoundedPanel(false, 8);

    LoginCard() {
        super(new BorderLayout());
        setOpaque(false);

        surface.setLayout(new BoxLayout(surface, BoxLayout.Y_AXIS));
        surface.setBorder(BorderFactory.createEmptyBorder(28, 32, 24, 32));
        JLabel wordmark = UiFactory.titleLabel("KitNapper");
        stretch(wordmark);
        JLabel tagline = UiFactory.label("Every kit, accounted for.", "muted");
        stretch(tagline);
        surface.add(wordmark);
        surface.add(Box.createVerticalStrut(4));
        surface.add(tagline);
        surface.add(Box.createVerticalStrut(20));
        surface.add(fieldRow("Email", email));
        surface.add(Box.createVerticalStrut(12));
        surface.add(fieldRow("Password", password));
        surface.add(Box.createVerticalStrut(8));
        surface.add(errorLine());
        surface.add(Box.createVerticalStrut(4));
        stretch(signIn);
        surface.add(signIn);
        surface.add(Box.createVerticalStrut(16));
        JButton demoToggle = UiFactory.subtleButton("Demo accounts");
        stretch(demoToggle);
        demoToggle.addActionListener(e -> toggleDemo());
        surface.add(demoToggle);
        demoPanel.setVisible(false);
        surface.add(demoPanel);

        add(surface);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
    }

    JTextField email() {
        return email;
    }

    JPasswordField password() {
        return password;
    }

    JLabel error() {
        return error;
    }

    JButton signInButton() {
        return signIn;
    }

    void toggleDemo() {
        demoPanel.setVisible(!demoPanel.isVisible());
        setPreferredSize(new Dimension(WIDTH,
            demoPanel.isVisible() ? HEIGHT_EXPANDED : HEIGHT));
        revalidate();
        repaint();
    }

    private JPanel errorLine() {
        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(false);
        line.setPreferredSize(new Dimension(0, 18));
        line.add(error, BorderLayout.WEST);
        stretch(line);
        return line;
    }

    private JPanel fieldRow(String labelText, JComponent field) {
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

    /** Left-aligns a child in the Y axis card and lets it span the card. */
    private static void stretch(JComponent component) {
        component.setAlignmentX(0f);
        component.setMaximumSize(new Dimension(
            Integer.MAX_VALUE, component.getPreferredSize().height));
    }
}
