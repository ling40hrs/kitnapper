package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;
import java.util.Objects;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

/**
 * The §5/§6 modal shell: 480 px undecorated card over a {@code rgba(0,0,0,0.4)}
 * scrim, {@code BG_ELEVATED}, radius 8, right-aligned footer with the primary
 * rightmost, Enter confirms, Esc cancels, and an inline DANGER line that keeps
 * the dialog open (§8.7). {@link Dialogs} is the public facade over this class.
 */
final class DialogSkin {

    private static final int WIDTH = 480;

    private DialogSkin() { }

    /**
     * Shows a modal dialog; the confirm handler returns {@code Optional.empty()}
     * to accept or a message to display inline without closing.
     */
    static boolean show(Window owner, String title, JComponent body, String confirmLabel,
        boolean destructive, Supplier<Optional<String>> onConfirm) {
        JButton confirm = destructive
            ? UiFactory.destructiveButton(confirmLabel)
            : UiFactory.primaryButton(confirmLabel);
        return show(owner, title, body, confirm, onConfirm);
    }

    /**
     * Shows a modal dialog around a caller-built confirm button, so a form can
     * restyle it while open (§8.9: primary for "Mark paid", destructive for
     * "Waive"). The caller owns the button's label and role.
     */
    static boolean show(Window owner, String title, JComponent body, JButton confirm,
        Supplier<Optional<String>> onConfirm) {
        Objects.requireNonNull(confirm, "confirm");
        JDialog dialog = new JDialog(owner, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));
        JComponent scrim = scrimOn(owner);
        boolean[] accepted = { false };

        JLabel error = messageLabel("");
        error.setVisible(false);
        JButton cancel = UiFactory.secondaryButton("Cancel");
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        footer.add(cancel);
        footer.add(confirm);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.add(error, BorderLayout.NORTH);
        south.add(footer, BorderLayout.SOUTH);

        RoundedPanel card = new RoundedPanel(true, 8);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));
        card.add(UiFactory.titleLabel(title), BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);
        dialog.setContentPane(card);

        confirm.addActionListener(e -> {
            Optional<String> failure = onConfirm.get();
            if (failure.isPresent()) {
                error.setText(failure.get());
                error.setVisible(true);
                resize(dialog, owner);
                return;
            }
            accepted[0] = true;
            close(dialog, scrim);
        });
        cancel.addActionListener(e -> close(dialog, scrim));
        dialog.getRootPane().setDefaultButton(confirm);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        dialog.getRootPane().getActionMap().put("close", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                close(dialog, scrim);
            }
        });
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                scrim.setVisible(false);
            }
        });
        dialog.pack();
        resize(dialog, owner);
        Motion.openDialog(dialog);
        dialog.setVisible(true);
        return accepted[0];
    }

    private static void resize(JDialog dialog, Window owner) {
        dialog.pack();
        dialog.setSize(WIDTH, dialog.getHeight());
        dialog.setLocationRelativeTo(owner);
    }

    /** 14 px body text in the secondary token, refreshed on theme switches. */
    static JLabel messageLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Fonts.text(14, java.awt.Font.PLAIN));
        label.putClientProperty(UiFactory.ROLE, "secondary");
        label.setForeground(Theme.textSecondary);
        return label;
    }

    /** Translucent overlay on the owner while the modal dialog shows. */
    private static JComponent scrimOn(Window owner) {
        JRootPane rootPane = owner instanceof JFrame frame ? frame.getRootPane()
            : owner instanceof JDialog parent ? parent.getRootPane() : null;
        if (rootPane == null) {
            return new JPanel();
        }
        JComponent scrim = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.scrim);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        scrim.setOpaque(false);
        rootPane.setGlassPane(scrim);
        scrim.setVisible(true);
        return scrim;
    }

    /** The §7 close: fade out first, then hide the scrim and dispose. */
    private static void close(JDialog dialog, JComponent scrim) {
        Motion.closeDialog(dialog, () -> {
            scrim.setVisible(false);
            dialog.dispose();
        });
    }
}
