package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import kitnapper.model.Equipment;

/**
 * The §8.7 checkout dialog: item summary, {@link DatePicker}, and
 * {@link Dialogs#showForm}'s footer, scrim and inline DANGER line. The confirm
 * handler returns the service message to keep the dialog open on failure; on
 * success the dialog closes and the caller toasts.
 */
final class CheckoutDialog {

    private CheckoutDialog() { }

    /** Returns true when the checkout succeeded; failures stay in the dialog. */
    static boolean show(Window owner, Equipment item, List<LocalDate> dueDates,
        Function<LocalDate, Optional<String>> checkout) {
        DatePicker picker = new DatePicker(dueDates);
        return Dialogs.showForm(owner, "Check out gear", body(item, picker), "Check out",
            false, () -> checkout.apply(picker.value()));
    }

    private static JComponent body(Equipment item, DatePicker picker) {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(item.getName());
        name.setFont(Fonts.display(16, java.awt.Font.BOLD));
        name.putClientProperty(UiFactory.ROLE, "title");
        name.setForeground(Theme.textPrimary);
        name.setAlignmentX(0f);
        body.add(name);
        body.add(Box.createVerticalStrut(12));
        body.add(summary("Category", Formats.category(item.getCategory()), false));
        body.add(summary("Serial", item.getSerialNumber(), true));
        body.add(summary("Replacement value",
            Formats.money(item.getReplacementValue()), true));
        body.add(Box.createVerticalStrut(16));
        JLabel dueLabel = UiFactory.label("Due date", "secondary");
        dueLabel.setAlignmentX(0f);
        body.add(dueLabel);
        body.add(Box.createVerticalStrut(4));
        picker.setAlignmentX(0f);
        body.add(picker);
        return body;
    }

    /** One 14 px label/value row; serials and money stay monospace (§8.2). */
    private static JPanel summary(String labelText, String value, boolean mono) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 6, 0));
        row.add(UiFactory.label(labelText, "secondary"), BorderLayout.WEST);
        JLabel text = new JLabel(value);
        text.setFont(mono ? Fonts.mono(14, java.awt.Font.PLAIN)
            : Fonts.text(14, java.awt.Font.PLAIN));
        text.putClientProperty(UiFactory.ROLE, "title");
        text.setForeground(Theme.textPrimary);
        row.add(text, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }
}
