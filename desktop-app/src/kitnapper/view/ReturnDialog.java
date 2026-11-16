package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.ToDoubleFunction;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The §8.8 return dialog: who borrowed what, the dates, the loan's current
 * badge, a {@link DatePicker} over the loan's valid return dates, and a live
 * "Fee to freeze" line recomputed from the controller on every pick while
 * {@link Dialogs}'s inline DANGER line keeps a rejected return open. Confirming
 * returns the frozen fee so the caller's toast can name it; cancelling is
 * empty.
 */
final class ReturnDialog {

    /** The form's label column, so the fee note wraps instead of clipping. */
    private static final int NOTE_WIDTH = 360;

    private ReturnDialog() { }

    /**
     * Opens the dialog. {@code feeOn} asks the controller for the fee at a date
     * (never the view); {@code onReturn} logs it and reports failures.
     *
     * @return the frozen fee, or empty when cancelled or rejected
     */
    static Optional<Double> show(Window owner, LoanRow row, List<LocalDate> dates,
        ToDoubleFunction<LocalDate> feeOn,
        BiFunction<LoanRow, LocalDate, Optional<String>> onReturn) {
        if (dates.isEmpty()) {
            return Optional.empty();
        }
        DatePicker picker = new DatePicker(dates, dates.size() - 1, "Return date");
        JLabel fee = new JLabel();
        fee.setFont(Fonts.mono(18, Font.BOLD));
        fee.putClientProperty(UiFactory.ROLE, "title");
        fee.setAlignmentX(0f);
        JLabel note = UiFactory.label("", "muted");
        note.setAlignmentX(0f);
        Runnable refreshFee = () -> {
            double amount = feeOn.applyAsDouble(picker.value());
            fee.setText(Formats.money(amount));
            note.setText(feeNote(amount));
        };
        picker.addActionListener(event -> refreshFee.run());
        refreshFee.run();

        LocalDate[] chosen = new LocalDate[1];
        double[] frozen = new double[1];
        boolean done = Dialogs.showForm(owner, "Log return", body(row, picker, fee, note),
            "Log return", false, () -> {
                chosen[0] = picker.value();
                frozen[0] = feeOn.applyAsDouble(chosen[0]);
                return onReturn.apply(row, chosen[0]);
            });
        return done && chosen[0] != null ? Optional.of(frozen[0]) : Optional.empty();
    }

    /**
     * The fee note under the amount, wrapped to the form's label column: the
     * 480 px card clipped "…when you log the retur…" (WP5 F7).
     */
    static String feeNote(double amount) {
        String text = amount > 0
            ? "This fee is frozen into one LATE_FEE penalty when you log the return."
            : "No fee is frozen when the return is on time.";
        return PreviewLayout.wrapText(text, NOTE_WIDTH, 2);
    }

    private static JComponent body(LoanRow row, DatePicker picker, JLabel fee, JLabel note) {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(row.itemName());
        name.setFont(Fonts.display(16, Font.BOLD));
        name.putClientProperty(UiFactory.ROLE, "title");
        name.setAlignmentX(0f);
        body.add(name);
        body.add(Box.createVerticalStrut(12));
        body.add(summary("Borrower", textLabel(row.borrowerName())));
        body.add(summary("Borrowed", monoLabel(Formats.date(row.borrowDate()))));
        body.add(summary("Due", monoLabel(Formats.date(row.dueDate()))));
        body.add(summary("Status", StatusBadges.loanStatus(row.status())));
        body.add(summary("Days late", monoLabel(Long.toString(row.daysLate()))));

        body.add(Box.createVerticalStrut(16));
        JLabel dateLabel = UiFactory.label("Return date", "secondary");
        dateLabel.setAlignmentX(0f);
        body.add(dateLabel);
        body.add(Box.createVerticalStrut(4));
        picker.setAlignmentX(0f);
        body.add(picker);

        body.add(Box.createVerticalStrut(12));
        JLabel feeLabel = UiFactory.label("Fee to freeze", "secondary");
        feeLabel.setAlignmentX(0f);
        body.add(feeLabel);
        body.add(Box.createVerticalStrut(4));
        body.add(fee);
        body.add(Box.createVerticalStrut(4));
        body.add(note);
        return body;
    }

    private static JLabel monoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Fonts.mono(14, Font.PLAIN));
        label.putClientProperty(UiFactory.ROLE, "title");
        return label;
    }

    private static JLabel textLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Fonts.text(14, Font.PLAIN));
        label.putClientProperty(UiFactory.ROLE, "title");
        return label;
    }

    /** One 14 px label/value row; money and ids stay monospace (§8.2). */
    private static JPanel summary(String labelText, JComponent value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        row.add(UiFactory.label(labelText, "secondary"), BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }
}
