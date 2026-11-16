package kitnapper.view;

import java.awt.Window;
import java.util.Optional;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JComponent;

/**
 * Public modal-dialog facade (design.md §5/§6/§7): {@link DialogSkin} owns the
 * scrim, card, footer, Enter/Esc wiring, and inline error line; this class only
 * shapes the two M2 entry points and the form dialog M3 needs.
 */
public final class Dialogs {

    private Dialogs() { }

    /** Informational dialog with a single primary OK button. */
    public static void showMessage(Window owner, String title, String body) {
        DialogSkin.show(owner, title, DialogSkin.messageLabel(body), "OK", false,
            () -> Optional.empty());
    }

    /** Destructive confirmation (§7); returns true when the user confirms. */
    public static boolean confirmDestructive(Window owner, String title, String body,
        String confirmLabel) {
        return DialogSkin.show(owner, title, DialogSkin.messageLabel(body), confirmLabel, true,
            () -> Optional.empty());
    }

    /**
     * Form dialog (§8.7). The confirm handler returns a message to keep the
     * dialog open with that DANGER line, or {@code Optional.empty()} to accept.
     * Returns true when the user confirmed.
     */
    public static boolean showForm(Window owner, String title, JComponent body,
        String confirmLabel, boolean destructive, Supplier<Optional<String>> onConfirm) {
        return DialogSkin.show(owner, title, body, confirmLabel, destructive, onConfirm);
    }

    /**
     * Form dialog around a caller-built confirm button, so the button can
     * change role while the dialog is open (§8.9: the settle dialog's confirm
     * is primary for "Mark paid" and destructive for "Waive"). The caller owns
     * the label and role; Enter still confirms.
     */
    public static boolean showForm(Window owner, String title, JComponent body,
        JButton confirm, Supplier<Optional<String>> onConfirm) {
        return DialogSkin.show(owner, title, body, confirm, onConfirm);
    }
}
