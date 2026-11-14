package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

/**
 * Modal dialog shell (design.md §5/§6/§7): undecorated 480 px dialog over a
 * rgba(0,0,0,0.4) scrim, BG_ELEVATED 8 px radius, right-aligned footer with
 * the primary rightmost, Enter confirms, Esc cancels. No milestone in M2
 * calls this yet — it is the shared shell for M3/M5 dialogs.
 */
public final class Dialogs {

    private static final int WIDTH = 480;

    private Dialogs() { }

    /** Informational dialog with a single primary OK button. */
    public static void showMessage(Window owner, String title, String body) {
        shell(owner, title, body, "OK", ButtonRole.PRIMARY, result -> { });
    }

    /** Destructive confirmation (§7); returns true when the user confirms. */
    public static boolean confirmDestructive(Window owner, String title, String body,
        String confirmLabel) {
        boolean[] confirmed = { false };
        shell(owner, title, body, confirmLabel, ButtonRole.DESTRUCTIVE, ok -> confirmed[0] = ok);
        return confirmed[0];
    }

    private static void shell(Window owner, String title, String body, String confirmLabel,
        ButtonRole role, java.util.function.Consumer<Boolean> done) {
        JDialog dialog = new JDialog(owner, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));
        JComponent scrim = scrimOn(owner);
        RoundedPanel card = new RoundedPanel(true, 8);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));
        card.add(UiFactory.titleLabel(title), BorderLayout.NORTH);
        JLabel bodyLabel = new JLabel(body);
        bodyLabel.setFont(Fonts.text(14, Font.PLAIN));
        bodyLabel.setForeground(Theme.textSecondary);
        card.add(bodyLabel, BorderLayout.CENTER);
        JButton cancel = UiFactory.secondaryButton("Cancel");
        JButton confirm = role == ButtonRole.DESTRUCTIVE
            ? UiFactory.destructiveButton(confirmLabel)
            : UiFactory.primaryButton(confirmLabel);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        footer.add(cancel);
        footer.add(confirm);
        card.add(footer, BorderLayout.SOUTH);
        dialog.setContentPane(card);
        cancel.addActionListener(e -> close(dialog, scrim, done, false));
        confirm.addActionListener(e -> close(dialog, scrim, done, true));
        dialog.getRootPane().setDefaultButton(confirm);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "close");
        dialog.getRootPane().getActionMap().put("close",
            new javax.swing.AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    close(dialog, scrim, done, false);
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
        dialog.setSize(WIDTH, dialog.getHeight());
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /** Translucent overlay on the owner while the modal dialog shows. */
    private static JComponent scrimOn(Window owner) {
        JRootPane rootPane = owner instanceof javax.swing.JFrame frame ? frame.getRootPane()
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

    private static void close(JDialog dialog, JComponent scrim,
        java.util.function.Consumer<Boolean> done, boolean confirmed) {
        scrim.setVisible(false);
        dialog.dispose();
        done.accept(confirmed);
    }

    private enum ButtonRole { PRIMARY, DESTRUCTIVE }
}
