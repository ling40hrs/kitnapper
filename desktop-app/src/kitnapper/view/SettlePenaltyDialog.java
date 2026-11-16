package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import kitnapper.model.PenaltyOutcome;

/**
 * The §8.9 settle-penalty dialog: the frozen amount, the loan it belongs to,
 * a {@code Mark paid}/{@code Waive} choice, and a reason field that is enabled
 * only for Waive and required there (FR-11). The confirm button is primary for
 * "Mark paid" and destructive for "Waive"; the dialog itself is the §7
 * confirmation. Rejections — including a blank reason — come back from the
 * service and stay inline while the dialog remains open.
 */
final class SettlePenaltyDialog {

    private static final int SEGMENT_PAID = 0;
    private static final int SEGMENT_WAIVE = 1;

    private SettlePenaltyDialog() { }

    /**
     * Opens the dialog. {@code onSettle} settles through the controller and
     * reports a failure message; a rejected settlement keeps the dialog open.
     *
     * @return the chosen outcome, or empty when cancelled
     */
    static Optional<PenaltyOutcome> show(Window owner, PenaltyRow row,
        PenaltyOutcome initial,
        BiFunction<PenaltyOutcome, String, Optional<String>> onSettle) {
        SegmentedControl choice = new SegmentedControl("Mark paid", "Waive");
        choice.getAccessibleContext().setAccessibleName("Settlement outcome");
        JTextField reason = UiFactory.textField("Why is this fee waived?");
        FlatButton confirm = new FlatButton("Confirm", FlatButton.Role.PRIMARY);
        apply(choice, reason, confirm);
        choice.setSelectedIndex(segmentOf(initial));
        choice.addChangeListener(index -> apply(choice, reason, confirm));

        boolean done = Dialogs.showForm(owner, "Settle penalty",
            body(row, choice, reason), confirm,
            () -> onSettle.apply(chosen(choice), reason.getText()));
        return done ? Optional.of(chosen(choice)) : Optional.empty();
    }

    /** The §8.9 body, split out so probes can paint it without the modal. */
    static JComponent body(PenaltyRow row, SegmentedControl choice, JTextField reason) {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JLabel amount = new JLabel(Formats.money(row.amount()));
        amount.setFont(Fonts.mono(24, Font.BOLD));
        amount.putClientProperty(UiFactory.ROLE, "title");
        amount.setAlignmentX(0f);
        body.add(amount);
        body.add(Box.createVerticalStrut(4));
        JLabel caption = UiFactory.label("Frozen late fee", "muted");
        caption.setAlignmentX(0f);
        body.add(caption);
        body.add(Box.createVerticalStrut(16));
        body.add(summary("Loan", monoLabel(row.loanId())));
        body.add(summary("Item", textLabel(row.itemName())));
        body.add(summary("Borrower", textLabel(row.borrowerName())));
        body.add(summary("Assessed", monoLabel(Formats.date(row.assessedDate()))));
        body.add(summary("Current status", textLabel(StatusBadges.penaltyWord(row.status()))));

        body.add(Box.createVerticalStrut(16));
        JLabel choiceLabel = UiFactory.label("Settlement", "secondary");
        choiceLabel.setAlignmentX(0f);
        body.add(choiceLabel);
        body.add(Box.createVerticalStrut(4));
        choice.setAlignmentX(0f);
        body.add(choice);

        body.add(Box.createVerticalStrut(12));
        JLabel reasonLabel = UiFactory.label("Reason (required to waive)", "secondary");
        reasonLabel.setAlignmentX(0f);
        body.add(reasonLabel);
        body.add(Box.createVerticalStrut(4));
        reason.setAlignmentX(0f);
        reason.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        body.add(reason);
        return body;
    }

    private static PenaltyOutcome chosen(SegmentedControl choice) {
        return choice.getSelectedIndex() == SEGMENT_WAIVE
            ? PenaltyOutcome.WAIVED : PenaltyOutcome.PAID;
    }

    /** The segment a preview action opens on (§8.6). */
    private static int segmentOf(PenaltyOutcome outcome) {
        return outcome == PenaltyOutcome.WAIVED ? SEGMENT_WAIVE : SEGMENT_PAID;
    }

    /** Enables the reason only for Waive and restyles Confirm (§8.9). */
    private static void apply(SegmentedControl choice, JTextField reason, FlatButton confirm) {
        boolean waive = choice.getSelectedIndex() == SEGMENT_WAIVE;
        reason.setEnabled(waive);
        if (!waive) {
            reason.setText("");
        }
        confirm.setRole(waive ? FlatButton.Role.DESTRUCTIVE : FlatButton.Role.PRIMARY);
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
