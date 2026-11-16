package kitnapper.view;

import java.awt.FontMetrics;
import java.util.List;

import javax.swing.JLabel;

/**
 * Dialog body checks (M6 WP5, finding F10): the 480 px card cut long copy, so
 * {@link DialogSkin#setBodyText} wraps it to the content width before the card
 * pins its size. Short copy stays a plain label line.
 */
final class DialogTextChecks {

    /** {@link DialogSkin} content width: the card less its 24 px side padding. */
    private static final int CONTENT_WIDTH = 480 - 24 * 2;

    private DialogTextChecks() { }

    static void run() {
        String longCopy = "Reset the demo data? Every equipment item, loan and penalty is "
            + "replaced with the seeded demo set, and this cannot be undone.";
        JLabel wrapped = DialogSkin.messageLabel(longCopy);
        UiProbeSupport.check("dialog-body-wraps", wrapped.getText().startsWith("<html>")
            && wrapped.getText().contains("<br>"));
        UiProbeSupport.check("dialog-body-fits-content-width",
            linesFit(wrapped.getText()));
        UiProbeSupport.check("dialog-body-short-plain",
            DialogSkin.messageLabel("Sign out?").getText().equals("Sign out?"));
        String note = ReturnDialog.feeNote(200).replace("<br>", " ");
        UiProbeSupport.check("return-note-fits",
            note.contains("when you log the return.") && !note.contains("\u2026")
                && ReturnDialog.feeNote(0).contains("on time"));
    }

    /** Each wrapped line measures inside the card's content width. */
    private static boolean linesFit(String html) {
        FontMetrics metrics = new JLabel().getFontMetrics(Fonts.text(14, java.awt.Font.PLAIN));
        String body = html.replace("<html>", "").replace("</html>", "");
        for (String line : List.of(body.split("<br>"))) {
            if (metrics.stringWidth(line) > CONTENT_WIDTH) {
                return false;
            }
        }
        return true;
    }
}
