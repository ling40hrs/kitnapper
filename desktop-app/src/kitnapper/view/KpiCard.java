package kitnapper.view;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;

/**
 * One §8.7 KPI card: the shared 8 px {@code BG_SURFACE} surface with a 12 px
 * secondary label, a 28 px semibold number and an optional 12 px muted
 * caption. The screen hands in strings — the card never counts loans or
 * formats money, it only renders what the controller resolved (D21).
 */
final class KpiCard extends RoundedPanel {

    private static final long serialVersionUID = 1L;

    private static final int RADIUS = 8;
    private static final int MIN_HEIGHT = 96;
    private static final int PAD = 16;

    private final String label;
    private final JLabel value = UiFactory.label("0", "title");
    private final JLabel caption = UiFactory.label("", "muted");

    KpiCard(String label) {
        super(false, RADIUS);
        this.label = label;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));
        setPreferredSize(new Dimension(0, MIN_HEIGHT));
        value.setFont(Fonts.display(28, Font.BOLD));
        caption.setVisible(false);
        add(UiFactory.label(label, "secondary"));
        add(Box.createVerticalStrut(6));
        add(value);
        add(Box.createVerticalStrut(4));
        add(caption);
        updateAccessibleName();
    }

    /** Replaces the number; the accessible name follows for probes and readers. */
    void setValue(String text) {
        value.setText(text);
        updateAccessibleName();
    }

    /** Shows the 12 px caption under the number; {@code null} hides it. */
    void setCaption(String text) {
        boolean visible = text != null && !text.isBlank();
        caption.setText(visible ? text : "");
        caption.setVisible(visible);
    }

    private void updateAccessibleName() {
        getAccessibleContext().setAccessibleName(label + ": " + value.getText());
    }
}
